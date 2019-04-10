package ceit.aut.digitrec.gui.dialogs;

import java.io.IOException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import ceit.aut.digitrec.gui.activities.TouchMode;
import ceit.aut.digitrec.utils.ImageUtils;

public class SelectDigit extends DialogFragment {
	public static final String TAG = "newchar_dialog";
	private EditText mNewCharEdit;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View view = RelativeLayout.inflate(getActivity(),
				R.layout.new_character, null);
		mNewCharEdit = (EditText) view.findViewById(R.id.newchar_character);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		AlertDialog dialog = builder.setView(view)
				.setPositiveButton(android.R.string.ok, mOkListener)
				.setNegativeButton(android.R.string.cancel, mCancelListener)
				.show();
		return dialog;
	}

	private OnClickListener mOkListener = new OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dismiss();

			// Acquire the entered character
			String newCharText = mNewCharEdit.getText().toString();
			if (newCharText.length() == 1) {
				char newCharacter = newCharText.charAt(0);
				// Add the character to known character labels
				if (ImageUtils.addCharacterLabel(newCharacter)) {
					// If the character is new, try to save the new labels list
					try {
						ImageUtils.saveCharacterLabels(getActivity()
								.getExternalFilesDir(null));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				// Start the handwriting activity to train this character
				Intent intent = new Intent();
				intent.setAction(TouchMode.TRAIN_CHAR)
						.setClass(getActivity(), TouchMode.class);
				intent.putExtra(TouchMode.KEY, newCharacter);
				startActivity(intent);
				dismiss();
			} else {
				Toast.makeText(getActivity(), R.string.invalid_char,
						Toast.LENGTH_SHORT).show();
			}
		}
	};

	private OnClickListener mCancelListener = new OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dismiss();
		}
	};
}
