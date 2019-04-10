package ceit.aut.digitrec.gui.activities;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import ceit.aut.digitrec.gui.dialogs.About;
import ceit.aut.digitrec.gui.dialogs.ExternalStorage;
import ceit.aut.digitrec.services.TrainingService;
import ceit.aut.digitrec.services.TransferService;
import ceit.aut.digitrec.services.TransferService.OnTransferFinishedListener;
import ceit.aut.digitrec.services.TransferService.TransferBinder;
import ceit.aut.digitrec.utils.FileUtils;
import ceit.aut.digitrec.utils.ImageUtils;

public class MainActivity extends FragmentActivity {
	public static final String label = "main_activity";

	/**
	 * Action on the basis of which this activity accepts intents. Handwriting
	 * MLP weights are removed.
	 */
	public static final String REMOVE_WEIGHTS = "ceit.aut.digitrec.ACTION_REMOVE_WEIGHTS_HW";

	/**
	 * Action on the basis of which this activity accepts intents. Image/camera
	 * MLP weights are removed.
	 */
	public static final String REMOVE_WEIGHTS_IM = "ceit.aut.digitrec.ACTION_REMOVE_WEIGHTS_IM";

	/**
	 * Action on the basis of which this activity accepts intents. This exits
	 * the application, as this is the launcher and a singleTop activity.
	 */
	public static final String EXIT_APPLICATION = "ceit.aut.digitrec.ACTION_EXIT";

	private static final String FILES_TRANSFERRED = "init_file_transfer";

	private ExternalStorage storageDialog;
	private BroadcastReceiver storageReceiver;

	private TransferService service;
	private boolean bound = false;

	private ServiceConnection connection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			service = ((TransferBinder) service).getService();
			service.setTransferFinishedListener(new OnTransferFinishedListener() {

				@Override
				public void onTransferFinished() {
					setContentView(R.layout.activity_main);
				}
			});
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		storageReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d("main_activity", "RECEIVED");
				if (Environment.MEDIA_MOUNTED.equals(Environment
						.getExternalStorageState())) {
					if (storageDialog != null
							&& storageDialog.isVisible()) {
						storageDialog.dismiss();
					}
				} else {
					storageDialog = new ExternalStorage();
					storageDialog.show(getSupportFragmentManager(),
							ExternalStorage.TAG);
				}
			}
		};

		registerReceiver(storageReceiver,
				FileUtils.getExternalStorageChangeFilter());

		if (!FileUtils.isInitFileCorrect(this)) {
			Toast.makeText(this, getString(R.string.init_checksum_fail),
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		Settings.correctOnlineDecimalValueIfNeeded(this);

		handleActions(getIntent());

		transferInitFilesIfNeeded();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleActions(intent);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (service != null && mServiceConnection != null && bound) {
			unbindService(mServiceConnection);
			bound = false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(storageReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivityForResult(new Intent(this, Settings.class),
					Settings.CODE);
			return true;
		case R.id.menu_about:
			About dialog = new About();
			dialog.show(getSupportFragmentManager(), About.TAG);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Handles actions passed to this activity.
	 *
	 * @param intent
	 *            Intent that has started this activity.
	 */
	private void handleActions(final Intent intent) {
		// If the action is to remove the perceptron weight files, do so
		if (REMOVE_WEIGHTS.equals(intent.getAction())) {
			retrainHandwritingNetwork();
		}
		if (REMOVE_WEIGHTS_IM.equals(intent.getAction())) {
			retrainImageNetwork();
		}

		// If the action is to exit the application, finish the activity
		if (EXIT_APPLICATION.equals(intent.getAction())) {
			finish();
			return;
		}
	}

	/**
	 * Starts a service to transfer the initial files from the APK's raw
	 * resources to external storage if this hasn't been done before.
	 */
	private void transferInitFilesIfNeeded() {
		SharedPreferences prefs = getSharedPreferences(label, MODE_PRIVATE);
		if (!prefs.getBoolean(FILES_TRANSFERRED, false)) {
			setContentView(R.layout.transferring);
			startAndBindToService();
		}
	}

	/**
	 * Starts an init file transfer service and binds to it.
	 */
	private void startAndBindToService() {
		Intent intent = new Intent(this, TransferService.class);
		startService(intent);
		bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
		bound = true;
	}

	public void onNavigHandwritingClick(final View view) {
		startActivity(new Intent(this, TouchMode.class));
	}

	public void onNavigCameraClick(final View view) {
		startActivity(new Intent(this, CameraMode.class));
	}

	public void onNavigCharactersClick(final View view) {
		startActivity(new Intent(this, CharacterList.class));
	}

	public void onNavigImageClick(final View view) {
		startActivity(new Intent(this, ImageMode.class));
	}

	/**
	 * Retrains the neural network used for handwriting recognition from touch
	 * input.
	 */
	private void retrainHandwritingNetwork() {
		File handwritingWeightFile = new File(getExternalFilesDir(null),
				TouchMode.FILENAME);
		handwritingWeightFile.delete();
		removeKnownCharactersFile();

		startHandwritingLearningService();

		Toast.makeText(this, getString(R.string.retrain_started_hw),
				Toast.LENGTH_LONG).show();
	}

	/**
	 * Retrains the neural network used for handwriting recognition from
	 * image/camera input.
	 */
	private void retrainImageNetwork() {
		File cameraWeightFile = new File(getExternalFilesDir(null),
				CameraMode.fileName);
		cameraWeightFile.delete();

		startImageLearningService();

		Toast.makeText(this, getString(R.string.retrain_started_im),
				Toast.LENGTH_LONG).show();
	}

	/**
	 * Starts a service that trains the neural network for handwriting
	 * recognition.
	 */
	private void startHandwritingLearningService() {
		Intent intent = new Intent(this, TrainingService.class);
		intent.putExtra(TrainingService.KEY_IS_HANDWRITING, true);
		startService(intent);
	}

	/**
	 * Starts a service that trains the neural network for image/camera
	 * recognition.
	 */
	private void startImageLearningService() {
		Intent intent = new Intent(this, TrainingService.class);
		intent.putExtra(TrainingService.KEY_IS_HANDWRITING, false);
		startService(intent);
	}

	/**
	 * Removes the file with the list of known characters.
	 */
	private void removeKnownCharactersFile() {
		File knownCharactersFile = new File(getExternalFilesDir(null),
				ImageUtils.CHARACTERS_FILE_NAME);
		knownCharactersFile.delete();
	}
}
