package ceit.aut.digitrec.gui.views;

import jama.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import ceit.aut.digitrec.gui.activities.Settings;
import ceit.aut.digitrec.gui.dialogs.Prediction;
import ceit.aut.digitrec.learning.DigitMlp;
import ceit.aut.digitrec.utils.ImageUtils;

public class DigitView extends View {
	private final int HIGHLIGHT_COLOR = getResources().getColor(
			R.color.charrec_green);

	private static final int STROKE_COLOR = Color.BLACK;
	private static final int STROKE_WIDTH = 10;
	private static final int HIGHLIGHT_WIDTH = 2;
	private static final int STROKE_POINT_WIDTH = 30;
	private static final int SHADOW_RADIUS = 10;
	private static final float MIN_CHAR_SIZE_RATIO = 0.4f;
	private static final float MAX_CHAR_SIZE_RATIO = 5.0f;

	/**
	 * The prefix of the stroke end point bitmap files.
	 */
	public static final String STROKE_MAP_PREFIX = "p_";

	private final FragmentActivity mActivity;
	private final DigitMlp mMlp;
	private final boolean mAskToConfirm;
	private char mTrainingChar;

	private int mWidth, mHeight;
	private Bitmap mBitmap, mOverlayBitmap, mStrokePointBitmap;
	private Canvas mCanvas, mOverlayCanvas, mStrokePointCanvas;
	private Paint mPaint, mOverlayPaint, mStrokePointPaint;
	private Path mDrawingPath;

	private final String mImageDirPath = getContext().getExternalFilesDir(
			Environment.DIRECTORY_PICTURES).getAbsolutePath();

	/**
	 * Constructor of the view, linking to a hosting activity and a character
	 * perceptron.
	 *
	 * @param activity
	 *            Hosting activity.
	 * @param mlp
	 *            Character multilayer perceptron used for character
	 *            recognition.
	 * @param askToConfirm
	 *            Whether the user should be asked for feedback after
	 *            predictions.
	 */
	public DigitView(final FragmentActivity activity,
			final DigitMlp mlp, final boolean askToConfirm) {
		super(activity);
		mActivity = activity;
		mMlp = mlp;
		mAskToConfirm = askToConfirm;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
		mHeight = View.MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(mWidth, mHeight);
		initialize();
	}

	/**
	 * Initializes the graphical components and resets the view.
	 */
	public void initialize() {
		mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
		mOverlayBitmap = Bitmap.createBitmap(mWidth, mHeight,
				Bitmap.Config.ARGB_8888);
		mStrokePointBitmap = Bitmap.createBitmap(mWidth, mHeight,
				Bitmap.Config.ARGB_8888);

		mCanvas = new Canvas(mBitmap);
		mOverlayCanvas = new Canvas(mOverlayBitmap);
		mStrokePointCanvas = new Canvas(mStrokePointBitmap);

		mPaint = new Paint();
		mPaint.setStyle(Style.STROKE);
		mPaint.setStrokeWidth(STROKE_WIDTH);
		mPaint.setColor(STROKE_COLOR);
		mPaint.setStrokeCap(Cap.ROUND);
		mPaint.setStrokeJoin(Join.ROUND);
		mPaint.setAntiAlias(true);
		mPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, STROKE_COLOR);

		mOverlayPaint = new Paint();
		mOverlayPaint.setStyle(Style.STROKE);
		mOverlayPaint.setStrokeWidth(HIGHLIGHT_WIDTH);
		mOverlayPaint.setColor(HIGHLIGHT_COLOR);

		mStrokePointPaint = new Paint();
		mStrokePointPaint.setStyle(Style.STROKE);
		mStrokePointPaint.setStrokeWidth(STROKE_POINT_WIDTH);
		mStrokePointPaint.setColor(STROKE_COLOR);
		mStrokePointPaint.setStrokeCap(Cap.ROUND);

