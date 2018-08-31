package infologo.infologo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Handler;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;
import com.github.kittinunf.fuel.util.Base64;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import cz.msebera.android.httpclient.Header;
import kotlin.Pair;

public class Main_window extends AppCompatActivity implements Serializable {
    public final static int MY_REQUEST_CODE = 1;
    private static String currentPhotoPath = "";
    private static int targetH = 0;
    private static int targetW = 0;
    private static int maxPhotoSide = 1024;
    public static String textString = "";
    private static String statusString = "";
    private static boolean textActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_window);
        if(!currentPhotoPath.equals("")) {
            setImage();
        }
        ((TextView)findViewById(R.id.statusText)).setText(statusString);
        setToolbar();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MY_REQUEST_CODE && resultCode == RESULT_OK) {
            setImage();
            setActualText(getString(R.string.status_text_sending_logo));

            // Load scaled image to send to Google Vision API with a fixed max size, that is different from previewImage photo
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
            int photoW=0, photoH=0;
            try {
                ExifInterface exif = new ExifInterface(currentPhotoPath); // Exif is a value that represents the rotation of the photo compared to the rotation of camera
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                switch (orientation) { //Images can have a different exif, so if they have a rotation of 90° or 270° then height and width are swapped
                    case 5:
                        photoW = bmOptions.outHeight;
                        photoH = bmOptions.outWidth;
                        break; // Mirror image
                    case 6:
                        photoW = bmOptions.outHeight;
                        photoH = bmOptions.outWidth;
                        break;
                    case 7:
                        photoW = bmOptions.outHeight;
                        photoH = bmOptions.outWidth;
                        break; // Mirror image
                    case 8:
                        photoW = bmOptions.outHeight;
                        photoH = bmOptions.outWidth;
                        break;
                    default:
                        photoW = bmOptions.outWidth;
                        photoH = bmOptions.outHeight;
                        break;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW/maxPhotoSide, photoH/maxPhotoSide);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            Bitmap picture = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

            // Send to Google Vision API
            try {
                ExifInterface exif = new ExifInterface(currentPhotoPath); // Exif is a value that represents the rotation of the photo compared to the rotation of camera
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                Matrix matrix = new Matrix(); // With this matrix it's possible to rotate the image so that the photo will be represented not rotated
                switch(orientation) {
                    case 2: matrix.postRotate(0); break; // Mirror image
                    case 3: matrix.postRotate(180); break;
                    case 4: matrix.postRotate(180); break; // Mirror image
                    case 5: matrix.postRotate(90); break; // Mirror image
                    case 6: matrix.postRotate(90); break;
                    case 7: matrix.postRotate(270); break; // Mirror image
                    case 8: matrix.postRotate(270); break;
                    default: matrix.postRotate(0); break;
                }
                picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);

                // Prepare the photo to send
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                picture.compress(Bitmap.CompressFormat.JPEG, 100, byteStream);
                String base64Data = Base64.encodeToString(byteStream.toByteArray(),
                        Base64.URL_SAFE);

                String requestURL =
                        "https://vision.googleapis.com/v1/images:annotate?key=" +
                                getResources().getString(R.string.mykey);
                // Create an array containing the LOGO_DETECTION feature
                JSONArray features = new JSONArray();
                JSONObject feature = new JSONObject();
                feature.put("type", "LOGO_DETECTION");
                features.put(feature);

                // Create an object containing the Base64-encoded image data
                JSONObject imageContent = new JSONObject();
                imageContent.put("content", base64Data);

                // Put the array and object into a single request and then put the request into an array of requests
                JSONArray requests = new JSONArray();
                JSONObject request = new JSONObject();
                request.put("image", imageContent);
                request.put("features", features);
                requests.put(request);
                JSONObject postData = new JSONObject();
                postData.put("requests", requests);

                // Convert the JSON into a string
                String body = postData.toString();

                Fuel.post(requestURL)
                        .header(
                                new Pair<String, Object>("content-length", body.length()),
                                new Pair<String, Object>("content-type", "application/json")
                        )
                        .body(body.getBytes())
                        .responseString(new Handler<String>() {
                            @Override
                            public void success(@NotNull Request request,
                                                @NotNull Response response,
                                                String data) {
                                // Access the logoAnnotations array
                                try {
                                    JSONArray labels = new JSONObject(data)
                                            .getJSONArray("responses")
                                            .getJSONObject(0)
                                            .getJSONArray("logoAnnotations");

                                    // Extract the description key of logo from the array
                                    String searchString = labels.getJSONObject(0).getString("description");

                                    // Set status text
                                    setActualText("Logo successfully recognized.");
                                    TextActivity.logoName = searchString;

                                    // Search Wikipedia Page
                                    searchPage(searchString);
                                } catch (JSONException e) {
                                    // Signal failure with an alert dialog and the status text
                                    setActualText("Logo not recognized.");
                                    String errorMessage = "Logo not recognized, please try again with a better photo";
                                    new ErrorAlert(Main_window.this, errorMessage, "Logo not recognized");
                                }
                            }

                            @Override
                            public void failure(@NotNull Request request,
                                                @NotNull Response response,
                                                @NotNull FuelError fuelError) {
                                // Signal connection problem with an alert dialog and the status text
                                setActualText("Connection problem occurred.");
                                String errorMessage = "A connection problem has occurred, please check connection and try again";
                                new ErrorAlert(Main_window.this, errorMessage, "Connection problem");
                            }
                        });
            } catch(Exception e) {
                e.printStackTrace();
            }
        } else {
            currentPhotoPath = "";
        }
    }

    public void takePicture(View view) {
        // Check if there is a recognition working
        if(!statusString.equals(getString(R.string.status_text_sending_logo))) {
            // Get the dimensions of previewImage here, because else the rotation of the phone will cause an error due to targetW==0 and targetH==0
            ImageView previewImage = (findViewById(R.id.previewImage));
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            if (size.x != 0 && previewImage.getHeight() != 0) {
                targetW = size.x;
                targetH = previewImage.getHeight();
            }

            // Delete old operation, cleaning all values and conditions
            setActualText("");
            textString = "";
            textActive = false;
            setToolbar();
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Ensure that there's a camera activity to handle the intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "infologo.infologo.fileprovider",
                            photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(intent, MY_REQUEST_CODE);
                }
            }
        }
    }

    private File createImageFile() throws Exception {
        // Create an empty file if it doesn't exists not to have the picture seen in the gallery
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File emptyFile = new File(storageDir+"/.NOMEDIA");
        if(!emptyFile.exists()) {
            emptyFile.createNewFile();
        }

        // Delete the old image if exists
        if(!currentPhotoPath.equals("")) {
            File image = new File(currentPhotoPath);
            if(image.exists()) {
                image.delete();
            }
        }

        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = ".JPEG_" + timeStamp + "_";
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void setImage() {
        // Convert image data to bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        int photoW=0, photoH=0;
        try {
            ExifInterface exif = new ExifInterface(currentPhotoPath); // Exif is a value that represents the rotation of the photo compared to the rotation of camera
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) { //Images can have a different exif, so if they have a rotation of 90° or 270° then height and width are swapped
                case 5:
                    photoW = bmOptions.outHeight;
                    photoH = bmOptions.outWidth;
                    break; // Mirror image
                case 6:
                    photoW = bmOptions.outHeight;
                    photoH = bmOptions.outWidth;
                    break;
                case 7:
                    photoW = bmOptions.outHeight;
                    photoH = bmOptions.outWidth;
                    break; // Mirror image
                case 8:
                    photoW = bmOptions.outHeight;
                    photoH = bmOptions.outWidth;
                    break;
                default:
                    photoW = bmOptions.outWidth;
                    photoH = bmOptions.outHeight;
                    break;
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap picture = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        // Set the bitmap as the source of the ImageView
        try {
            ExifInterface exif = new ExifInterface(currentPhotoPath); // Exif is a value that represents the rotation of the photo compared to the rotation of camera
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Matrix matrix = new Matrix(); // With this matrix it's possible to rotate the image so that the photo will be represented not rotated
            switch(orientation) {
                case 2: matrix.postRotate(0); break; // Mirror image
                case 3: matrix.postRotate(180); break;
                case 4: matrix.postRotate(180); break; // Mirror image
                case 5: matrix.postRotate(90); break; // Mirror image
                case 6: matrix.postRotate(90); break;
                case 7: matrix.postRotate(270); break; // Mirror image
                case 8: matrix.postRotate(270); break;
                default: matrix.postRotate(0); break;
            }
            picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);
            ImageView previewImage = (findViewById(R.id.previewImage));
            previewImage.setImageBitmap(picture);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    AppCompatActivity getActivity() {
        return this;
    }

    private void setToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    void activeTextActivity() {
        // Change toolbar of main window
        textActive = true;
        setToolbar();

        // Display the annotations inside textActivity TextView and show textActivity
        Intent intent = new Intent(getActivity(), TextActivity.class);
        startActivity(intent);
    }

    private void searchPage(String logo) {
        setActualText("Searching Wikipedia page…");
        RESTClass.get(logo, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject
                try {
                    String urlLogo = response.getJSONArray("items")
                            .getJSONObject(0)
                            .getString("link");

                    // Download and save html page
                    String path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                    String fileName = "wikiHtml";
                    DownloaderClass.downloadFile(urlLogo, path, fileName, getThis());
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Main_window getThis() {
        return this;
    }

    void setActualText(String newStatusString) {
        statusString = newStatusString;
        ((TextView) findViewById(R.id.statusText)).setText(statusString);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        if(textActive)
            getMenuInflater().inflate(R.menu.menu_main, menu);
        else
            getMenuInflater().inflate(R.menu.menu_main_notext, menu);
        return true;
    }

    public void onForwardAction(MenuItem mi) {
        Intent intent = new Intent(this, TextActivity.class);
        startActivity(intent);
    }
}
