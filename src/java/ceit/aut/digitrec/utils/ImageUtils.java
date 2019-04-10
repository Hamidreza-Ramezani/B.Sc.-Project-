package ceit.aut.digitrec.utils;

import jama.Matrix;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import ceit.aut.digitrec.gui.views.DigitView;
import ceit.aut.digitrec.learning.DigitMlp;
import ceit.aut.mlp.Pair;

public class ImageUtils {
	public static final int IMAGE_WIDTH = 20;
	public static final int IMAGE_HEIGHT = 20;

	public static final String CHARACTERS_FILE_NAME = "known_characters.dat";

	/**
	 * List that represents all known character labels with its indices
	 * corresponding to the perceptron output layer indices.
	 */
	private static ArrayList<Character> sCharacters = new ArrayList<Character>();

	/**
	 * Loads a complete dataset of character and stroke map images along with
	 * the labels from a given directory. This is to be used for online
	 * handwriting input.
	 *
	 * @param externalFilesDir
	 *            Directory containing labeled subdirectories, each of which
	 *            contains character datasets.
	 * @return A pair of a dataset and its labels.
	 */
	public static Pair<Matrix> loadHandwritingDataset(File externalFilesDir) {
		return loadDataset(externalFilesDir, true);
	}

	/**
	 * Loads a complete dataset of character images along with the labels from a
	 * given directory. This is to be used for offline (image, camera frames)
	 * handwriting input.
	 *
	 * @param externalFilesDir
	 *            Directory containing labeled subdirectories, each of which
	 *            contains character datasets.
	 * @return A pair of a dataset and its labels.
	 */
	public static Pair<Matrix> loadPictureDataset(File externalFilesDir) {
		return loadDataset(externalFilesDir, false);
	}

