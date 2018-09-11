package infologo.infologo;

import android.os.Environment;

import com.github.kittinunf.fuel.util.Base64;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import cz.msebera.android.httpclient.Header;

public class TextToSpeechRequest {
    private static final int substringSize = 2000;

    public static void sendText(String text, final Main_window window) {
        final String language = "en-gb";
        final String languageName = "en-GB-Standard-A";
        final String gender = "FEMALE";
        final String audioEncoding = "MP3";

        String textToSend = cutStringText(text);

        try {
            // Prepare JSON for Text-to-speech GET call
            JSONObject input = new JSONObject();
            input.put("text", textToSend);
            JSONObject voice = new JSONObject();
            voice.put("languageCode", language);
            voice.put("name", languageName);
            voice.put("ssmlGender", gender);
            JSONObject audioConfig = new JSONObject();
            audioConfig.put("audioEncoding", audioEncoding);
            JSONObject postData = new JSONObject();
            postData.put("input", input);
            postData.put("voice", voice);
            postData.put("audioConfig", audioConfig);

            String body = postData.toString();

            // Text-to-speech GET call
            RESTClass.getAudio(window, body, new JsonHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    // Signal connection problem with an alert dialog and the status text
                    window.setActualText("Connection problem occurred.");
                    String errorMessage = "A connection problem has occurred, please check connection and try again.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            ";
                    new ErrorAlert(window, errorMessage, "Connection problem");
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        // Get speech
                        String speech = response.get("audioContent").toString();

                        // Set status text
                        window.setActualText("Speech received.");

                        // Decode Base64 String into a binary String which contains the mp3 file
                        byte[] bytesSpeech = Base64.decode(speech, Base64.DEFAULT);

                        // Save audio in a local directory
                        File storageDir = window.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                        File audioFile = new File(storageDir, "logoAudio.mp3");
                        TextActivity.logoAudioDir = audioFile.getAbsolutePath();
                        if(audioFile.exists()) {
                            audioFile.delete();
                        }
                        audioFile.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(audioFile);
                        DataOutputStream outWriter = new DataOutputStream(fOut);
                        outWriter.write(bytesSpeech);
                        outWriter.close();
                        fOut.flush();
                        fOut.close();

                        // Display results in TextActivity
                        window.activeTextActivity();
                    } catch(Exception e) {
                        window.setActualText("Speech not received.");
                    }
                }
            });
        } catch(Exception e) {
            window.setActualText("Problem with JSON construction.");
        }
    }

    private static String cutStringText(String text) {
        // Get substring from beginning to the next full stop from substringSize
        int index = text.indexOf(".", substringSize)+1;
        return text.substring(0, index);
    }
}
