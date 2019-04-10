package ceit.aut.digitrec.learning;

import jama.Matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import android.content.Context;
import android.widget.Toast;
import ceit.aut.digitrec.gui.activities.Settings;
import ceit.aut.digitrec.utils.ImageUtils;
import ceit.aut.mlp.Architecture;
import ceit.aut.mlp.Configuration;
import ceit.aut.mlp.MlpIOException;
import ceit.aut.mlp.MultilayerPerceptron;

public class DigitMlp extends MultilayerPerceptron {
	private static final long serialVersionUID = 8168162459386613767L;

	public static final double LEARNING_RATE = 0.3;
	private static final int NUM_ITERATIONS = 100;
	private static final double REGULARIZATION_PARAMETER = 0;
	private static final double ETA_PLUS = 1.2;
	private static final double ETA_MINUS = 0.5;
	private static final double INIT_DELTA = 0.01;

	/**
	 * Constructs a new multilayer perceptron to be used for character
	 * recognition.
	 *
	 * @param architecture
	 *            Multilayer perceptron architecture.
	 */
	public DigitMlp(final Architecture architecture) {
		super(architecture, new Configuration(LEARNING_RATE, NUM_ITERATIONS,
				REGULARIZATION_PARAMETER, ETA_PLUS, ETA_MINUS, INIT_DELTA));
	}

	/**
	 * Predicts a character in a given image.
	 *
	 * @param dataset
	 *            A 1xN matrix containing a single instance of a dataset.
	 * @return Character predicted based on the image.
	 */
	public char predictCharacter(final Matrix dataset) {
		return ImageUtils.decodeLabel(predictIndex(dataset));
	}

	/**
	 * Performs online learning, assigning a new character label to the last
	 * predicted instance of data.
	 *
	 * @see MultilayerPerceptron#onlineLearn(int, double)
	 *
	 * @param context
	 *            Context for toast and saved values access.
	 * @param character
	 *            Character to teach.
	 */
	public void characterOnlineLearn(final Context context, final char character) {
		int label = ImageUtils.encodeLabel(character);
		if (label == -1 || label >= ImageUtils.getCharacterLabels().size()) {
			Toast.makeText(context, context.getString(R.string.unknown_char),
					Toast.LENGTH_LONG).show();
			return;
		}
		onlineLearn(label, Settings.getOnlineLearningRate(context));
	}

	/**
	 * Saves this perceptron to a file.
	 *
	 * @param file
	 *            Output file.
	 * @throws IOException
	 *             If an IO error occurs.
	 */
	public void saveToFile(final File file) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				file));
		try {
			oos.writeObject(this);
		} catch (IOException e) {
			throw e;
		} finally {
			oos.close();
		}
	}

	/**
	 * Loads a perceptron from a file.
	 *
	 * @param file
	 *            Input file.
	 * @return Loaded character perceptron.
	 * @throws MlpIOException
	 *             If an error occurs while reading a perceptron from the file.
	 */
	public static DigitMlp loadFromFile(final File file)
			throws MlpIOException {
		ObjectInputStream ois = null;
		DigitMlp mlp = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(file));
			mlp = (DigitMlp) ois.readObject();
		} catch (StreamCorruptedException e) {
			throw new MlpIOException("Failed to load the perceptron.", e);
		} catch (IOException e) {
			throw new MlpIOException("Failed to load the perceptron.", e);
		} catch (ClassNotFoundException e) {
			throw new MlpIOException("Failed to load the perceptron.", e);
		} finally {
			try {
				ois.close();
			} catch (IOException e) {
				throw new MlpIOException("Failed to load the perceptron.", e);
			}
		}
		return mlp;
	}
}
