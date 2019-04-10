package ceit.aut.digitrec.gui.activities;

import jama.Matrix;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import ceit.aut.digitrec.gui.dialogs.About;
import ceit.aut.digitrec.gui.dialogs.ExternalStorage;
import ceit.aut.digitrec.gui.fragments.ImageFragment;
import ceit.aut.digitrec.learning.DigitMlp;
import ceit.aut.digitrec.services.TrainingService;
import ceit.aut.digitrec.services.TrainingService.OnTrainingFinishedListener;
import ceit.aut.digitrec.services.TrainingService.TrainingBinder;
import ceit.aut.digitrec.utils.FileUtils;
import ceit.aut.digitrec.utils.ImageUtils;
import ceit.aut.mlp.MlpIOException;

public class ImageMode extends FragmentActivity {
	@SuppressWarnings("unused")
	private static final String label = "image_activity";
	private static final int SELECT_PICTURE_CODE = 1;

	public static final String FILENAME = "image_mlp.dat";
	public static final int size_hidden = 25;
	private static final int CV_HERSHEY = 2;
	private static final int CHAR_AREA = 40;

	private boolean contentView;
	private TrainingService service;
	private DigitMlp mlp;
	private Bitmap imageBitmap;
	private Mat imageMatrix;
	private LinkedList<Rect> charRects = new LinkedList<Rect>();
	private CheckBox segmentCheckBox;
	private SeekBar thresholdBar;
	private ImageView imageView;
	private ImageFragment imageFragment;
	private RelativeLayout imageLayout;

	private ExternalStorage storageDialog;
	private BroadcastReceiver storageReceiver;

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
					initialize();
				}
			});
		}
	};

	private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				processAndShowBitmap();
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
		// Load the perceptron
		FileUtils.loadCharacterLabels(this);
		if (FileUtils.savedPerceptronExists(this, FILENAME)) {
			if (!contentView) {
				setContentView(R.layout.image);
				contentView = true;
				loadLayout();
			}
			try {
				mlp = FileUtils.loadPerceptron(this, FILENAME);
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
		segmentCheckBox = (CheckBox) findViewById(R.id.image_filter_check);
		thresholdBar = (SeekBar) findViewById(R.id.image_threshold_seek);
		imageView = (ImageView) findViewById(R.id.image_char);
		imageFragment = (ImageFragment) getSupportFragmentManager()
				.findFragmentById(R.id.image_char_fragment);
		imageLayout = (RelativeLayout) findViewById(R.id.image_controls);

		// Callbacks
		segmentCheckBox
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						processAndShowBitmap();
					}
				});
		thresholdBar
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						processAndShowBitmap();
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
					}
				});

		imageView.setVisibility(View.VISIBLE);
		imageLayout.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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
	};

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
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(storageReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.image, menu);
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
		case R.id.menu_load_image:
			startGalleryIntent();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Starts an intent to let the user load an image file.
	 */
	private void startGalleryIntent() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(Intent.createChooser(intent,
				getString(R.string.image_select_file)), SELECT_PICTURE_CODE);
	}

	/**
	 * Converts a {@link Uri} into a {@link String} file path.
	 *
	 * @param uri
	 *            URI of a file.
	 * @return File path as a String.
	 */
	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		// Using a deprecated method for API level <11 compatibility
		@SuppressWarnings("deprecation")
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	/**
	 * Starts a training service for images and binds to it.
	 */
	private void startAndBindToService() {
		Intent intent = new Intent(this, TrainingService.class);
		intent.putExtra(TrainingService.KEY_IS_HANDWRITING, false);
		startService(intent);
		bindService(intent, serviceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == SELECT_PICTURE_CODE) {
			if (resultCode == RESULT_OK) {
				Uri selectedImageUri = data.getData();
				String selectedImagePath = getPath(selectedImageUri);
				imageBitmap = ImageUtils.decodeSampledBitmapFromFile(
						selectedImagePath, imageView.getWidth(),
						imageView.getHeight());

				Mat imageMat = new Mat();
				Utils.bitmapToMat(imageBitmap, imageMat);
				imageMatrix = imageMat;
				processAndShowBitmap();

				imageFragment.setImageBitmap(imageBitmap);
				imageFragment.setOriginalImageMat(imageMatrix);
			}
		}
	}

	/**
	 * Marks characters in the image bitmap and shows it.
	 */
	private void processAndShowBitmap() {
		if (imageBitmap == null) {
			return;
		}

		Mat processed = markAndPredict(imageMatrix);
		Utils.matToBitmap(processed, imageBitmap);
		imageView.setImageBitmap(imageBitmap);
	}

	/**
	 * Performs image preprocessing and character recognition in the image.
	 *
	 * @param originalImage
	 *            Matrix of the original image bitmap.
	 * @return Processed matrix with marked characters and drawn labels.
	 */
	private Mat markAndPredict(final Mat originalImage) {
		Mat inputImage = originalImage.clone();

		// Get grayscale image
		Mat grayscale = inputImage.clone();
		Imgproc.cvtColor(inputImage, grayscale, Imgproc.COLOR_RGB2GRAY);

		Mat segmented = ImageUtils.segmentImage(grayscale,
				thresholdBar.getProgress());

		Mat shownImage = segmentCheckBox.isChecked() ? segmented : inputImage;

		// Find the bounding rectangles
		Mat contourImg = segmented.clone();
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
					segmented, boundingRect);

			// Make a prediction
			Matrix potentialCharacterImage = ImageUtils
					.openCvMatToJamaImageMatrix(resizedCharacterImage);
			char prediction = mlp.predictCharacter(potentialCharacterImage);

			// Mark the boundary and show the prediction
			Core.rectangle(shownImage, boundingRect.tl(), boundingRect.br(),
					color);
			Core.putText(shownImage, String.valueOf(prediction), boundingRect
					.tl(), CV_HERSHEY, 2,
					new Scalar(255, 0, 0, 255), 3);
		}

		return shownImage;
	}

	/**
	 * Sets the original bitmap instance.
	 *
	 * @param bitmap
	 *            New "original" bitmap instance.
	 */
	public void setOriginalImageBitmap(final Bitmap bitmap) {
		imageBitmap = bitmap;
	}

	/**
	 * Sets the original bitmap matrix.
	 *
	 * @param mat
	 *            New "original" bitmap matrix.
	 */
	public void setOriginalImageMat(final Mat mat) {
		imageMatrix = mat;
	}
}
