package ceit.aut.digitrec.gui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import ceit.aut.digitrec.gui.dialogs.SelectDigit;
import ceit.aut.digitrec.gui.dialogs.ExternalStorage;
import ceit.aut.digitrec.utils.FileUtils;

public class CharacterList extends FragmentActivity {
	private ExternalStorage storageDialog;
	private BroadcastReceiver storageReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.characters);

		storageReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				if (Environment.MEDIA_MOUNTED.equals(Environment
						.getExternalStorageState())) {
					if (storageDialog != null
							&& storageDialog.isVisible()) {
						storageDialog.dismiss();
					}
				} else {
					storageDialog = new ExternalStorage();
					storageDialog.show(getSupportFragmentManager(),
							ExternalStorage.TAG);
				}
			}
		};

		registerReceiver(storageReceiver,
				FileUtils.getExternalStorageChangeFilter());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(storageReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.characters, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivityForResult(new Intent(this, Settings.class),
					Settings.CODE);
			return true;
		case R.id.menu_add:
			SelectDigit dialog = new SelectDigit();
			dialog.show(getSupportFragmentManager(), SelectDigit.TAG);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
