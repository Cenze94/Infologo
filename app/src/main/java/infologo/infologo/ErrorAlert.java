package infologo.infologo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class ErrorAlert {
    AlertDialog.Builder alertDialogBuilder;
    AlertDialog alertDialog;

    public ErrorAlert(Context context, String errorMessage, String errorTitle) {
        alertDialogBuilder = new AlertDialog.Builder(context);

        // set title
        alertDialogBuilder.setTitle(errorTitle);

        // set dialog message
        alertDialogBuilder
                .setMessage(errorMessage)
                .setCancelable(false)
                .setNeutralButton("Ok",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        alertDialog = alertDialogBuilder.create();
        // show it
        alertDialog.show();
    }
}
