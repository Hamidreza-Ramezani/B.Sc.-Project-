package ceit.aut.digitrec.gui.activities;

import java.io.IOException;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import ceit.aut.digitrec.gui.dialogs.About;
import ceit.aut.digitrec.gui.dialogs.ExternalStorage;
import ceit.aut.digitrec.gui.dialogs.Prediction.PredictionDialogListener;
import ceit.aut.digitrec.gui.views.DigitView;
import ceit.aut.digitrec.learning.DigitMlp;
import ceit.aut.digitrec.services.TrainingService;
import ceit.aut.digitrec.services.TrainingService.OnTrainingFinishedListener;
import ceit.aut.digitrec.services.TrainingService.TrainingBinder;
import ceit.aut.digitrec.utils.FileUtils;
import ceit.aut.digitrec.utils.ImageUtils;
import ceit.aut.mlp.MlpIOException;

public class TouchMode extends FragmentActivity implements
		PredictionDialogListener {
	@SuppressWarnings("unused")
	private static final String label = "handwriting_activity";
	public static final String TRAIN_CHAR = "ceit.aut.digitrec.ACTION_TRAIN_CHAR";
	public static final String KEY = "train_char";
	public static final String FILENAME = "handwriting_mlp.dat";
	public static final int size_hidden = 25;

	private TrainingService service;
	private DigitMlp mlp;
	private DigitView characterView;
	private boolean askToConfirm;
	private char trainingChar;

	private ExternalStorage storageDialog;
	private BroadcastReceiver storageReceiver;

	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			service = ((TrainingBinder) service).getService();
			service.setTrainingFinishedListener(new OnTrainingFinishedListener() {

				@Override
				public void onTrainingFinished() {
					initialize();
				}
			});
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		askToConfirm = !TRAIN_CHAR.equals(getIntent().getAction());
		trainingChar = getIntent().getCharExtra(KEY, '0');

		storageReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
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
	}

	@Override
	protected void onStart() {
		super.onStart();
		initialize();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mlp != null) {
			try {
				FileUtils.savePerceptron(this, mlp, FILENAME);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (service != null && mServiceConnection != null) {
			try {
				unbindService(mServiceConnection);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(storageReceiver);
	}

	/**
	 * Performs activity layout initialization.
	 */
	private void initialize() {
		// Load the perceptron
		FileUtils.loadCharacterLabels(this);
		if (FileUtils.savedPerceptronExists(this, FILENAME)) {
			try {
				mlp = FileUtils.loadPerceptron(this, FILENAME);
			} catch (MlpIOException e) {
				// This should not happen, but if it does, print the stack trace
				e.printStackTrace();
			}
			// If we're training a new character, modify the perceptron
			// architecture
			if (!askToConfirm) {
				mlp.setLabelCount(ImageUtils.getCharacterLabels().size());
			}
			setContentView(createCharacterLayout(mlp));
		} else {
			setContentView(R.layout.training);
			startAndBindToService();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.handwriting, menu);
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
	 * Starts a training service for handwriting and binds to it.
	 */
	private void startAndBindToService() {
		Intent intent = new Intent(this, TrainingService.class);
		intent.putExtra(TrainingService.KEY_IS_HANDWRITING, true);
		startService(intent);
		bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
	}

	/**
	 * Creates a layout that contains a DigitView for drawing characters and
 its controls.
	 *
	 * @param mlp
	 *            Perceptron associated with the created DigitView.
	 * @return RelativeLayout with DigitView and its controls.
	 */
	private RelativeLayout createCharacterLayout(final DigitMlp mlp) {
		// View container
		RelativeLayout rootLayout = new RelativeLayout(this);

		// Controls container
		LinearLayout controlsLayout = new LinearLayout(this);
		controlsLayout.setId(1);
		if (VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			addLayoutDividers(controlsLayout);
		}

		// Clear button
		Button clearButton = new Button(this);
		LinearLayout.LayoutParams linParams = new LayoutParams(0,
				LayoutParams.WRAP_CONTENT);
		linParams.weight = 1;
		clearButton.setId(2);
		clearButton.setText(R.string.clear);
		clearButton.setLayoutParams(linParams);
		clearButton.setBackgroundResource(R.drawable.grid_item_selector);
		controlsLayout.addView(clearButton);

		// Prediction button
		Button predictionButton = new Button(this);
		linParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
		linParams.weight = 1;
		predictionButton.setId(3);
		predictionButton.setText(R.string.predict);
		predictionButton.setLayoutParams(linParams);
		predictionButton.setBackgroundResource(R.drawable.grid_item_selector);
		controlsLayout.addView(predictionButton);

		// Controls container
		RelativeLayout.LayoutParams relParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		relParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rootLayout.addView(controlsLayout, relParams);

		// Character view
		characterView = new DigitView(this, mlp, askToConfirm);
		if (!askToConfirm) {
			characterView.setTrainingCharacter(trainingChar);
		}

		relParams = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		relParams.addRule(RelativeLayout.ABOVE, controlsLayout.getId());
		characterView.setLayoutParams(relParams);
		rootLayout.addView(characterView);

		// Listeners
		clearButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				characterView.clear();
			}
		});
		predictionButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				characterView.predictCharacter();
			}
		});

		return rootLayout;
	}

	/**
	 * Adds layout dividers to a given {@link LinearLayout}.
	 *
	 * @param layout
	 *            LinearLayout to add the dividers to.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void addLayoutDividers(final LinearLayout layout) {
		layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		layout.setDividerDrawable(getResources().getDrawable(
				android.R.drawable.divider_horizontal_dim_dark));
	}

	@Override
	public void OnPredictionOkClick(char predictedCharacter) {
		mlp.characterOnlineLearn(this, predictedCharacter);
		characterView.saveExampleIfNeededAs(predictedCharacter);
		characterView.initialize();
	}

	@Override
	public void OnPredictionCorrectionClick(char newCharacter) {
		mlp.characterOnlineLearn(this, newCharacter);
		characterView.saveExampleIfNeededAs(newCharacter);
		characterView.initialize();
	}

	@Override
	public void OnPredictionCancelClick() {
		characterView.initialize();
	}
}