		mDrawingPath = new Path();

		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawPath(mDrawingPath, mPaint);
		canvas.drawBitmap(mOverlayBitmap, 0, 0, mOverlayPaint);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN) {
			mDrawingPath.moveTo(event.getX(), event.getY());
			// Draw the stroke start point to the stroke map
			mStrokePointCanvas.drawPoint(event.getX(), event.getY(),
					mStrokePointPaint);
			invalidate();
			return true;
		}

		if (action == MotionEvent.ACTION_UP) {
			mDrawingPath.lineTo(event.getX(), event.getY());
			// Draw the stroke start point to the stroke map
			mStrokePointCanvas.drawPoint(event.getX(), event.getY(),
					mStrokePointPaint);
			invalidate();
			return true;
		}

		if (action == MotionEvent.ACTION_MOVE) {
			for (int i = 0; i < event.getHistorySize(); i++) {
				mDrawingPath.lineTo(event.getHistoricalX(i),
						event.getHistoricalY(i));
				mDrawingPath.moveTo(event.getHistoricalX(i),
						event.getHistoricalY(i));
			}
			invalidate();
			return true;
		}

		return false;
	}

	/**
	 * Sets the training character for this view. When set and the view was
	 * constructed not to ask for user confirmations after predictions, this is
	 * the character that us automatically used as the label assigned to each
	 * character drawn in this view. As such, the assigned multilayer perceptron
	 * learns each drawn character without asking for feedback.
	 *
	 * @param trainingChar
	 *            The character the user is training.
	 */
	public void setTrainingCharacter(final char trainingChar) {
		mTrainingChar = trainingChar;
	}

	/**
	 * Returns the bounding box rectangle of the drawing.
	 *
	 * @return Boundary rectangle of the drawing.
	 */
	private Rect getDrawingBounds() {
		RectF boundsF = new RectF();
		mDrawingPath.computeBounds(boundsF, true);

		float ratio = boundsF.width() / boundsF.height();
		float widthMargin = 0f, heightMargin = 0f;
		if (ratio < MIN_CHAR_SIZE_RATIO) {
			widthMargin = (MIN_CHAR_SIZE_RATIO * boundsF.height() - boundsF
					.width()) / 2;
		} else if (ratio > MAX_CHAR_SIZE_RATIO) {
			heightMargin = (1 / (MAX_CHAR_SIZE_RATIO / boundsF.width()) - boundsF
					.height()) / 2;
		}

		final int shift = STROKE_WIDTH / 2 + SHADOW_RADIUS;
		Rect bounds = new Rect();
		bounds.set((int) (boundsF.left - shift - widthMargin),
				(int) (boundsF.top - shift - heightMargin),
				(int) (boundsF.right + shift + widthMargin),
				(int) (boundsF.bottom + shift + heightMargin));
		return bounds;
	}

	/**
	 * Draws a boundary around the drawing.
	 */
	private void markDrawing() {
		mOverlayCanvas.drawRect(getDrawingBounds(), mOverlayPaint);
		invalidate();
	}

	/**
	 * Predicts the drawn character and displays the result.
	 */
	public void predictCharacter() {
		mCanvas.drawPath(mDrawingPath, mPaint);
		markDrawing();

		// Assemble the dataset
		double[] characterPixels = ImageUtils
				.readImagePixelsFromBitmap(getOutputBitmap());
		double[] strokePointPixels = ImageUtils
				.readImagePixelsFromBitmap(getStrokePointBitmap());
		double[] datasetPixels = new double[characterPixels.length
				+ strokePointPixels.length];
		System.arraycopy(characterPixels, 0, datasetPixels, 0,
				characterPixels.length);
		System.arraycopy(strokePointPixels, 0, datasetPixels,
				characterPixels.length, strokePointPixels.length);
		final Matrix dataset = new Matrix(datasetPixels, 1);

		// Make the prediction
		char prediction = mMlp.predictCharacter(dataset);

		if (mAskToConfirm) {
			// In normal mode, show the prediction dialog
			showPredictionDialog(prediction);
		} else {
			// In training mode, perform online learning
			Toast.makeText(
					getContext(),
					getContext().getString(R.string.predict_result) + " "
							+ prediction, Toast.LENGTH_SHORT).show();
			mMlp.characterOnlineLearn(mActivity, mTrainingChar);
			// Save the files if needed
			saveExampleIfNeededAs(mTrainingChar);
			initialize();
		}
		invalidate();
	}

	/**
	 * Instantiates and shows the prediction dialog.
	 *
	 * @param prediction
	 *            Predicted character.
	 */
	private void showPredictionDialog(final char prediction) {
		Prediction dialog = Prediction.newInstance(prediction);
		dialog.show(mActivity.getSupportFragmentManager(), Prediction.TAG);
	}

	/**
	 * Saves the current character and stroke bitmaps to files if needed. This
	 * can later be used for offline learning. This method checks the
	 * application settings to determine whether to save these bitmaps or not.
	 *
	 * @param predictedCharacter
	 *            The character the bitmaps should be saved as. This determines
	 *            the directory the files will be saved to.
	 */
	public void saveExampleIfNeededAs(char predictedCharacter) {
		if (PreferenceManager.getDefaultSharedPreferences(getContext())
				.getBoolean(Settings.PREF_SAVE, true)) {
			String fileName = generateFileName();
			try {
				saveOutputBitmap(fileName, predictedCharacter);
				saveStrokePointBitmap(fileName, predictedCharacter);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the bitmap after image post-processing has been applied.
	 *
	 * @return Output bitmap.
	 */
	private Bitmap getOutputBitmap() {
		mCanvas.drawPath(mDrawingPath, mPaint);
		Rect bounds = getDrawingBounds();

		Bitmap outputBitmap = Bitmap.createBitmap(ImageUtils.IMAGE_WIDTH,
				ImageUtils.IMAGE_HEIGHT, Config.ARGB_8888);
		Canvas outputCanvas = new Canvas(outputBitmap);
		Paint outputPaint = new Paint();
		outputPaint.setColor(Color.WHITE);

		outputCanvas.drawBitmap(mBitmap.extractAlpha(), bounds, new Rect(0, 0,
				ImageUtils.IMAGE_WIDTH, ImageUtils.IMAGE_HEIGHT), outputPaint);
		return outputBitmap;
	}

	/**
	 * Returns the bitmap containing stroke end points after image
	 * post-processing has been applied.
	 *
	 * @return Stroke point bitmap.
	 */
	private Bitmap getStrokePointBitmap() {
		Rect bounds = getDrawingBounds();

		Bitmap outputStrokePointBitmap = Bitmap.createBitmap(
				ImageUtils.IMAGE_WIDTH, ImageUtils.IMAGE_HEIGHT,
				Config.ARGB_8888);
		Canvas outputStrokePointCanvas = new Canvas(outputStrokePointBitmap);
		Paint outputStrokePointPaint = new Paint();
		outputStrokePointPaint.setColor(Color.WHITE);

		outputStrokePointCanvas.drawBitmap(mStrokePointBitmap.extractAlpha(),
				bounds, new Rect(0, 0, ImageUtils.IMAGE_WIDTH,
						ImageUtils.IMAGE_HEIGHT), outputStrokePointPaint);
		return outputStrokePointBitmap;
	}

	/**
	 * Saves the bitmap after image post-processing has been applied.
	 *
	 * @param baseFileName
	 *            Base name of the image file.
	 * @param character
	 *            Predicted character, which represents the folder of the saved
	 *            file.
	 * @return Image file name.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	private String saveOutputBitmap(final String baseFileName,
			final char character) throws IOException {
		final Bitmap outputBitmap = getOutputBitmap();

		String fileNamePrefix = "" + character + File.separatorChar;
		final String fileName = fileNamePrefix + baseFileName + ".jpg";
		saveBitmap(outputBitmap, fileName);

		return fileName;
	}

	/**
	 * Saves the bitmap containing stroke end points after image post-processing
	 * has been applied.
	 *
	 * @param baseFileName
	 *            Base name of the image file.
	 * @param character
	 *            Predicted character, which represents the folder of the saved
	 *            file.
	 * @return Image file name.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	private String saveStrokePointBitmap(final String baseFileName,
			final char character) throws IOException {
		final Bitmap outputStrokePointBitmap = getStrokePointBitmap();

		String fileNamePrefix = "" + character + File.separatorChar;
		final String fileName = fileNamePrefix + STROKE_MAP_PREFIX
				+ baseFileName + ".jpg";
		saveBitmap(outputStrokePointBitmap, fileName);

		return fileName;
	}

	/**
	 * Saves a bitmap to a file.
	 *
	 * @param bitmap
	 *            Bitmap to save.
	 * @param fileName
	 *            File name to save the bitmap as.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	private void saveBitmap(final Bitmap bitmap, final String fileName)
			throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
		File directory = new File(mImageDirPath);
		directory.mkdir();

		File f = new File(directory, fileName);
		f.getParentFile().mkdirs();
		f.createNewFile();
		FileOutputStream fo = new FileOutputStream(f);
		fo.write(bytes.toByteArray());

		fo.close();
	}

	/**
	 * Generates a file name for the current point in time to be used when
	 * saving character bitmaps.
	 *
	 * @return Generated file name.
	 */
	private String generateFileName() {
		return Long.toString(System.currentTimeMillis());
	}

	/**
	 * Clears all input made in the drawing.
	 */
	public void clear() {
		initialize();
	}
}
