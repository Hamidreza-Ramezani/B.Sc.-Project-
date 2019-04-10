package ceit.aut.digitrec.gui.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ceit.aut.digitrec.gui.models.DigitGridItem;

public class GridItem extends ArrayAdapter<DigitGridItem> {
	private final Context mContext;
	private final LayoutInflater mInflater;
	private final List<DigitGridItem> mList;

	/**
	 * Constructor of the adapter.
	 *
	 * @param context
	 *            Context to which the grid view associated with this adapter
	 *            belogs.
	 * @param itemList
	 *            List of {@link DigitGridItem} items to be appeared in
	 *            the grid.
	 */
	public GridItem(final Context context,
			final List<DigitGridItem> itemList) {
		super(context, R.layout.grid_item_layout, itemList);
		mList = itemList;
		mContext = context;
		mInflater = LayoutInflater.from(context);
	}

	/**
	 * A view holder class that contains the views for a single item.
	 */
	private static class ViewHolder {
		final View rootView;
		final TextView labelView;
		final ImageView imageView;

		public ViewHolder(final View rootView, final TextView labelView,
				final ImageView imageView) {
			this.rootView = rootView;
			this.labelView = labelView;
			this.imageView = imageView;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.grid_item_layout, parent,
					false);
			TextView labelView = (TextView) convertView
					.findViewById(R.id.grid_item_label);
			ImageView imageView = (ImageView) convertView
					.findViewById(R.id.grid_item_image);
			holder = new ViewHolder(convertView, labelView, imageView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		DigitGridItem model = mList.get(position);

		holder.labelView.setText(Character.toString(model.getLabel()));

		if (model.getImage() != null) {
			holder.imageView.setImageBitmap(model.getImage());
		} else {
			holder.imageView.setImageDrawable(mContext.getResources()
					.getDrawable(android.R.drawable.ic_menu_help));
		}

		return holder.rootView;
	}
}
