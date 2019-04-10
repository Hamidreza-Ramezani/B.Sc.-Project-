package ceit.aut.digitrec.gui.views;

import java.util.Collection;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;
import ceit.aut.digitrec.gui.adapters.GridItem;
import ceit.aut.digitrec.gui.models.DigitGridItem;

public class DigitGridView extends GridView {

	/**
	 * Constructor with no attributes.
	 *
	 * @param context
	 *            Context associated with the view.
	 */
	public DigitGridView(Context context) {
		super(context);
	}

	/**
	 * Constructor with a context and view attributes.
	 *
	 * @param context
	 *            Context associated with the view.
	 * @param attrs
	 *            View attributes.
	 */
	public DigitGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Constructor with a context and view attributes.
	 *
	 * @param context
	 *            Context associated with the view.
	 * @param attrs
	 *            View attributes.
	 * @param defStyle
	 *            The default style to apply to this view. If 0, no style will
	 *            be applied (beyond what is included in the theme). This may
	 *            either be an attribute resource, whose value will be retrieved
	 *            from the current theme, or an explicit style resource.
	 */
	public DigitGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Adds a character grid item to the view.
	 *
	 * @param item
	 *            Character grid item model.
	 */
	public void add(final DigitGridItem item) {
		if (getAdapter() instanceof GridItem) {
			GridItem adapter = (GridItem) getAdapter();
			adapter.add(item);
		}
	}

	/**
	 * Adds an array of character grid items to the view.
	 *
	 * @param items
	 *            Character grid item models.
	 */
	public void addAll(final DigitGridItem... items) {
		if (getAdapter() instanceof GridItem) {
			GridItem adapter = (GridItem) getAdapter();
			for (DigitGridItem item : items) {
				adapter.add(item);
			}
		}
	}

	/**
	 * Adds an array of character grid items to the view.
	 *
	 * @param items
	 *            Character grid item models.
	 */
	public void addAll(final Collection<? extends DigitGridItem> items) {
		if (getAdapter() instanceof GridItem) {
			GridItem adapter = (GridItem) getAdapter();
			for (DigitGridItem item : items) {
				adapter.add(item);
			}
		}
	}

	/**
	 * Returns the character model at the given position in the view.
	 *
	 * @param position
	 *            Position in the view whose character model is requested.
	 * @return Character model at the given position.
	 */
	public DigitGridItem getItem(final int position) {
		return (DigitGridItem) getItemAtPosition(position);
	}
}
