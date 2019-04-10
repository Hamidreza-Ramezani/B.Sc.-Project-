package ceit.aut.mlp;

import jama.Matrix;

import java.io.Serializable;

public class MultilayerPerceptron implements Serializable {
	private static final long serialVersionUID = -5243301476595755280L;

	private double mLearningRate;
	private int mIterationCount;
	private double mRegularization;
	private double mEtaPlus;
	private double mEtaMinus;
	private double mInitDelta;

	/** Input layer weights - Theta1 */
	private Matrix mInputWeights;
	/** Hidden layer weights - Theta2 */
	private Matrix mHiddenWeights;

	/** Last data instance (feature vector) used as feedback for online learning */
	private Matrix mLastDataInstance;

	/** Perceptron architecture */
	private Architecture mArchitecture;

	/**
	 * Constructs a perceptron with the given architecture and configuration.
	 *
	 * @param architecture
	 *            Network architecture (layer sizes).
	 * @param configuration
	 *            Network configuration (learning parameters).
	 */
	public MultilayerPerceptron(final Architecture architecture,
			final Configuration configuration) {
		mArchitecture = architecture;
		mLearningRate = configuration.getLearningRate();
		mIterationCount = configuration.getLearningIterations();
		mRegularization = configuration.getReguralization();
		mEtaPlus = configuration.getEtaPlus();
		mEtaMinus = configuration.getEtaMinus();
		mInitDelta = configuration.getInitDelta();
	}

	/**
	 * Returns a new instance of a {@link MultilayerPerceptron} with the given
	 * architecture and configuration.
	 *
	 * @param architecture
	 *            Network architecture (layer sizes).
	 * @param configuration
	 *            Network configuration (learning parameters).
	 */
	public static MultilayerPerceptron newInstance(
			final Architecture architecture,
			final Configuration configuration) {
		return new MultilayerPerceptron(architecture, configuration);
	}

	/**
	 * Makes a prediction on the given input dataset and returns the index of
	 * the label with maximum prediction value.
	 *
	 * @param dataset
	 *            A 1xN matrix containing a single instance of an input dataset.
	 * @return Index of the predicted label.
	 */
	public int predictIndex(final Matrix dataset) {
		mLastDataInstance = dataset;

		Matrix imageWithBaseParam = addOnesHorizontally(dataset);

		Matrix layer1Outputs = imageWithBaseParam.times(
				mInputWeights.transpose()).sigmoid();
		Matrix layer1OutputsWithBias = addOnesHorizontally(layer1Outputs);
		Matrix layer2Outputs = layer1OutputsWithBias.times(
				mHiddenWeights.transpose()).sigmoid();
		return layer2Outputs.maxIndex();
	}

	/**
	 * Trains the network according to specified parameters. The perceptron
	 * contains 1 hidden layer. This learning method uses pure backpropagation
	 * algorithm.
	 *
	 * @param dataset
	 *            Training examples.
	 * @param labels
	 *            Labels for training examples.
	 */
	public void backpropTrain(final Matrix dataset, final Matrix labels) {
		// Obtain architecture values
		int inputLayerSize = dataset.getColumnDimension();
		int hiddenLayerSize = mArchitecture.getHiddenLayerSize();
		int labelCount = mArchitecture.getLabelCount();
		mArchitecture.setInputLayerSize(inputLayerSize);

		randomizeWeights(inputLayerSize, hiddenLayerSize, labelCount);

		int dataSize = dataset.getRowDimension();

		// Labels transformation to contain vectors with a label (0|1) for
		// each example
		Matrix transformedLabels = new Matrix(dataSize, labelCount);
		for (int i = 0; i < dataSize; i++) {
			transformedLabels.set(i, (int) labels.get(i, 0), 1);
		}

		for (int i = 0; i < mIterationCount; i++) {
			// Obtain unregularized gradients
			Pair<Matrix> gradients = getGradients(dataset, transformedLabels);

			// Regularize the gradients
			regularizeGradients(gradients, dataSize);

			// Perform gradient descent
			backpropGradientDescent(gradients);
		}
	}