	/**
	 * Loads a complete dataset along with its labels from a given directory.
	 *
	 * @param directoryFile
	 *            Directory containing labeled subdirectories, each of which
	 *            contains character datasets.
	 * @param loadStrokeMap
	 *            Whether the stroke maps (stroke end points) images should be
	 *            loaded.
	 * @return A pair of a dataset and its labels.
	 */
	private static Pair<Matrix> loadDataset(final File directoryFile,
			final boolean loadStrokeMap) {
		// Get dataset directories, one for each character
		File[] characterDirs = getDatasetDirs(directoryFile);
		if (characterDirs == null) {
			return null;
		}

		final FilenameFilter imageFileFilter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				return !filename.startsWith(DigitView.STROKE_MAP_PREFIX);
			}
		};

		// Count the files
		int fileCount = 0;
		for (File dir : characterDirs) {
			fileCount += dir.list(imageFileFilter).length;
		}

		// Initialize the dataset and labels
		double[][] dataset = new double[fileCount][IMAGE_WIDTH * IMAGE_HEIGHT
				* (loadStrokeMap ? 2 : 1)];
		double[] labels = new double[fileCount];
		int readCount = 0;
		sCharacters = new ArrayList<Character>();

		// For every character, load all of the examples and put them into
		// a single dataset matrix along with labels
		for (int i = 0; i < characterDirs.length; i++) {
			String[] imageFileNames = characterDirs[i].list(imageFileFilter);
			if (imageFileNames == null) {
				return null;
			}

			// Get the character directory name
			String characterDirName = characterDirs[i].getName();

			// If the character isn't known yet, add it to the list
			addCharacterLabel(characterDirName.charAt(0));

			// Read the character images and save them in the dataset
			for (int j = 0; j < imageFileNames.length; j++) {
				// Read the character pixels into the dataset in both cases
				double[] characterPixels = readImagePixelsFromFile(characterDirs[i]
						.getAbsolutePath()
						+ File.separatorChar
						+ imageFileNames[j]);
				System.arraycopy(characterPixels, 0, dataset[readCount + j], 0,
						characterPixels.length);

				if (loadStrokeMap) {
					// Load the stroke map and put both arrays into the dataset
					double[] strokePointPixels = readImagePixelsFromFile(characterDirs[i]
							.getAbsolutePath()
							+ File.separatorChar
							+ DigitView.STROKE_MAP_PREFIX
							+ imageFileNames[j]);
					if (strokePointPixels != null) {
						System.arraycopy(strokePointPixels, 0,
								dataset[readCount + j], characterPixels.length,
								strokePointPixels.length);
					}
				}

				labels[readCount + j] = encodeLabel(characterDirName.charAt(0));
			}
			readCount += imageFileNames.length;
		}

		// Normalize the dataset values
		Matrix datasetMatrix = new Matrix(dataset).divide(255);
		Matrix labelsMatrix = new Matrix(labels, labels.length);

		return new Pair<Matrix>(datasetMatrix, labelsMatrix);
	}

	/**
	 * Returns a sorted array of directories with character datasets (labeled
	 * subdirectories) or null,
	 *
	 * @param directoryFile
	 *            Directory containing labeled subdirectories, each of which
	 *            contains character datasets.
	 * @return Array of found dataset directories.
	 */
	public static File[] getDatasetDirs(final File directoryFile) {
		File[] characterDirs = directoryFile.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});

		if (characterDirs == null) {
			return null;
		}

		Arrays.sort(characterDirs);

		return characterDirs;
	}

	/**
	 * Encodes a character label into an index - integer value usable in a
	 * neural network.
	 *
	 * @param character
	 *            Character label.
	 * @return Index for a neural network.
	 */
	public static int encodeLabel(final char character) {
		// Return the index in the list of known characters.
		return sCharacters.indexOf(character);
	}

	/**
	 * Decodes a label into a human readable character representing the label.
	 *
	 * @param label
	 *            Label (perceptron output layer index).
	 * @return Human readable character label.
	 */
	public static char decodeLabel(final int label) {
		return sCharacters.get(label);
	}

	/**
	 * Reads pixels from an image file and returns them as a double array
	 * containing grayscale valued bytes.
	 *
	 * @param imageFileName
	 *            Name of the image file to read.
	 * @return Double array of grayscale valued bytes to be used for a matrix,
	 *         or null if the bitmap file could not be decoded.
	 */
	public static double[] readImagePixelsFromFile(final String imageFileName) {
		Bitmap bitmap = BitmapFactory.decodeFile(imageFileName);
		if (bitmap == null) {
			return null;
		}
		return readImagePixelsFromBitmap(bitmap);
	}

	/**
	 * Reads pixels from an image bitmap and returns them as a double array
	 * containing grayscale valued bytes.
	 *
	 * @param bitmap
	 *            Bitmap of the image to read.
	 * @return Double array of grayscale valued bytes to be used for a matrix.
	 */
	public static double[] readImagePixelsFromBitmap(final Bitmap bitmap) {
		int pixels[] = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
		bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(),
				bitmap.getHeight());
		double[] grayscalePixels = new double[pixels.length];
		for (int i = 0; i < pixels.length; i++) {
			grayscalePixels[i] = grayscaleValue(pixels[i]);
		}
		return grayscalePixels;
	}

	/**
	 * Retuns the grayscale value of a Color of a pixel.
	 *
	 * @param pixel
	 *            Color of a pixel as an integer.
	 * @return Grayscale pixel value.
	 */
	private static double grayscaleValue(final int pixel) {
		return Color.blue(pixel);
	}

	/**
	 * Adds a new character label to be used by the {@link DigitMlp}. This
	 * method is required to map the new character to a new index used in the
	 * perceontron's output layer.
	 *
	 * @param character
	 *            New character label.
	 * @return True if the character was new, false otherwise.
	 */
	public static boolean addCharacterLabel(char character) {
		if (!sCharacters.contains(character)) {
			sCharacters.add(character);
			return true;
		}
		return false;
	}

	/**
	 * Saves known character labels to a file.
	 *
	 * @param dir
	 *            The directory the character labels file should be saved into.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public static void saveCharacterLabels(final File dir) throws IOException {
		final File saveFile = new File(dir, CHARACTERS_FILE_NAME);
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				saveFile));
		try {
			oos.writeObject(sCharacters);
		} catch (IOException e) {
			throw e;
		} finally {
			oos.close();
		}
	}

	/**
	 * Loads known character labels from a file.
	 *
	 * @param dir
	 *            The directory where the character labels file is located.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	@SuppressWarnings("unchecked")
	public static void loadCharacterLabels(final File dir) throws IOException {
		final File loadFile = new File(dir, CHARACTERS_FILE_NAME);
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(loadFile));
			sCharacters = (ArrayList<Character>) ois.readObject();
		} catch (IOException e) {
			throw e;
		} catch (ClassNotFoundException e) {
			throw new IOException(e.getMessage());
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					throw e;
				}
			}
		}
	}

	/**
	 * Returns the character label list.
	 *
	 * @return Character label list.
	 */
	public static ArrayList<Character> getCharacterLabels() {
		return sCharacters;
	}

	/**
	 * Converts an OpenCv CV_8U matrix (0-255 ranged values) with RxC size into
	 * a Jama matrix (0.0 - 1.0 ranged values) with 1xR*C size.
	 *
	 * @param inMatrix
	 *            OpenCv CV_8U matrix.
	 * @return Jama matrix.
	 */
	public static Matrix openCvMatToJamaImageMatrix(final Mat inMatrix) {
		// Matrix outMatrix = new Matrix(inMatrix.rows(), inMatrix.cols());
		// for (int i = 0; i < inMatrix.rows(); i++) {
		// for (int j = 0; j < inMatrix.cols(); j++) {
		// outMatrix.set(i, j,
		// inMatrix.get(i, j)[0] / 255.0d);
		// }
		// }
		Matrix outMatrix = new Matrix(1, inMatrix.rows() * inMatrix.cols());
		for (int i = 0; i < inMatrix.rows(); i++) {
			for (int j = 0; j < inMatrix.cols(); j++) {
				outMatrix.set(0, inMatrix.cols() * i + j,
						inMatrix.get(i, j)[0] / 255.0d);
			}
		}
		return outMatrix;
	}

	/**
	 * This method returns a bitmap with the requested dimensions. Note that the
	 * dimensions are not always equal to the requested ones, as aspect ratio is
	 * preserved.
	 *
	 * @param path
	 *            Location of the bitmap file.
	 * @param requestedWidth
	 *            Requested width.
	 * @param requestedHeight
	 *            Requested height.
	 * @return Decoded bitmap with dimensions close to the requested ones.
	 */
	public static Bitmap decodeSampledBitmapFromFile(final String path,
			int requestedWidth, int requestedHeight) {
		// Only check the dimensions at first
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize, requesting chosen bitmap size
		options.inSampleSize = calculateInSampleSize(options, requestedWidth,
				requestedHeight);

		// Decode a bitmap with requested size
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(path, options);
	}

	/**
	 * Calculates the inSampleSize of the bitmap according to the requested
	 * size. Aspect ratio is preserved.
	 *
	 * @param options
	 *            Bitmap options.
	 * @param requestedWidth
	 *            Requested width.
	 * @param requestedHeight
	 *            Requested height.
	 * @return inSampleSize to be used for bitmap creation.
	 */
	private static int calculateInSampleSize(BitmapFactory.Options options,
			int requestedWidth, int requestedHeight) {
		// Get the bitmap size
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > requestedHeight || width > requestedWidth) {

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height
					/ (float) requestedHeight);
			final int widthRatio = Math.round((float) width
					/ (float) requestedWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee a final image with both dimensions larger than or equal
			// to the requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	/**
	 * Resizes the image matrix to fit the given rectangle.
	 *
	 * @param image
	 *            Input image matrix.
	 * @param boundingRect
	 *            Rectangle the image matrix is to be fit to.
	 * @return Resized image matrix.
	 */
	public static Mat getResizedSubImage(final Mat image,
			final Rect boundingRect) {
		// Obtain submatrix within the character bounding rectangle
		Mat subImage = image.submat(boundingRect);
		// Resize the obtained image
		Mat resizedSubImage = new Mat(ImageUtils.IMAGE_HEIGHT,
				ImageUtils.IMAGE_WIDTH, CvType.CV_8UC1);
		Imgproc.resize(subImage, resizedSubImage, resizedSubImage.size(), 0, 0,
				Imgproc.INTER_AREA);
		return resizedSubImage;
	}

	/**
	 * Performs segmentation on a grayscale image matrix with the given
	 * threshold. Before the actual segmentation, median blur of filter size 3
	 * is applied to the input image.
	 *
	 * @param grayscaleImage
	 *            Input grayscale image.
	 * @param threshold
	 *            Threshold for binary segmentation.
	 * @return Filtered and segmented image matrix.
	 */
	public static Mat segmentImage(final Mat grayscaleImage, final int threshold) {
		// Apply median blur
		Mat segmented = grayscaleImage.clone();
		Imgproc.medianBlur(grayscaleImage, segmented, 3);

		// Apply thresholding
		Imgproc.threshold(segmented, segmented, threshold, 255,
				Imgproc.THRESH_BINARY_INV);

		return segmented;
	}
}
