package streambot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChannelPreferences {

    private Path configPath;

    public ChannelPreferences() {
        configPath = Paths.get("data" + File.separator + "channels.json");
    }

    public JSONArray loadPreferences() {
        try {
            String jsonText = new String(Files.readAllBytes(configPath));
            JSONObject jsonPrefs = new JSONObject(jsonText);
            return jsonPrefs.getJSONArray("channels");
        } catch (IOException e) {
            System.out.println("Error loading channel preferences " + e.getMessage());
        }
        return null;
    }

    public void savePreferences(ArrayList<StreamChannel> channels) {

        System.out.println("Saving preferences...");

        JSONObject jsonConfig = new JSONObject();
        JSONArray jsonChannelsArray = new JSONArray();
        for (StreamChannel channel : channels) {
            JSONObject jsonChannel = new JSONObject();
            jsonChannel.put("channel", channel.getChannel());
            jsonChannel.put("flavor", channel.getFlavor());
            jsonChannel.put("randomize_flavor", channel.isFlavorRandomized());
            jsonChannel.put("announce_offline", channel.isAnnouncingOffline());

            jsonChannelsArray.put(jsonChannel);
        }

        jsonConfig.put("channels", jsonChannelsArray);

        try {
            FileWriter writer = new FileWriter(configPath.toString());
            jsonConfig.write(writer);
            writer.close();
        } catch (IOException e) {
            System.out.println("Error writing channel config " + e.getMessage());
        }
    }
}
