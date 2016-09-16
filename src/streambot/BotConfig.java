package streambot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class BotConfig {

    private static BotConfig instance;
    
    private String server = "";
    private String nick = "";
    private String ident = "";
    private int port = -1;
    private String clientID = "";

    private ArrayList<String> ops = new ArrayList<>();
    private ArrayList<String> mods = new ArrayList<>();
    private ArrayList<String> channels = new ArrayList<>();

    public static BotConfig getInstance() throws IOException {
        if (instance == null) {
            instance = new BotConfig();
        }
        return instance;
    }
    
    private BotConfig() throws IOException {

        String jsonText = new String(Files.readAllBytes(Paths.get("data" + File.separator + "config.json")));
        JSONObject json = new JSONObject(jsonText);

        server = json.getString("server");
        nick = json.getString("nick");
        ident = json.getString("ident");
        port = json.getInt("port");
        clientID = json.getString("client_id");

        JSONArray jsonChannelsArray = json.getJSONArray("channels");
        for(int i=0; i<jsonChannelsArray.length(); i++) {
            channels.add(jsonChannelsArray.getString(i));
        }

        JSONObject jsonUsers = json.getJSONObject("users");
        JSONArray jsonOpsArray = jsonUsers.getJSONArray("ops");
        for (int i=0; i<jsonOpsArray.length(); i++) {
            ops.add(jsonOpsArray.getString(i));
        }

        JSONArray jsonModsArray = jsonUsers.getJSONArray("mods");
        for (int i=0; i<jsonModsArray.length(); i++) {
            mods.add(jsonModsArray.getString(i));
        }
    }

    public String getNick() {
        return nick;
    }

    public String getServer() {
        return server;
    }

    public String getIdent() {
        return ident;
    }

    public int getPort() {
        return port;
    }
    
    public String getClientID() {
        return clientID;
    }

    public ArrayList<String> getChannels() {
        return channels;
    }

    public boolean isOp(String user) {
        return ops.contains(user);
    }

    public boolean isMod(String user) {
        return ops.contains(user);
    }
}
