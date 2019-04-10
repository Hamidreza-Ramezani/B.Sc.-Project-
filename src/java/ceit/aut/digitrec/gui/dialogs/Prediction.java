package ceit.aut.digitrec.gui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Prediction extends DialogFragment {
	public static final String TAG = "prediction_dialog";
	public static final String KEY_CHARACTER = "predicted_character";
	private Character mPredictedCharacter;
	private PredictionDialogListener mListener;
	private EditText mNewCharEdit;

	public interface PredictionDialogListener {
		public void OnPredictionOkClick(char predictedCharacter);

		public void OnPredictionCorrectionClick(char newCharacter);

		public void OnPredictionCancelClick();
	}

	/**
	 * Create a new instance of Prediction, providing the predicted
 character as an argument.
	 */
	public static Prediction newInstance(char prediction) {
		Prediction dialog = new Prediction();

		// Supply the prediction argument
		Bundle args = new Bundle();
		args.putChar(KEY_CHARACTER, prediction);
		dialog.setArguments(args);

		return dialog;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Obtain dialog arguments
		Bundle args = getArguments();
		if (args == null) {
			return;
		}
		mPredictedCharacter = args.getChar(KEY_CHARACTER);

		// Verify that the host activity implements the callback interface
		try {
			// Instantiate the PredictionDialogListener so we can send events to
			// the host
			mListener = (PredictionDialogListener) activity;
		} catch (ClassCastException e) {
			// If the activity doesn't implement the interface, throw exception
			throw new ClassCastException(activity.toString()
					+ " must implement PredictionDialogListener");
		}

	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		if (mPredictedCharacter == null) {
			return null;
		}

		View view = RelativeLayout.inflate(getActivity(), R.layout.prediction,
				null);
		TextView charView = (TextView) view
				.findViewById(R.id.predict_character);
		charView.setText(mPredictedCharacter.toString());

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		AlertDialog dialog = builder
				.setView(view)
				.setPositiveButton(android.R.string.ok, mOkListener)
				.setNeutralButton(R.string.predict_correction,
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								// Do nothing here, we override it below
							}
						})
				.setNegativeButton(android.R.string.cancel, mCancelListener)
				.create();

		// Do not allow outside clicks
		dialog.setCanceledOnTouchOutside(false);

		// Override the correction button listener for custom behavior
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {

			@Override
			public void onShow(DialogInterface arg0) {
				Button button = ((AlertDialog) getDialog())
						.getButton(AlertDialog.BUTTON_NEUTRAL);
				button.setOnClickListener(mCorrectionListener);
			}
		});

		dialog.show();
		return dialog;
	}

	DialogInterface.OnClickListener mOkListener = new OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (mPredictedCharacter != null) {
				mListener.OnPredictionOkClick(mPredictedCharacter);
			}
		}
	};

	View.OnClickListener mCorrectionListener = new View.OnClickListener() {

		@Override
		public void onClick(View view) {
			mNewCharEdit = (EditText) getDialog().findViewById(
					R.id.predict_correction);
			TextView predictionView = (TextView) getDialog().findViewById(
					R.id.predict_character);
			predictionView.setVisibility(TextView.GONE);

			mNewCharEdit.setVisibility(EditText.VISIBLE);
			Button correctionButton = ((AlertDialog) getDialog())
					.getButton(AlertDialog.BUTTON_NEUTRAL);
			((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE)
					.setVisibility(Button.GONE);

			correctionButton.setText(android.R.string.ok);
			correctionButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {
					String newCharText = mNewCharEdit.getText().toString();
					if (newCharText.length() == 1) {
						char newCharacter = newCharText.charAt(0);
						mListener.OnPredictionCorrectionClick(newCharacter);
						dismiss();
					} else {
						Toast.makeText(getActivity(),
								R.string.invalid_char,
								Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
	};

	DialogInterface.OnClickListener mCancelListener = new OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			mListener.OnPredictionCancelClick();
			dismiss();
		}
	};
}