	/**
	 * Trains the network according to specified parameters. The perceptron
	 * contains 1 hidden layer. This learning method uses Rprop algorithm.
	 *
	 * @param dataset
	 *            Training examples.
	 * @param labels
	 *            Labels for training examples.
	 */
	public void rpropTrain(final Matrix dataset, final Matrix labels) {
		// Obtain architecture values
		int inputLayerSize = dataset.getColumnDimension();
		int hiddenLayerSize = mArchitecture.getHiddenLayerSize();
		int labelCount = mArchitecture.getLabelCount();
		mArchitecture.setInputLayerSize(inputLayerSize);

		randomizeWeights(inputLayerSize, hiddenLayerSize, labelCount);

		int dataSize = dataset.getRowDimension();

		// Labels transformation to contain vectors with a label (0|1) for
		// each example
		Matrix transformedlabels = new Matrix(dataSize, labelCount);
		for (int i = 0; i < dataSize; i++) {
			transformedlabels.set(i, (int) labels.get(i, 0), 1);
		}

		// Initialize matrices
		Matrix gradientDelta1 = null;
		Matrix gradientDelta2 = null;
		Matrix prevTheta1Gradient = null;
		Matrix prevTheta2Gradient = null;

		for (int i = 0; i < mIterationCount; i++) {
			// Obtain unregularized gradients
			Pair<Matrix> gradients = getGradients(dataset, transformedlabels);
			Matrix theta1Gradient = gradients.first;
			Matrix theta2Gradient = gradients.second;

			// Regularize the gradients
			regularizeGradients(gradients, dataSize);

			// Perform gradient descent
			if (gradientDelta1 == null) {
				gradientDelta1 = Matrix.ones(theta1Gradient.getRowDimension(),
						theta1Gradient.getColumnDimension()).times(mInitDelta);
			}
			if (gradientDelta2 == null) {
				gradientDelta2 = Matrix.ones(theta2Gradient.getRowDimension(),
						theta2Gradient.getColumnDimension()).times(mInitDelta);
			}
			if (prevTheta1Gradient == null) {
				prevTheta1Gradient = Matrix.ones(
						theta1Gradient.getRowDimension(),
						theta1Gradient.getColumnDimension());
			}
			if (prevTheta2Gradient == null) {
				prevTheta2Gradient = Matrix.ones(
						theta2Gradient.getRowDimension(),
						theta2Gradient.getColumnDimension());
			}
			rpropGradientDescent(theta1Gradient, theta2Gradient,
					prevTheta1Gradient, prevTheta2Gradient, gradientDelta1,
					gradientDelta2);
			prevTheta1Gradient = theta1Gradient.copy();
			prevTheta2Gradient = theta2Gradient.copy();
		}
	}

	/**
	 * Performs online learning on the last predicted instance of data.
	 *
	 * @param labelIndex
	 *            The output layer index of the taught label.
	 * @param learningRate
	 *            The learning rate to use to perform weight-updates.
	 */
	public void onlineLearn(final int labelIndex, final double learningRate) {
		if (mLastDataInstance == null) {
			return;
		}

		// Obtain architecture values
		int labelCount = mArchitecture.getLabelCount();

		// Boundary check
		if (labelIndex >= labelCount || labelIndex < 0) {
			return;
		}

		// Label vector that contains a label (0|1) for each example
		Matrix labelVector = new Matrix(1, labelCount);
		for (int i = 0; i < labelCount; i++) {
			labelVector.set(0, i, i == labelIndex ? 1 : 0);
		}

		// Obtain unregularized gradients
		Pair<Matrix> gradients = getGradients(mLastDataInstance, labelVector);

		// Regularize the gradients
		regularizeGradients(gradients, 1);

		// Perform gradient descent
		backpropGradientDescent(gradients, learningRate);
	}

	/**
	 * Obtains and returns yet unregularized gradients on the given dataset and
	 * labels according to a perceptron architecture.
	 *
	 * @param dataset
	 *            A set of training examples.
	 * @param labels
	 *            A set of labels for the dataset.
	 * @return A pair of gradient matrices.
	 */
	private Pair<Matrix> getGradients(final Matrix dataset, final Matrix labels) {
		// Obtain architecture values
		int inputLayerSize = mArchitecture.getInputLayerSize();
		int hiddenLayerSize = mArchitecture.getHiddenLayerSize();
		int labelCount = mArchitecture.getLabelCount();

		// Obtain dataset dimensions
		int dataSize = dataset.getRowDimension();
		int dataColumnCount = dataset.getColumnDimension();

		// Initialize deltas, perform backpropagation and obtain the gradients
		Matrix delta1 = new Matrix(hiddenLayerSize, inputLayerSize + 1);
		Matrix delta2 = new Matrix(labelCount, hiddenLayerSize + 1);
		for (int i = 0; i < dataSize; i++) {
			// Forward propagation
			Matrix layer1 = addBiasNeuron(dataset.getMatrix(i, i, 0,
					dataColumnCount - 1).transpose());
			Matrix weightedLayer1 = mInputWeights.times(layer1);
			Matrix activationLayer1 = weightedLayer1.sigmoid();
			activationLayer1 = addBiasNeuron(activationLayer1);
			Matrix weightedLayer2 = mHiddenWeights.times(activationLayer1);
			Matrix activationLayer2 = weightedLayer2.sigmoid();

			// Initial error term
			Matrix deltaLayer3 = activationLayer2.minus(labels.getMatrix(i, i,
					0, labelCount - 1).transpose());
			// Error term for the hidden layer
			Matrix deltaLayer2 = mHiddenWeights.transpose().times(deltaLayer3)
					.arrayTimes(sigmoidGradient(addBiasNeuron(weightedLayer1)));

			// We do not need deltaLayer2(1)
			deltaLayer2 = deltaLayer2.getMatrix(1,
					deltaLayer2.getRowDimension() - 1, 0, 0);

			// Accumulate error terms
			delta1.plusEquals(deltaLayer2.times(layer1.transpose()));
			delta2.plusEquals(deltaLayer3.times(activationLayer1.transpose()));
		}
		// Obtain the gradients
		Matrix theta1Gradient = delta1.divide(dataSize);
		Matrix theta2Gradient = delta2.divide(dataSize);

		return new Pair<Matrix>(theta1Gradient, theta2Gradient);
	}

