package infologo.infologo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

// Pop-up message shown when there is an error, it's used in some cases to give more information
public class ErrorAlert {
    AlertDialog.Builder alertDialogBuilder;
    AlertDialog alertDialog;

    public ErrorAlert(Context context, String errorMessage, String errorTitle) {
        alertDialogBuilder = new AlertDialog.Builder(context);

        // Set title
        alertDialogBuilder.setTitle(errorTitle);

        // Set dialog message
        alertDialogBuilder
                .setMessage(errorMessage)
                .setCancelable(false)
                .setNeutralButton("Ok",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // If this button is clicked, just close the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // Create alert dialog
        alertDialog = alertDialogBuilder.create();
        // Show it
        alertDialog.show();
    }
}
