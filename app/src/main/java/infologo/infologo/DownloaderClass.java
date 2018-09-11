package infologo.infologo;

import android.text.Html;
import android.text.Spanned;

import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import cz.msebera.android.httpclient.Header;

public class DownloaderClass {
    public static void downloadFile(String urlString, String path, String fileName, final Main_window window) {
        try {
            File file = new File(path + "/" + fileName);
            RESTClass.getFile(urlString, new FileAsyncHttpResponseHandler(file) {
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                    window.setActualText("Wikipedia page download failed.");
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, File file) {
                    try {
                        // Parse HTML file as String
                        FileInputStream fin = new FileInputStream(file);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        reader.close();
                        String fileString = sb.toString();
                        // Make sure you close all streams
                        fin.close();
                        // Set Main_window status string, although for some reason this change isn't shown
                        window.setActualText("Preparing text of Wikipedia page…");
                        // Extract data-content pairs
                        setLogoData(fileString);
                        // Clean HTML file
                        fileString = cleanString(fileString);
                        // Transform all remaining HTML tags into normal text
                        Spanned textHtml;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            textHtml = Html.fromHtml(fileString, Html.FROM_HTML_MODE_LEGACY);
                        } else {
                            textHtml = Html.fromHtml(fileString);
                        }
                        // Prepare text for Text-to-speech service
                        TextToSpeechRequest.sendText(textHtml.toString(), window);
                    } catch(Exception e) {
                        window.setActualText("Error during text preparation.");
                    }
                }
            });
        } catch(Exception e) {
            window.setActualText("Error during downloading of Wikipedia page.");
        }
    }

    private static String cleanString(String fileString) {
        int index;

        // Delete header until table with data
        index = fileString.indexOf("class=\"infobox vcard\"");
        index = fileString.indexOf("</table", index) +8;
        fileString = cleanSubstring(fileString, index, 0);

        // Delete coordinates if are present
        index = fileString.indexOf("Geographic coordinate system");
        if(index != -1) {
            String substring = fileString.substring(0, index);
            index = substring.lastIndexOf("<p");
            index = fileString.indexOf("</p", index) +4;
            fileString = cleanSubstring(fileString, index, 0);
        }

        // Delete all remaining stuff until text
        index = fileString.indexOf("<p");
        fileString = cleanSubstring(fileString, index, 0);

        // Delete all lines from See also if exists, else start from References
        index = fileString.indexOf("id=\"See_also\"");
        if(index != -1) {
            String substring = fileString.substring(0, index);
            index = substring.lastIndexOf("<h2");
            fileString = cleanSubstring(fileString, index);
        } else {
            // Delete all lines from References
            index = fileString.indexOf("id=\"References\"");
            String substring = fileString.substring(0, index);
            index = substring.lastIndexOf("<h2");
            fileString = cleanSubstring(fileString, index);
        }

        // Delete all images
        index = fileString.indexOf("<img");
        while(index != -1) {
            int nextIndex = fileString.indexOf("/>", index);
            fileString = cleanSubstring(fileString, nextIndex+2, index);
            index = fileString.indexOf("<img");
        }

        // Delete all captions of single images
        index = fileString.indexOf("class=\"thumbcaption\"");
        while(index != -1) {
            String substring = fileString.substring(0, index);
            index = substring.lastIndexOf("<div");
            int closureIndex = fileString.indexOf("/div", index);
            substring = fileString.substring(index+1, closureIndex);
            int count=0, nextIndex;
            nextIndex = substring.indexOf("<div");
            while(nextIndex != -1) {
                count++;
                nextIndex = substring.indexOf("<div", nextIndex+1);
            }
            while(count > 0) {
                closureIndex = fileString.indexOf("/div", closureIndex+1);
                count--;
            }
            fileString = cleanSubstring(fileString, closureIndex+5, index);
            index = fileString.indexOf("class=\"thumbcaption\"");
        }

        // Delete captions of images inside galleries
        index = fileString.indexOf("class=\"gallerytext\"");
        while(index != -1) {
            int nextIndex = fileString.indexOf("/div", index) +5;
            String substring = fileString.substring(0, index);
            index = substring.lastIndexOf("<div");
            fileString = cleanSubstring(fileString, nextIndex, index);
            index = fileString.indexOf("class=\"gallerytext\"");
        }

        // Delete link to main articles
        index = fileString.indexOf("hatnote navigation-not-searchable");
        while(index != -1) {
            int nextIndex = fileString.indexOf("/div", index) +5;
            String substring = fileString.substring(0, index);
            index = substring.lastIndexOf("<div");
            fileString = cleanSubstring(fileString, nextIndex, index);
            index = fileString.indexOf("hatnote navigation-not-searchable");
        }

        // Delete all sup text, including links to references
        index = fileString.indexOf("<sup");
        while(index != -1) {
            int nextIndex = fileString.indexOf("/sup", index) +5;
            fileString = cleanSubstring(fileString, nextIndex, index);
            index = fileString.indexOf("<sup");
        }

        // Delete warning messages
        index = fileString.indexOf("plainlinks metadata ambox");
        while(index != -1) {
            int nextIndex = fileString.indexOf("/table", index) +7;
            String substring = fileString.substring(0, index);
            index = substring.lastIndexOf("<table");
            substring = fileString.substring(index, nextIndex);
            int midIndex = substring.indexOf("<table", 1);
            while(midIndex != -1) {
                nextIndex = fileString.indexOf("/table", nextIndex) +7;
                substring = fileString.substring(index, nextIndex);
                midIndex = substring.indexOf("<table", midIndex+1);
            }
            fileString = cleanSubstring(fileString, nextIndex, index);
            index = fileString.indexOf("plainlinks metadata ambox");
        }

        // Delete content table
        index = fileString.indexOf("id=\"toc\"");
        String substring = fileString.substring(0, index);
        index = substring.lastIndexOf("<div");
        int closureIndex = fileString.indexOf("/div>", index) +4;
        int count = 0, nextIndex;
        substring = fileString.substring(index+1, closureIndex);
        nextIndex = substring.indexOf("<div");
        while(nextIndex != -1) {
            count++;
            nextIndex = substring.indexOf("<div", nextIndex+1);
        }
        while(count > -1) {
            fileString = cleanSubstring(fileString, closureIndex, index);
            closureIndex = fileString.indexOf("/div>", index) +5;
            count--;
        }

        // Delete all tables
        index = fileString.indexOf("<table");
        while(index != -1) {
            nextIndex = fileString.indexOf("/table>", index) +7;
            substring = fileString.substring(index, nextIndex);
            int midIndex = substring.indexOf("<table", 1);
            while(midIndex != -1) {
                nextIndex = fileString.indexOf("/table", nextIndex) +7;
                substring = fileString.substring(index, nextIndex);
                midIndex = substring.indexOf("<table", midIndex+1);
            }
            fileString = cleanSubstring(fileString, nextIndex, index);
            index = fileString.indexOf("<table");
        }

        // Delete h tags
        for(int i=1; i<7; i++) {
            index = fileString.indexOf("<h"+i);
            while(index != -1) {
                nextIndex = fileString.indexOf("</h", index) +5;
                fileString = cleanSubstring(fileString, nextIndex, index);
                index = fileString.indexOf("<h"+i);
            }
        }

        // Delete chart (found in Tesla page, I don't know if it's a standard element of Wikipedia)
        index = fileString.indexOf("chart noresize");
        while(index != -1) {
            nextIndex = fileString.indexOf("/div", index) +5;
            substring = fileString.substring(0, index);
            index = substring.lastIndexOf("<div");
            substring = fileString.substring(index, nextIndex);
            int midIndex = substring.indexOf("<div", 1);
            while(midIndex != -1) {
                nextIndex = fileString.indexOf("/div", nextIndex) +5;
                substring = fileString.substring(index, nextIndex);
                midIndex = substring.indexOf("<div", midIndex+1);
            }
            fileString = cleanSubstring(fileString, nextIndex, index);
            index = fileString.indexOf("chart noresize");
        }

        // Replace all \t char with \n and replace substrings of \n with a single \n
        fileString = fileString.replace("\t", "\n");
        StringBuilder sb = new StringBuilder(fileString);
        index = sb.indexOf("\n");
        while(index != -1 && index < sb.length()-1) {
            while(sb.charAt(index+1) == '\n' && index+1 < sb.length())
                sb.deleteCharAt(index+1);
            index = sb.indexOf("\n", index+1);
        }
        fileString = sb.toString();

        // Add .<br> to end of all li tags not empty
        index = fileString.indexOf("<li>");
        while(index != -1) {
            nextIndex = fileString.indexOf("</li>", index+4);
            if(nextIndex - index > 4) {
                fileString = fileString.substring(0, index) + fileString.substring(index+4, nextIndex) + ".<br>" + fileString.substring(nextIndex+5);
            }
            index = fileString.indexOf("<li>", index);
        }

        return fileString;
    }

    private static String cleanSubstring(String fileString, int index) {
        // Delete the substring from index to end file, that must be unique, else other parts will be deleted, with unknown consequences
        String substring = fileString.substring(index);
        return fileString.replace(substring, "");
    }

    private static String cleanSubstring(String fileString, int index, int startIndex) {
        // Delete the substring from startIndex to index, that must be unique, else other parts will be deleted, with unknown consequences
        String substring = fileString.substring(startIndex, index);
        return fileString.replace(substring, "");
    }

    private static void setLogoData(String fileString) {
        // Get the initial table on the right of the Wikipedia page
        int index = fileString.indexOf("infobox vcard");
        int nextIndex = fileString.indexOf("/table", index);
        String substring = fileString.substring(index, nextIndex);

        // Delete all sup text, including links to references
        index = substring.indexOf("<sup");
        while(index != -1) {
            nextIndex = substring.indexOf("/sup", index) +5;
            substring = cleanSubstring(substring, nextIndex, index);
            index = substring.indexOf("<sup");
        }

        // Get all pairs Type - Data inside the table
        index = substring.indexOf("<th");
        Spanned textHtml;
        String data;
        while(index != -1) {
            nextIndex = substring.indexOf("/th", index) +4;
            int checkIndex = substring.indexOf("<th", index+1);
            int contentIndex = substring.indexOf("<td", index);
            if(contentIndex != -1 && contentIndex < checkIndex) {
                // Extract data type
                data = substring.substring(index, nextIndex);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    textHtml = Html.fromHtml(data, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    textHtml = Html.fromHtml(data);
                }
                TextActivity.logoDataType.add(textHtml.toString());

                // Extract data content
                index = substring.indexOf("<td", nextIndex);
                nextIndex = substring.indexOf("/td", index) + 4;
                data = substring.substring(index, nextIndex);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    textHtml = Html.fromHtml(data, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    textHtml = Html.fromHtml(data);
                }
                TextActivity.logoDataContent.add(textHtml.toString());

                index = substring.indexOf("<th", nextIndex);
            } else
                index = -1;
        }
    }
}
