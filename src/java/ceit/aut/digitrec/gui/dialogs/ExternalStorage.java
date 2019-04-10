package ceit.aut.digitrec.gui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import ceit.aut.digitrec.gui.activities.MainActivity;

public class ExternalStorage extends DialogFragment {
	public static final String TAG = "ext_storage_dialog";

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		AlertDialog dialog = builder
				.setTitle(R.string.ext_storage_dialog_title)
				.setMessage(R.string.ext_storage_dialog_body)
				.setNeutralButton(R.string.ext_storage_exit,
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								passExitToMainActivity();
								dismiss();
							}
						}).setCancelable(false).create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
		return dialog;
	}

	/**
	 * Sends an intent to the {@link MainActivity} that finishes it, exiting the
	 * application in effect, as the {@link MainActivity} is singleTop.
	 */
	private void passExitToMainActivity() {
		Intent intent = new Intent(getActivity(), MainActivity.class);
		intent.setAction(MainActivity.EXIT_APPLICATION);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
}
