package ceit.aut.mlp;

import java.io.Serializable;

public class Configuration implements Serializable {
	private static final long serialVersionUID = 5493928445306769496L;

	/**
	 * Learning rate of the network used for backpropagation and online
	 * learning.
	 */
	private double mLearningRate;

	/**
	 * The number of iterations in offline learning.
	 */
	private int mLearningIterations;

	/**
	 * Regularization parameter.
	 */
	private double mRegularization;

	/**
	 * etaPlus parameter for the Rprop learning algorithm as described in
	 * original Rprop documentation papers.
	 */
	private double mEtaPlus;

	/**
	 * etaMinus parameter for the Rprop learning algorithm as described in
	 * original Rprop documentation papers.
	 */
	private double mEtaMinus;

	/**
	 * Initial delta value for the Rprop learning algorithm as described in
	 * original Rprop documentation papers.
	 */
	private double mInitDelta;

	/**
	 * Constructs a perceptron configuration.
	 *
	 * @param learningRate
	 *            Learning rate of the network used for backpropagation and
	 *            online learning.
	 * @param learningIterations
	 *            The number of iterations in offline learning.
	 * @param regularization
	 *            Regularization parameter.
	 * @param etaPlus
	 *            etaPlus parameter for the Rprop learning algorithm as
	 *            described in original Rprop documentation papers.
	 * @param etaMinus
	 *            etaMinus parameter for the Rprop learning algorithm as
	 *            described in original Rprop documentation papers.
	 * @param initDelta
	 *            Initial delta value for the Rprop learning algorithm as
	 *            described in original Rprop documentation papers.
	 */
	public Configuration(final double learningRate,
			final int learningIterations, final double regularization,
			final double etaPlus, final double etaMinus, final double initDelta) {
		mLearningRate = learningRate;
		mLearningIterations = learningIterations;
		mRegularization = regularization;
		mEtaPlus = etaPlus;
		mEtaMinus = etaMinus;
		mInitDelta = initDelta;
	}

	/**
	 * Returns the learning rate of the network used for backpropagation and
	 * online learning.
	 *
	 * @return Input layer size.
	 */
	public double getLearningRate() {
		return mLearningRate;
	}

	/**
	 * Sets the learning rate of the network used for backpropagation and online
	 * learning.
	 *
	 * @param learningRate
	 *            Network's learning rate.
	 */
	public void setLearningRate(final double learningRate) {
		mLearningRate = learningRate;
	}

	/**
	 * Gets the number of iterations in offline learning.
	 *
	 * @return The number of learning iterations.
	 */
	public int getLearningIterations() {
		return mLearningIterations;
	}

	/**
	 * Sets the number of iterations in offline learning.
	 *
	 * @param learningIterations
	 *            The number of learning iterations.
	 */
	public void setLearningIterations(final int learningIterations) {
		mLearningIterations = learningIterations;
	}

	/**
	 * Gets the network's regularization parameter.
	 *
	 * @return Network's regularization parameter.
	 */
	public double getReguralization() {
		return mRegularization;
	}

	/**
	 * Sets the network's regularization parameter.
	 *
	 * @param regularization
	 *            Network's regularization parameter.
	 */
	public void setRegularization(final double regularization) {
		mRegularization = regularization;
	}

	/**
	 * Gets the etaPlus parameter for the Rprop learning algorithm as described
	 * in original Rprop documentation papers.
	 *
	 * @return Network's etaPlus parameter.
	 */
	public double getEtaPlus() {
		return mEtaPlus;
	}

	/**
	 * Sets the etaPlus parameter for the Rprop learning algorithm as described
	 * in original Rprop documentation papers.
	 *
	 * @param etaPlus
	 *            Network's etaPlus parameter.
	 */
	public void setEtaPlus(final double etaPlus) {
		mEtaPlus = etaPlus;
	}

	/**
	 * Gets the etaMinus parameter for the Rprop learning algorithm as described
	 * in original Rprop documentation papers.
	 *
	 * @return Network's etaMinus parameter.
	 */
	public double getEtaMinus() {
		return mEtaMinus;
	}

	/**
	 * Sets the etaMinus parameter for the Rprop learning algorithm as described
	 * in original Rprop documentation papers.
	 *
	 * @param etaMinus
	 *            Network's etaMinus parameter.
	 */
	public void setEtaMinus(final double etaMinus) {
		mEtaMinus = etaMinus;
	}

	/**
	 * Gets the initial delta value for the Rprop learning algorithm as
	 * described in original Rprop documentation papers.
	 *
	 * @return Network's initial delta parameter.
	 */
	public double getInitDelta() {
		return mInitDelta;
	}

	/**
	 * Sets the initial delta value for the Rprop learning algorithm as
	 * described in original Rprop documentation papers.
	 *
	 * @param initDelta
	 *            Network's initial delta parameter.
	 */
	public void setInitDelta(final double initDelta) {
		mInitDelta = initDelta;
	}
}
