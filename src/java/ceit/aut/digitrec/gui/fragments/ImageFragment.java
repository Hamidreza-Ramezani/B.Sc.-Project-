package ceit.aut.digitrec.gui.fragments;

import org.opencv.core.Mat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import ceit.aut.digitrec.gui.activities.ImageMode;

public class ImageFragment extends Fragment {
	private Bitmap mImageBitmap;
	private Mat mOriginalImageMat;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.image_fragment, null);
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof ImageMode && mImageBitmap != null
				&& mOriginalImageMat != null) {
			ImageMode imageActivity = (ImageMode) activity;
			imageActivity.setOriginalImageBitmap(mImageBitmap);
			imageActivity.setOriginalImageMat(mOriginalImageMat);
		}
	}

	/**
	 * Sets the image bitmap to retain in the fragment.
	 *
	 * @param bitmap
	 *            Bitmap shown in {@link ImageMode}.
	 */
	public void setImageBitmap(final Bitmap bitmap) {
		mImageBitmap = bitmap;
	}

	/**
	 * Sets the image matrix to retain in the fragment.
	 *
	 * @param mat
	 *            Matrix held in {@link ImageMode}.
	 */
	public void setOriginalImageMat(final Mat mat) {
		mOriginalImageMat = mat;
	}
}
