package org.rockettrack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class Utils {

	public static void displayAbortDialog( final Activity context, int messageId ) {

		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(messageId)
		.setCancelable(false)
		.setNeutralButton("Ok",  new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				dialog.cancel();
				context.finish();
			}});
		final AlertDialog alert = builder.create();
		alert.show();

	}
}
