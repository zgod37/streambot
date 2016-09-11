package streambot;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * Class to manage tasks related to stream monitoring
 * - holds global list of streams and their stream statuses
 * - checks twitch's REST API and pushes status updates to channels
 * - adds/remove streams from channel lists
 * - saves any user-made changes in data
 */

public class StreamManager {

    StreamFileHandler fileHandler;
    ChannelPreferences prefs;

    //global map of all streams and their current online status
    //key = twitch channel name; value = StreamInfo (current online status)
    private HashMap<String, StreamInfo> streamers = new HashMap<>();
    
    //each channel that the bot announces streams in
    //key = irc channel name; value = StreamChannel (data relevant to channel)
    private HashMap<String, StreamChannel> channels = new HashMap<>();

    //each time user changes a pref or adds/removes a stream, the files
    //need to be rewritten, so flag the change and rewrite the files
    //periodically, rather than *every* time they're changed.
    boolean filesChanged = false;
    boolean prefsChanged = false;

    public StreamManager(ArrayList<String> ircChannels) {
        fileHandler = new StreamFileHandler();
        prefs = new ChannelPreferences();
        initializeStreamers();
        setUpChannels(ircChannels);
    }

    private void initializeStreamers() {
        ArrayList<String> globalList = fileHandler.getGlobalList();
        for (String stream : globalList) {
            streamers.put(stream, new StreamInfo(stream));
        }
    }

    private void setUpChannels(ArrayList<String> ircChannels) {

        JSONArray jsonChannelsArray = prefs.loadPreferences();
        if (jsonChannelsArray == null) {
            FileLogger.logWarning("Error loading stream channel prefs");
            return;
        }

        for (int i=0; i<jsonChannelsArray.length(); i++) {
            JSONObject channelJson = jsonChannelsArray.getJSONObject(i);
            String channel = channelJson.getString("channel");
            int flavor = channelJson.getInt("flavor");
            boolean randomize = channelJson.getBoolean("randomize_flavor");
            boolean announceOffline = channelJson.getBoolean("announce_offline");

            channels.put(channel, new StreamChannel(channel,fileHandler.getChannelList(channel),
                    flavor,randomize,announceOffline));
        }

    }

    public void checkForUpdates() {
        getUpdates();
        saveData();
    }

    private void getUpdates() {
        // *****************************************
        // **** MAIN STREAM CHECKING LOGIC HERE ****
        // *****************************************
        //poll twitch api to get the list of the streams that are currently live
        //compare list of live streams to cached, create list of streams that have changed
        //push list of stream changes to each stream channel to format & announce change

        ArrayList<StreamInfo> changedStreams = new ArrayList<>();
        
        ArrayList<String> chunkedStreams = TwitchApiUtils.chunkifyStreams(new ArrayList<String>(streamers.keySet()));
        for (String streams : chunkedStreams) {

            JSONObject json = TwitchApiUtils.getJsonObject("https://api.twitch.tv/kraken/streams?channel=" + streams);
            try {
                JSONArray streamsArray = json.getJSONArray("streams");
                int total = json.getInt("_total");
                ArrayList<String> onlineStreams = new ArrayList<>();

                for (int i=0; i<total; i++) {

                    //get stream info
                    JSONObject stream = streamsArray.getJSONObject(i);
                    JSONObject channel = stream.getJSONObject("channel");
                    String streamName = channel.getString("name");
                    String currentStatus = channel.getString("status");
                    
                    //***temporary bug fix***
                    //game can be null if not set by streamer
                    String currentGame = "";
                    if (!channel.isNull("game")) currentGame = channel.getString("game");
                    
                    onlineStreams.add(streamName);

                    //compare each current stream status to previously stored stream status
                    StreamInfo streamInfo = streamers.get(streamName);

                    //all streams here are online, so reset offline checks
                    if (streamInfo.getOfflineChecks() != 0) {
                        streamInfo.resetOfflineChecks();
                    }

                    //if stream has not been announced
                    if (streamInfo.getStatus() == null) {
                        streamInfo.setStatus(currentStatus);
                        streamInfo.setGame(currentGame);
                        changedStreams.add(streamInfo);
                    }

                    //if stream has been online and updated title or game
                    else if (streamInfo.getStatus() != null && 
                            (!streamInfo.getStatus().equals(currentStatus) || 
                                    !streamInfo.getGame().equals(currentGame))) {
                        streamInfo.setStatus(currentStatus);
                        streamInfo.setGame(currentGame);
                        streamInfo.setStatusChanged(true);
                        changedStreams.add(streamInfo);
                    }
                }

                //now check for streams that went offline
                //needs three consecutive offline checks to be announced
                for (String streamName : streamers.keySet()) {
                    
                    if (streamers.get(streamName).getStatus() != null && !onlineStreams.contains(streamName)) {
                        StreamInfo streamInfo = streamers.get(streamName);
                        if (streamInfo.getOfflineChecks() == 2) {
                            streamInfo.resetOfflineChecks();
                            streamInfo.setStatus(null);
                            streamInfo.setStatusChanged(false);
                            changedStreams.add(streamInfo);
                        } else {
                            streamInfo.incrementOfflineChecks();
                            System.out.println(streamInfo.getOfflineChecks() + " offline checks for " + streamName);
                        }

                    }
                }

            } catch (JSONException e) {
                System.out.println("JSONException error!" + e.getMessage());
            } catch (NullPointerException e) {
                System.out.println("Null json returned!" + e.getMessage());
            }
        }

        if (changedStreams.size() > 0) {
            notifyChannels(changedStreams);
        }
    }