	/**
	 * Regularizes gradients according to the global regularization parameter.
	 *
	 * @param gradients
	 *            The unregularized gradients to regularize.
	 * @param dataSize
	 *            The size of the dataset on which the gradients have been
	 *            obtained.
	 */
	private void regularizeGradients(final Pair<Matrix> gradients,
			final int dataSize) {
		Matrix theta1Gradient = gradients.first;
		Matrix theta2Gradient = gradients.second;

		theta1Gradient.getMatrix(0, theta1Gradient.getRowDimension() - 1, 1,
				theta1Gradient.getColumnDimension() - 1).plusEquals(
				mInputWeights
						.getMatrix(0, mInputWeights.getRowDimension() - 1, 1,
								mInputWeights.getColumnDimension() - 1)
						.times(mRegularization).divide(dataSize));
		theta2Gradient.getMatrix(0, theta2Gradient.getRowDimension() - 1, 1,
				theta2Gradient.getColumnDimension() - 1).plusEquals(
				mHiddenWeights
						.getMatrix(0, mHiddenWeights.getRowDimension() - 1, 1,
								mHiddenWeights.getColumnDimension() - 1)
						.times(mRegularization).divide(dataSize));
	}

	private void randomizeWeights(int inputLayerSize, int hiddenLayerSize,
			int labelCount) {
		mInputWeights = randomWeights(inputLayerSize, hiddenLayerSize);
		mHiddenWeights = randomWeights(hiddenLayerSize, labelCount);
	}

	/**
	 * Initializes a matrix with random weights.
	 *
	 * @param inLayerCount
	 *            Number of neurons in the input layer.
	 * @param outLayerCount
	 *            Number of neurons in the output layer.
	 * @return Matrix initialized with random weights.
	 */
	private Matrix randomWeights(final int inLayerCount, final int outLayerCount) {
		double initMargin = 0.12;
		return Matrix.random(outLayerCount, 1 + inLayerCount).times(2)
				.times(initMargin).minus(initMargin);
	}

	/**
	 * Adds ones to the first column of each row of a matrix.
	 *
	 * @param inMatrix
	 *            Original matrix.
	 * @return Matrix padded with ones.
	 */
	private Matrix addOnesHorizontally(final Matrix inMatrix) {
		int m = inMatrix.getRowDimension();
		int n = inMatrix.getColumnDimension();
		Matrix matrixWithBiases = new Matrix(m, n + 1);
		matrixWithBiases.setMatrix(0, m - 1, 0, 0, Matrix.ones(m, 1));
		matrixWithBiases.setMatrix(0, m - 1, 1, n - 1, inMatrix);
		return matrixWithBiases;
	}

	/**
	 * Adds 1 to the first row of a vertical vector.
	 *
	 * @param layer
	 *            Original vector.
	 * @return Matrix padded with 1.
	 */
	private Matrix addBiasNeuron(final Matrix layer) {
		int length = layer.getRowDimension();
		Matrix layerWithBias = new Matrix(length + 1, 1);
		layerWithBias.set(0, 0, 1);
		layerWithBias.setMatrix(1, length - 1, 0, 0, layer);
		return layerWithBias;
	}

	/**
	 * Same as {@link #backpropGradientDescent(Pair, double)}, but a default
	 * learning rate is used.
	 *
	 * @param thetaGradients
	 *            A pair of theta gradients for the output and the hidden layer.
	 */
	private void backpropGradientDescent(final Pair<Matrix> thetaGradients) {
		backpropGradientDescent(thetaGradients, mLearningRate);
	}

