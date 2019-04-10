package ceit.aut.digitrec.gui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.RelativeLayout;

public class About extends DialogFragment {
	public static final String TAG = "about_dialog";

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View view = RelativeLayout.inflate(getActivity(), R.layout.about, null);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		AlertDialog dialog = builder.setView(view)
				.setNeutralButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dismiss();
					}
				}).show();
		return dialog;
	}
}
