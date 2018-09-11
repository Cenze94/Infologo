package infologo.infologo;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import cz.msebera.android.httpclient.entity.StringEntity;

// Utility class that contains all REST calls, except for logo detection one
public class RESTClass {
    private static final String BASE_URL = "https://www.googleapis.com/customsearch/v1";
    private static final String mykey = "AIzaSyBKEMiI34cGvC-pt6LwkdgeLvBYufB8HIg";
    private static final String cx = "011467683329735307367:sxasfjhmbw4";
    private static final String audioUrl = "https://texttospeech.googleapis.com/v1beta1/text:synthesize?key=";
    private static AsyncHttpClient client = new AsyncHttpClient();

    // Get call for Custom Search JSON API, which returns Wikipedia HTML page URL of logo
    public static void get(String logo, AsyncHttpResponseHandler responseHandler) {
        String url = BASE_URL + "?key=" + mykey + "&cx=" + cx + "&q=" + logo + " brand";
        client.get(url, null, responseHandler);
    }

    // Get call that returns Wikipedia HTML page file of logo
    public static void getFile(String url, FileAsyncHttpResponseHandler responseHandler) {
        client.get(url, null, responseHandler);
    }

    // Get call for Text-to-speech service, which returns the coded speech
    public static void getAudio(Context context, String json, JsonHttpResponseHandler responseHandler) {
        try {
            StringEntity entity = new StringEntity(json);
            client.post(context, audioUrl+mykey, entity, "application/json", responseHandler);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
