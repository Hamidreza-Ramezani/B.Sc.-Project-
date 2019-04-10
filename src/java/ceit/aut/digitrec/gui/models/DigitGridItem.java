package ceit.aut.digitrec.gui.models;

import android.graphics.Bitmap;

public class DigitGridItem {
	private char mLabel;
	private Bitmap mImage;

	/**
	 * Default constructor leaving the values uninitialized.
	 */
	public DigitGridItem() {
	}

	/**
	 * Constructor with model values.
	 *
	 * @param label
	 *            Character label.
	 * @param image
	 *            Character image bitmap.
	 */
	public DigitGridItem(final char label, final Bitmap image) {
		mLabel = label;
		mImage = image;
	}

	/**
	 * Returns the character label.
	 *
	 * @return Character label.
	 */
	public char getLabel() {
		return mLabel;
	}

	/**
	 * Sets the character label.
	 *
	 * @param label
	 *            New character label.
	 */
	public void setLabel(final char label) {
		mLabel = label;
	}

	/**
	 * Returns the character image bitmap.
	 *
	 * @return Character image bitmap.
	 */
	public Bitmap getImage() {
		return mImage;
	}

	/**
	 * Sets the character image bitmap.
	 *
	 * @param image
	 *            New character image bitmap.
	 */
	public void setImage(final Bitmap image) {
		mImage = image;
	}
}
