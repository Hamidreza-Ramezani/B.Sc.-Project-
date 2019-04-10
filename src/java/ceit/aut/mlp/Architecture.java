package ceit.aut.mlp;

import java.io.Serializable;

public class Architecture implements Serializable {
	private static final long serialVersionUID = -3649536621889483006L;

	private int mInputLayerSize;
	private int mHiddenLayerSize;
	private int mLabelCount;

	/**
	 * Constructs a perceptron architecture with the specified layer sizes.
	 *
	 * @param inputLayerSize
	 *            Input layer size.
	 * @param hiddenLayerSize
	 *            Hidden layer size.
	 * @param labelCount
	 *            Label count (output layer size).
	 */
	public Architecture(final int inputLayerSize, final int hiddenLayerSize,
			final int labelCount) {
		mInputLayerSize = inputLayerSize;
		mHiddenLayerSize = hiddenLayerSize;
		mLabelCount = labelCount;
	}

	/**
	 * Returns the input layer size.
	 *
	 * @return Input layer size.
	 */
	public int getInputLayerSize() {
		return mInputLayerSize;
	}

	/**
	 * Sets the input layer size.
	 *
	 * @param inputLayerSize
	 *            Input layer size.
	 */
	public void setInputLayerSize(final int inputLayerSize) {
		mInputLayerSize = inputLayerSize;
	}

	/**
	 * Returns the hidden layer size.
	 *
	 * @return Hidden layer size.
	 */
	public int getHiddenLayerSize() {
		return mHiddenLayerSize;
	}

	/**
	 * Sets the hidden layer size.
	 *
	 * @param hiddenLayerSize
	 *            Hidden layer size.
	 */
	public void setHiddenLayerSize(final int hiddenLayerSize) {
		mHiddenLayerSize = hiddenLayerSize;
	}

	/**
	 * Returns the label count (output layer size).
	 *
	 * @return Label count.
	 */
	public int getLabelCount() {
		return mLabelCount;
	}

	/**
	 * Sets the label count (output layer size).
	 *
	 * @param labelCount
	 *            Label count.
	 */
	public void setLabelCount(final int labelCount) {
		mLabelCount = labelCount;
	}
}
