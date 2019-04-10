package ceit.aut.digitrec.gui.activities;

import jama.Matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.SeekBar;
import ceit.aut.digitrec.gui.dialogs.About;
import ceit.aut.digitrec.gui.dialogs.ExternalStorage;
import ceit.aut.digitrec.gui.dialogs.Prediction;
import ceit.aut.digitrec.gui.dialogs.Prediction.PredictionDialogListener;
import ceit.aut.digitrec.learning.DigitMlp;
import ceit.aut.digitrec.services.TrainingService;
import ceit.aut.digitrec.services.TrainingService.OnTrainingFinishedListener;
import ceit.aut.digitrec.services.TrainingService.TrainingBinder;
import ceit.aut.digitrec.utils.FileUtils;
import ceit.aut.digitrec.utils.ImageUtils;
import ceit.aut.mlp.MlpIOException;

public class CameraMode extends FragmentActivity implements
		CvCameraViewListener2, PredictionDialogListener {
	private static final String TAG = "camera_activity";
	public static final String fileName = "image_mlp.dat";
	public static final int size_hidden = 25;
	private static final int CV_HERSHEY = 2;
	private static final int CHAR_AREA = 40;

	private TrainingService service;
	private DigitMlp mlp;
	private CheckBox segmentCheckBox;
	private SeekBar thresholdBar;
	private Mat segmentedImage;
	private Mat lastShownImage;
	private LinkedList<Rect> charRects = new LinkedList<Rect>();
	private double horizontalMargin;
	private double verticalMargin;
	private boolean active = true;

	private ExternalStorage StorageDialog;
	private BroadcastReceiver StorageReceiver;

	private CameraBridgeViewBase OpenCvCameraView;

	private ServiceConnection serviceConnection = new ServiceConnection() {

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
					finish();
				}
			});
		}
	};

	private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.i(TAG, "OpenCV loaded successfully");
				if (OpenCvCameraView != null) {
					OpenCvCameraView.enableView();
				}
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	/**
	 * Performs activity layout initialization.
	 */
	private void initialize() {
		// Set up OpenCvCameraView
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.camera);

		// Load the perceptron
		FileUtils.loadCharacterLabels(this);
		if (FileUtils.savedPerceptronExists(this, fileName)) {
			loadLayout();
			try {
				mlp = FileUtils.loadPerceptron(this, fileName);
			} catch (MlpIOException e) {
				// This should not happen, but if it does, print the stack trace
				e.printStackTrace();
			}
			// If the number of labels is changed, set the MLP's label count
			mlp.setLabelCount(ImageUtils.getCharacterLabels().size());
		} else {
			setContentView(R.layout.training);
			startAndBindToService();
		}
	}

	/**
	 * Loads the activity layout objects.
	 */
	private void loadLayout() {
		segmentCheckBox = (CheckBox) findViewById(R.id.opencv_filter_check);
		thresholdBar = (SeekBar) findViewById(R.id.opencv_threshold_seek);
		OpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_view);
		OpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		OpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		StorageReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (Environment.MEDIA_MOUNTED.equals(Environment
						.getExternalStorageState())) {
					if (StorageDialog != null
							&& StorageDialog.isVisible()) {
						StorageDialog.dismiss();
					}
				} else {
					StorageDialog = new ExternalStorage();
					StorageDialog.show(getSupportFragmentManager(),
							ExternalStorage.TAG);
				}
			}
		};

		registerReceiver(StorageReceiver,
				FileUtils.getExternalStorageChangeFilter());
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				loaderCallback);
	}

	@Override
	protected void onStart() {
		super.onStart();
		initialize();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (OpenCvCameraView != null)
			OpenCvCameraView.disableView();
		if (mlp != null) {
			try {
				FileUtils.savePerceptron(this, mlp, fileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (service != null && serviceConnection != null) {
			try {
				unbindService(serviceConnection);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (OpenCvCameraView != null) {
			OpenCvCameraView.disableView();
		}
		unregisterReceiver(StorageReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.camera, menu);
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
	 * Starts the training service for camera and binds the activity to it.
	 */
	private void startAndBindToService() {
		Intent intent = new Intent(this, TrainingService.class);
		intent.putExtra(TrainingService.KEY_IS_HANDWRITING, false);
		startService(intent);
		bindService(intent, serviceConnection, BIND_AUTO_CREATE);
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		horizontalMargin = (OpenCvCameraView.getWidth() - width) / 2;
		verticalMargin = (OpenCvCameraView.getHeight() - height) / 2;
	}

	@Override
	public void onCameraViewStopped() {
	}

	@Override
	synchronized public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		if (!active) {
			return lastShownImage;
		}

		// Get color image
		Mat rgba = inputFrame.rgba();

		// Get grayscale image
		Mat grayscale = inputFrame.gray();

		segmentedImage = ImageUtils.segmentImage(grayscale,
				thresholdBar.getProgress());

		Mat shownImage = segmentCheckBox.isChecked() ? segmentedImage : rgba;

		// Find the bounding rectangles
		Mat contourImg = segmentedImage.clone();
		Scalar color = segmentCheckBox.isChecked() ? new Scalar(255, 255, 255)
				: new Scalar(0, 255, 0);
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(contourImg, contours, new Mat(),
				Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		charRects.clear();

		// For each rectangle, resize the subimage and predict the character
		for (MatOfPoint contour : contours) {
			// Skip small fragments for faster processing
			if (contour.size().area() < CHAR_AREA) {
				continue;
			}

			// Crop to the bounding box and resize it
			Rect boundingRect = Imgproc.boundingRect(contour);
			charRects.add(boundingRect);

			Mat resizedCharacterImage = ImageUtils.getResizedSubImage(
					segmentedImage, boundingRect);

			char prediction = predictCharacter(resizedCharacterImage);

			// Mark the boundary and show the prediction
			Core.rectangle(shownImage, boundingRect.tl(), boundingRect.br(),
					color);
			Core.putText(shownImage, String.valueOf(prediction), boundingRect
					.tl(), CV_HERSHEY, 2,
					new Scalar(255, 0, 0, 255), 3);
		}

		lastShownImage = shownImage;
		return shownImage;
	}

	@Override
	synchronized public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (segmentedImage == null) {
				return true;
			}

			active = false;

			// Find the clicked character rectangle
			Rect charRect = null;
			for (Rect rect : charRects) {
				if (rect.contains(new Point(event.getX() - horizontalMargin,
						event.getY() - verticalMargin))) {
					charRect = rect;
					break;
				}
			}
			if (charRect == null) {
				active = true;
				return true;
			}

			Mat characterImage = ImageUtils.getResizedSubImage(segmentedImage,
					charRect);

			char prediction = predictCharacter(characterImage);
			showPredictionDialog(prediction);
			return true;
		}
		return false;
	}

	/**
	 * Performs and returns the prediction on the given character image matrix.
	 *
	 * @param characterImage
	 *            Matrix of the character image.
	 * @return Predicted character.
	 */
	private char predictCharacter(final Mat characterImage) {
		Matrix potentialCharacterImage = ImageUtils
				.openCvMatToJamaImageMatrix(characterImage);
		return mlp.predictCharacter(potentialCharacterImage);
	}

	/**
	 * Instantiates and shows the prediction dialog.
	 *
	 * @param prediction
	 *            Predicted character.
	 */
	private void showPredictionDialog(final char prediction) {
		Prediction dialog = Prediction.newInstance(prediction);
		dialog.show(getSupportFragmentManager(), Prediction.TAG);
	}

	@Override
	public void OnPredictionOkClick(char predictedCharacter) {
		mlp.characterOnlineLearn(this, predictedCharacter);
		active = true;
	}

	@Override
	public void OnPredictionCorrectionClick(char newCharacter) {
		mlp.characterOnlineLearn(this, newCharacter);
		active = true;
	}

	@Override
	public void OnPredictionCancelClick() {
		active = true;
	}
}