    private void notifyChannels(ArrayList<StreamInfo> changedStreams) {
        for (String channel : channels.keySet()) {
            channels.get(channel).createAnnouncements(changedStreams);
        }
    }

    public void getLiveStreams(String channel) {
        //get that are currently live for that channel

        String liveStreams = TwitchApiUtils.getLiveStreams(channels.get(channel).getList());
        if (liveStreams.length() > 0) {
            Bot.say("Online: " + liveStreams, channel);
        } else {
            Bot.say("No one~", channel);
        }
    }

    public void checkStream(String stream, String channel) {
        //check stream status from api

        String status = TwitchApiUtils.getStreamStatus(stream);
        Bot.say(status, channel);
    }

    public void addStream(String stream, String channel) {
        //add twitch channel to all relevant locations
        //if stream is valid and new, it will re-add to global list
        //so the stream will be announced in the new channel

        if (channels.get(channel).getList().contains(stream)) {
            Bot.say(stream + " is already on the list!", channel);
            return;
        }
        JSONObject json = TwitchApiUtils.getJsonObject("https://api.twitch.tv/kraken/channels/" + stream);
        if (json == null) {
            Bot.say(stream + " is not a valid twitch name", channel);
            return;
        }
        channels.get(channel).addStream(stream);
        streamers.put(stream, new StreamInfo(stream));
        filesChanged = true;
        Bot.say(stream + " added successfully!", channel);
    }

    public void removeStream(String stream, String channel) {
        //remove stream from channel's list
        //if stream is no longer referenced in any channel,
        //it will be removed when files are updated

        if (channels.get(channel).removeStream(stream)) {
            filesChanged = true;
            Bot.say(stream + " removed from list", channel);
        } else {
            Bot.say(stream + " is not on the list!", channel);
        }

    }

    public void saveData() {
        if (filesChanged) {
            updateFiles();
        }
        if (prefsChanged) {
            savePreferences();
        }
    }

    private void updateFiles() {
        System.out.println("Rewriting files...");
        ArrayList<String> allStreams = new ArrayList<>();
        for (String channel : channels.keySet()) {
            ArrayList<String> channelList = channels.get(channel).getList();
            fileHandler.rewriteChannelFile(channelList, channel);
            for (String stream : channelList) {
                if (!allStreams.contains(stream)) {
                    allStreams.add(stream);
                }
            }
        }
        fileHandler.rewriteGlobalFile(allStreams);
        filesChanged = false;
    }

    private void savePreferences() {
        ArrayList<StreamChannel> updatedChannels = new ArrayList<>();
        for (String channel : channels.keySet()) {
            updatedChannels.add(channels.get(channel));
        }
        prefs.savePreferences(updatedChannels);
        prefsChanged = false;
    }

    public void displayFlavors(String channel) {
        String[] flavors = Announcer.getFlavors();
        Bot.say("- Flavors -", channel);
        for (int i=0; i<flavors.length; i++) {
            Bot.sleep(500);
            Bot.say(i + ": " + flavors[i], channel);
        }
        Bot.say("type '!changeflavor <number>' to select new flavor", channel);
    }

    public void changeFlavor(String flavor, String channel) {
        try {
            int flavorIndex = Integer.valueOf(flavor);

            if (flavorIndex >= 0 && flavorIndex <= Announcer.getFlavors().length) {
                channels.get(channel).setRandomizeFlavor(false);
                channels.get(channel).setFlavor(flavorIndex);
                Bot.say("got it!", channel);
                prefsChanged = true;
            } else {
                channels.get(channel).setRandomizeFlavor(true);
                Bot.say("flavor randomized", channel);
                prefsChanged = true;
            }
        } catch (NumberFormatException e) {
            Bot.say("sorry, i dont understand", channel);
            return;
        }
    }

    public void setOfflineAnnouncements(String offline, String channel) {
        if (offline.equalsIgnoreCase("on")) {
            channels.get(channel).setAnnouncingOffline(true);
            prefsChanged = true;
        } else if (offline.equalsIgnoreCase("off")) {
            channels.get(channel).setAnnouncingOffline(false);
            prefsChanged = true;
        } else {
            Bot.say("sorry, i dont understand", channel);
        }    
    }
}