	/**
	 * Performs gradient descent (for pure backpropagation) on Theta parameters
	 * according to the learning rate.
	 *
	 * @param thetaGradients
	 *            A pair of theta gradients for the output and the hidden layer.
	 * @param learningRate
	 *            The learning rate to use when updating weights.
	 */
	private void backpropGradientDescent(final Pair<Matrix> thetaGradients,
			double learningRate) {
		mInputWeights = mInputWeights.minus(thetaGradients.first
				.times(learningRate));
		mHiddenWeights = mHiddenWeights.minus(thetaGradients.second
				.times(learningRate));
	}

	/**
	 * Performs gradient descent on Theta parameters using the RPROP- version of
	 * the Rprop algorithm (no weight-backtracking).
	 *
	 * @param theta1Gradient
	 *            Theta 1 gradient.
	 * @param theta2Gradient
	 *            Theta 2 gradient.
	 * @param prevTheta1Gradient
	 *            Theta 1 gradient from last iteration.
	 * @param prevTheta2Gradient
	 *            Theta 2 gradient from last iteration.
	 * @param prevStepSize1
	 *            Step size (weight change amount) from last iteration for the
	 *            input layer.
	 * @param prevStepSize2
	 *            Step size (weight change amount) from last iteration for the
	 *            hidden layer.
	 */
	private void rpropGradientDescent(final Matrix theta1Gradient,
			final Matrix theta2Gradient, final Matrix prevTheta1Gradient,
			final Matrix prevTheta2Gradient, final Matrix prevStepSize1,
			final Matrix prevStepSize2) {
		// Gradient descent for Rprop - no weight-backtracking
		Matrix stepSize1 = prevStepSize1;
		Matrix stepSize2 = prevStepSize2;

		for (int row = 0; row < stepSize1.getRowDimension(); row++) {
			for (int col = 0; col < stepSize1.getColumnDimension(); col++) {
				if (prevTheta1Gradient.get(row, col)
						* theta1Gradient.get(row, col) > 0) {
					stepSize1.set(row, col, prevStepSize1.get(row, col)
							* mEtaPlus);
				} else if (prevTheta1Gradient.get(row, col)
						* theta1Gradient.get(row, col) < 0) {
					stepSize1.set(row, col, prevStepSize1.get(row, col)
							* mEtaMinus);
				}
				mInputWeights.set(
						row,
						col,
						mInputWeights.get(row, col)
								- Math.signum(theta1Gradient.get(row, col))
								* stepSize1.get(row, col));
			}
		}

		for (int row = 0; row < stepSize2.getRowDimension(); row++) {
			for (int col = 0; col < stepSize2.getColumnDimension(); col++) {
				if (prevTheta2Gradient.get(row, col)
						* theta2Gradient.get(row, col) > 0) {
					stepSize2.set(row, col, prevStepSize2.get(row, col)
							* mEtaPlus);
				} else if (prevTheta2Gradient.get(row, col)
						* theta2Gradient.get(row, col) < 0) {
					stepSize2.set(row, col, prevStepSize2.get(row, col)
							* mEtaMinus);
				}
				mHiddenWeights.set(
						row,
						col,
						mHiddenWeights.get(row, col)
								- Math.signum(theta2Gradient.get(row, col))
								* stepSize2.get(row, col));
			}
		}
	}

	/**
	 * Returns the gradient of the sigmoid function evaluated at inMatrix.
	 *
	 * @param inMatrix
	 *            Matrix on which sigmoid is calculated.
	 * @return Gradient of the sigmoid function evaluated at inMatrix.
	 */
	private Matrix sigmoidGradient(final Matrix inMatrix) {
		Matrix sigmoid = inMatrix.sigmoid();
		return sigmoid.arrayTimes(sigmoid.uminus().plus(1));
	}

	/**
	 * Return the assigned perceptron architecture.
	 *
	 * @see Architecture
	 * @return Assigned perceptron architecture.
	 */
	public Architecture getArchitecture() {
		return mArchitecture;
	}

	/**
	 * Changes the label count (size of the output layer) to the specified
	 * number. If the new size exceeds the old size, random weights are
	 * appended; if it is lower, extra weights are cut off.
	 *
	 * @param labelCount
	 *            New label count.
	 */
	public void setLabelCount(final int labelCount) {
		if (mArchitecture.getLabelCount() == labelCount) {
			return;
		}
		// Change the architecture
		mArchitecture.setLabelCount(labelCount);
		// Hidden weights need to change the row dimension - first create a new
		// matrix with random weights with the right dimensions
		Matrix newHiddenWeights = randomWeights(
				mArchitecture.getHiddenLayerSize(), labelCount);
		// Then set its submatrix to the original weights matrix
		newHiddenWeights.setMatrix(0,
				Math.min(mHiddenWeights.getRowDimension(), labelCount) - 1, 0,
				mHiddenWeights.getColumnDimension() - 1, mHiddenWeights);
		// Finally, replace the original weights with the resized weights matrix
		mHiddenWeights = newHiddenWeights;
	}
}