package ceit.aut.digitrec.services;

import java.io.IOException;
import java.io.InputStream;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import ceit.aut.digitrec.gui.activities.MainActivity;
import ceit.aut.digitrec.utils.FileUtils;

public class TransferService extends Service {
	@SuppressWarnings("unused")
	private static final String TAG = "transferring_service";
	private static final int BASE_NOTIFICATION_ID = 2;
	private static final String KEY_INIT_FILES_TRANSFERRED = "init_file_transfer";

	private TransferBinder mBinder;
	private OnTransferFinishedListener mTransferListener;
	private boolean mIsTransferring = false;

	/**
	 * Transfer service binder.
	 */
	public class TransferBinder extends Binder {
		/**
		 * Returns the {@link TransferService}.
		 *
		 * @return TransferService instance.
		 */
		public TransferService getService() {
			return TransferService.this;
		}
	}

	public static interface OnTransferFinishedListener {
		/**
		 * Callback for when the transfer process is complete.
		 */
		public void onTransferFinished();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		startForeground(BASE_NOTIFICATION_ID, buildServiceNotification());
	}

	@Override
	public IBinder onBind(Intent arg0) {
		mBinder = new TransferBinder();
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand();

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
				.setContentTitle(getString(R.string.transfer_service_title))
				.setContentText(getString(R.string.transfer_service_body));

		Intent resultIntent = new Intent(this, MainActivity.class);
		PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
				resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);

		return builder.build();
	}

	/**
	 * Handles the command the intent was started with. This checks for whether
	 * the transfer process already exists. Only one transfer process can be run
	 * at a time.
	 */
	private synchronized void handleCommand() {
		if (mIsTransferring) {
			return;
		}

		mIsTransferring = true;

		transferInitFiles();
	}

	/**
	 * Transfers the initial files from the APK's raw resources to external
	 * storage.
	 */
	private void transferInitFiles() {
		new AsyncTask<InputStream, Void, InputStream>() {

			@Override
			protected InputStream doInBackground(InputStream... params) {
				InputStream inputStream = params[0];
				try {
					FileUtils.unzip(inputStream, getExternalFilesDir(null));
				} catch (IOException e) {
					e.printStackTrace();
				}
				return inputStream;
			}

			@Override
			protected void onPostExecute(InputStream result) {
				try {
					result.close();
					mIsTransferring = false;
					getSharedPreferences(MainActivity.label, MODE_PRIVATE).edit()
							.putBoolean(KEY_INIT_FILES_TRANSFERRED, true)
							.commit();
					if (mTransferListener != null) {
						mTransferListener.onTransferFinished();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					stopSelf();
				}
			}
		}.execute(getResources().openRawResource(R.raw.init_data));
	}

	/**
	 * Sets the transfer finished callback.
	 *
	 * @param transferListener
	 *            Transfer finished listener.
	 */
	public void setTransferFinishedListener(
			final OnTransferFinishedListener transferListener) {
		mTransferListener = transferListener;
	}
}
