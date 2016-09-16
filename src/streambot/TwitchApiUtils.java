package streambot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TwitchApiUtils {

    private static final String TWITCH_BASE_URL = "https://api.twitch.tv/kraken/streams?channel=";

    public static String getStreamStatus(String stream) {
        //check individual channel's online status

        String streamStatus = "";
        JSONObject json = getJsonObject(TWITCH_BASE_URL + stream);
        if (json != null) {
            JSONArray streams = json.getJSONArray("streams");
            if (streams.length() > 0) {
                JSONObject theStream = streams.getJSONObject(0);
                JSONObject channelInfo = theStream.getJSONObject("channel");
                streamStatus = stream + " is online! Title: " + channelInfo.getString("status")
                        + " Game: " + theStream.getString("game")
                        + " Viewers: " + String.valueOf(theStream.getInt("viewers"));
            } else {
                streamStatus = stream + " is not online.";
            }
        } else {
            streamStatus = "An error occured checking stream.";
        }

        return streamStatus;
    }

    public static String getLiveStreams(ArrayList<String> streams) {
        //produce list of streams that are live

        StringBuilder messageBuilder = new StringBuilder();
        ArrayList<String> chunkedStreams = chunkifyStreams(streams);
        for (String streamsUrl : chunkedStreams) {
            JSONObject json = getJsonObject(TWITCH_BASE_URL + streamsUrl);
            JSONArray jsonStreamsArray = json.getJSONArray("streams");
            int total = json.getInt("_total");
            for (int i=0;i<total;i++) {
                JSONObject jsonStream = jsonStreamsArray.getJSONObject(i);
                JSONObject channel = jsonStream.getJSONObject("channel");
                messageBuilder.append(channel.get("display_name") + ", ");
            }
        }
        return messageBuilder.toString();
    }

    public static JSONObject getJsonObject(String url) {
        //build and return JSONObject from url
        
        JSONObject json = null;
        try {
            //grab client id
            BotConfig config = BotConfig.getInstance();
            String clientID = config.getClientID();
            
            System.out.println("checking url - " + url);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Client-ID", clientID);
            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            StringBuilder builder = new StringBuilder();
            int cp;
            while ((cp = reader.read()) != -1) {
                builder.append((char) cp);
            }
            String jsonText = builder.toString();
            json = new JSONObject(jsonText);
            inputStream.close();
            return json;
        } catch (IOException e) {
            System.out.println("IOError " + e);
        } catch (JSONException e) {
            System.out.println("JSON error " + e);
        }
        return json;
    }

    public static ArrayList<String> chunkifyStreams(ArrayList<String> allStreams) {
        //take list of all channels and make them url-ready strings (comma-separated)
        //if > 100 names, split into chunks of size 100 (twitch api limitations)
        //returns arraylist of {"name0,name1,....,name99", "name100,name101,...,name199",...}

        ArrayList<String> chunkedStreams = new ArrayList<String>();
        int i = 1;
        StringBuilder chunkBuilder = new StringBuilder();
        for (String stream : allStreams) {

            //start new chunk at 100
            if (i % 100 == 0) {
                chunkedStreams.add(chunkBuilder.toString());
                chunkBuilder = new StringBuilder();
            }
            chunkBuilder.append(stream + ",");
            i++;
        }
        chunkedStreams.add(chunkBuilder.toString());
        return chunkedStreams;
    }

}
