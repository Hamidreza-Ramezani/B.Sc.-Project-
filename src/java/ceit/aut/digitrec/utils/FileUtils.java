package ceit.aut.digitrec.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import ceit.aut.digitrec.learning.DigitMlp;
import ceit.aut.mlp.MlpIOException;

public class FileUtils {
	/**
	 * SHA-1 checksum of the initial file.
	 */
	public static final String INIT_FILE_CHECKSUM = "aa684ead2e3e8bdcf1b499f7de95e3ec63278925";

	/**
	 * Constructs an IntentFilter with actions set to be passed to a module that
	 * handles the change of external storage state.
	 *
	 * @return Constructed IntentFilter for listening for external storage state
	 *         changes.
	 */
	public static IntentFilter getExternalStorageChangeFilter() {
		final IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		return filter;
	}

	/**
	 * Unzips an input stream and saves its content to a directory specified by
	 * the location.
	 *
	 * @param inputStream
	 *            Zipped input stream.
	 * @param location
	 *            The directory where the output files should be saved.
	 * @throws IOException
	 *             When a file IO exception occurs while unzipping or copying
	 *             data.
	 */
	public static void unzip(final InputStream inputStream, final File location)
			throws IOException {
		final ZipInputStream zis = new ZipInputStream(inputStream);
		ZipEntry entry = null;
		while ((entry = zis.getNextEntry()) != null) {
			if (entry.isDirectory()) {
				new File(location, entry.getName()).mkdirs();
			} else {
				FileOutputStream fos = new FileOutputStream(new File(location,
						entry.getName()));
				for (int b = zis.read(); b != -1; b = zis.read()) {
					fos.write(b);
				}
				fos.close();
			}
			zis.closeEntry();
		}
		zis.close();
	}

	/**
	 * Verifies the initial file as a raw resource against the SHA-1 checksum.
	 * This prevents any malicious code that could otherwise be injected in the
	 * APK and copied to the device's external storage.
	 *
	 * @param context
	 *            Context used for opening the raw resource.
	 * @return True if the file is correct, false otherwise.
	 */
	public static boolean isInitFileCorrect(Context context) {
		InputStream stream = context.getResources().openRawResource(
				R.raw.init_data);

		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] dataBytes = new byte[1024];

			int nread = 0;

			while ((nread = stream.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			;

			byte[] mdbytes = md.digest();

			// convert the byte to hex format
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
						.substring(1));
			}

			String checksum = sb.toString();
			return checksum.equals(INIT_FILE_CHECKSUM);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * Checks and returns whether a perceptron for handwriting exists.
	 *
	 * @return True if a file with saved perceptron exists.
	 */
	public static boolean savedPerceptronExists(final Context context,
			final String fileName) {
		File storageDir = context.getExternalFilesDir(null);
		File weightsFile = new File(storageDir, fileName);
		return weightsFile.exists();
	}

	/**
	 * Saves a perceptron to a file.
	 *
	 * @param mlp
	 *            Multilayer perceptron.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public static void savePerceptron(final Context context,
			final DigitMlp mlp, final String fileName) throws IOException {
		File storageDir = context.getExternalFilesDir(null);
		File mlpFile = new File(storageDir, fileName);
		mlp.saveToFile(mlpFile);
	}

	/**
	 * Loads a character perceptron from a file.
	 *
	 * @return Loaded perceptron for character recognition.
	 * @throws MlpIOException
	 *             If an IO error regarding a multilayer perceptron occurs.
	 */
	public static DigitMlp loadPerceptron(final Context context,
			final String fileName) throws MlpIOException {
		File storageDir = context.getExternalFilesDir(null);
		File mlpFile = new File(storageDir, fileName);
		return DigitMlp.loadFromFile(mlpFile);
	}

	/**
	 * Loads the known character labels for a neural network from a file.
	 */
	public static void loadCharacterLabels(final Context context) {
		try {
			ImageUtils.loadCharacterLabels(context.getExternalFilesDir(null));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
