package ceit.aut.digitrec.services;

import jama.Matrix;

import java.io.File;
import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import ceit.aut.digitrec.gui.activities.CameraMode;
import ceit.aut.digitrec.gui.activities.TouchMode;
import ceit.aut.digitrec.gui.activities.MainActivity;
import ceit.aut.digitrec.gui.activities.Settings;
import ceit.aut.digitrec.learning.DigitMlp;
import ceit.aut.digitrec.utils.FileUtils;
import ceit.aut.digitrec.utils.ImageUtils;
import ceit.aut.mlp.Architecture;
import ceit.aut.mlp.Pair;

public class TrainingService extends Service {
	private static final String TAG = "training_service";

	/**
	 * Whether or not this service is to be started in handwriting or
	 * camera/image mode.
	 */
	public static final String KEY_IS_HANDWRITING = "is_handwriting";

	private static final int BASE_NOTIFICATION_ID = 1;
	private static final String WAKE_LOCK_TAG = "TrainingService_WL";

	private TrainingBinder mBinder;
	private OnTrainingFinishedListener mTrainingListener;
	private boolean mIsTrainingHandwriting = false;
	private boolean mIsTrainingCamera = false;

	/**
	 * Training service binder.
	 */
	public class TrainingBinder extends Binder {
		/**
		 * Returns the {@link TrainingService}.
		 *
		 * @return TrainingService instance.
		 */
		public TrainingService getService() {
			return TrainingService.this;
		}
	}

	public static interface OnTrainingFinishedListener {
		/**
		 * Callback for when the training process is complete.
		 */
		public void onTrainingFinished();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		startForeground(BASE_NOTIFICATION_ID, buildServiceNotification());
	}

	@Override
	public IBinder onBind(Intent arg0) {
		mBinder = new TrainingBinder();
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);

		return START_NOT_STICKY;
	}

	/**
	 * Builds the service's foreground notification.
	 *
	 * @return Built notification.
	 */
	private Notification buildServiceNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getString(R.string.service_title))
				.setContentText(getString(R.string.service_body));

		Intent resultIntent = new Intent(this, MainActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
				resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);

		return builder.build();
	}

	/**
	 * Handles the command the service was started with.
	 *
	 * @param intent
	 *            Intent specifying what the service should do.
	 */
	private synchronized void handleCommand(final Intent intent) {
		if (intent == null) {
			return;
		}

		final boolean isHandwriting = intent.getBooleanExtra(
				KEY_IS_HANDWRITING, true);

		if ((isHandwriting && mIsTrainingHandwriting)
				|| (!isHandwriting && mIsTrainingCamera)) {
			return;
		}

		if (isHandwriting
				&& FileUtils.savedPerceptronExists(this,
						TouchMode.FILENAME)) {
			return;
		}
		if (!isHandwriting
				&& FileUtils.savedPerceptronExists(this,
						CameraMode.fileName)) {
			return;
		}

		if (isHandwriting) {
			mIsTrainingHandwriting = true;
		} else {
			mIsTrainingCamera = true;
		}

		performTraining(isHandwriting);
	}

	/**
	 * This initiates the asynchronous training process based on whether the
	 * service has been started in handwriting or camera/image mode.
	 *
	 * @param isHandwriting
	 *            Whether the handwriting MLP should be trained. If false, the
	 *            camera/image MLP is trained.
	 */
	private void performTraining(final boolean isHandwriting) {
		new AsyncTask<Boolean, Void, DigitMlp>() {
			private boolean mIsHandwriting;
			private PowerManager.WakeLock mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
					.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);

			@Override
			protected DigitMlp doInBackground(Boolean... params) {
				// Acquire a partial wake lock
				mWakeLock.acquire();

				mIsHandwriting = params[0];

				Pair<Matrix> labeledDatasetPair;
				if (mIsHandwriting) {
					labeledDatasetPair = ImageUtils
							.loadHandwritingDataset(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
				} else {
					labeledDatasetPair = ImageUtils
							.loadPictureDataset(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
				}

				final DigitMlp mlp = new DigitMlp(new Architecture(
						ImageUtils.IMAGE_WIDTH * ImageUtils.IMAGE_HEIGHT
								* (mIsHandwriting ? 2 : 1),
						getHiddenLayerSize(mIsHandwriting), ImageUtils
								.getCharacterLabels().size()));

				if (shouldUseBackprop()) {
					mlp.backpropTrain(labeledDatasetPair.first,
							labeledDatasetPair.second);
					Log.i(TAG, "using backpropagation");
				} else {
					mlp.rpropTrain(labeledDatasetPair.first,
							labeledDatasetPair.second);
					Log.i(TAG, "using rprop");
				}

				savePerceptron(mlp, mIsHandwriting);
				saveCharacterLabels();

				return mlp;
			}

			@Override
			protected void onPostExecute(DigitMlp mlp) {
				// Release the partial wake lock
				mWakeLock.release();

				if (mIsHandwriting) {
					mIsTrainingHandwriting = false;
				} else {
					mIsTrainingCamera = false;
				}
				if (mTrainingListener != null) {
					mTrainingListener.onTrainingFinished();
				}
				stopSelf();
			}
		}.execute(isHandwriting);
	}

	/**
	 * Gets the hidden layer size based on the mode of this service.
	 *
	 * @param isHandwriting
	 *            Whether the handwriting MLP should be trained. If false, the
	 *            camera/image MLP is trained.
	 * @return Hidden size of the network that is being trained.
	 */
	private int getHiddenLayerSize(final boolean isHandwriting) {
		return isHandwriting ? TouchMode.size_hidden
				: CameraMode.size_hidden;
	}

	/**
	 * Saves a perceptron to a file.
	 *
	 * @param mlp
	 *            Perceptron.
	 * @param isHandwriting
	 *            If the dataset to be trained is in touch input handwriting
	 *            mode.
	 */
	private void savePerceptron(final DigitMlp mlp,
			final boolean isHandwriting) {
		savePerceptron(mlp, isHandwriting ? TouchMode.FILENAME
				: CameraMode.fileName);
	}

	/**
	 * Saves a perceptron to a file with the given file name.
	 *
	 * @param mlp
	 *            Multilayer perceptron instance.
	 * @param fileName
	 *            The file name to save the perceptron to.
	 */
	private void savePerceptron(final DigitMlp mlp, final String fileName) {
		File storageDir = getExternalFilesDir(null);
		File weightsFile = new File(storageDir, fileName);
		try {
			mlp.saveToFile(weightsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Saves the known character labels to a file.
	 */
	private void saveCharacterLabels() {
		try {
			ImageUtils.saveCharacterLabels(getExternalFilesDir(null));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the training finished callback.
	 *
	 * @param trainingListener
	 *            Training finished listener.
	 */
	public void setTrainingFinishedListener(
			final OnTrainingFinishedListener trainingListener) {
		mTrainingListener = trainingListener;
	}

	/**
	 * Returns whether the pure backpropagation learning algorithm or Rprop
	 * should be used.
	 *
	 * @return True if backpropagation is to be used, false if Rprop should be
	 *         used.
	 */
	private boolean shouldUseBackprop() {
		final String[] entries = getResources().getStringArray(
				R.array.pref_algorithm_values);
		if (entries == null || entries.length == 0) {
			return true;
		}

		final String backpropValue = entries[0];

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (prefs.getString(Settings.KEY_PREF, backpropValue)
				.equals(backpropValue)) {
			return true;
		}

		return false;
	}
}
