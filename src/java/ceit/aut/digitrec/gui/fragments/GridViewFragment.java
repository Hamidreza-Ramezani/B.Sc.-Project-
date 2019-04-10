package ceit.aut.digitrec.gui.fragments;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import ceit.aut.digitrec.gui.activities.TouchMode;
import ceit.aut.digitrec.gui.adapters.GridItem;
import ceit.aut.digitrec.gui.models.DigitGridItem;
import ceit.aut.digitrec.gui.views.DigitGridView;
import ceit.aut.digitrec.utils.ImageUtils;

public class GridViewFragment extends Fragment {
	private DigitGridView mCharacterGrid;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mCharacterGrid = (DigitGridView) inflater.inflate(
				R.layout.character_grid, container, false);
		mCharacterGrid.setAdapter(new GridItem(mCharacterGrid
				.getContext(), new ArrayList<DigitGridItem>()));
		loadGridItems();
		return mCharacterGrid;
	}

	/**
	 * Starts an asynchronous process that loads the GridView items.
	 */
	private void loadGridItems() {
		new AsyncTask<Void, Void, DigitGridItem[]>() {

			@Override
			protected DigitGridItem[] doInBackground(Void... params) {
				// Try to load the known character list
				try {
					ImageUtils.loadCharacterLabels(getActivity()
							.getExternalFilesDir(null));
				} catch (IOException e) {
					e.printStackTrace();
				}
				// Get the character dataset directory list
				List<String> datasetDirs = Arrays.asList(getActivity()
						.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
						.list());
				// Initialize models for all known characters
				DigitGridItem[] models = new DigitGridItem[ImageUtils
						.getCharacterLabels().size()];

				// Fill the models array
				for (int i = 0; i < models.length; i++) {
					models[i] = new DigitGridItem();
					models[i].setLabel(ImageUtils.getCharacterLabels().get(i));

					final String label = Character.toString(models[i]
							.getLabel());

					if (datasetDirs.contains(label)) {
						File[] datasets = new File(getActivity()
								.getExternalFilesDir(
										Environment.DIRECTORY_PICTURES),
								datasetDirs.get(datasetDirs.indexOf(label)))
								.listFiles();
						if (datasets != null && datasets.length > 0) {
							models[i].setImage(BitmapFactory
									.decodeFile(datasets[0].getAbsolutePath()));
						}
					}
				}
				return models;
			}

			@Override
			protected void onPostExecute(DigitGridItem[] result) {
				mCharacterGrid.addAll(result);
				mCharacterGrid.setOnItemClickListener(mOnItemClickListener);
			}
		}.execute();
	}

	private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// Get the character label
			final char clickedCharacter = mCharacterGrid.getItem(position)
					.getLabel();
			// Start the handwriting activity in character learning mode
			Intent intent = new Intent();
			intent.setAction(TouchMode.TRAIN_CHAR).setClass(getActivity(), TouchMode.class);
			intent.putExtra(TouchMode.KEY, clickedCharacter);
			startActivity(intent);
		};
	};
}
