package streambot;

import java.util.ArrayList;

/**
 * 
 * this represents the irc channel that the bot resides in
 * keeps track of the list of streams that get announced in that channel
 * as well as the following user preferences:
 * add/remove streams to the list,
 * customize color scheme (flavor),
 * whether or not streams get announced if they go offline.
 */

public class StreamChannel {

    private String channel = "";

    private ArrayList<String> streams = new ArrayList<>();

    private int flavor = -1;
    private boolean isFlavorRandomized = false;
    private boolean isAnnouncingOffline = true;

    public StreamChannel(String channel, ArrayList<String> streams,
            int flavor, boolean isFlavorRandomized, boolean isAnnouncingOffline) {

        this.channel = channel;
        this.streams = streams;
        this.flavor = flavor;
        this.isFlavorRandomized = isFlavorRandomized;
        this.isAnnouncingOffline = isAnnouncingOffline;
    }

    public void createAnnouncements(ArrayList<StreamInfo> changedStreams) {
        ArrayList<StreamInfo> streamsToAnnounce = new ArrayList<>();
        for (StreamInfo stream : changedStreams) {
            if (streams.contains(stream.getName())) {
                streamsToAnnounce.add(stream);
            }
        }
        if (streamsToAnnounce.size() > 0) {
            Announcer.getAnnouncer().announce(this, streamsToAnnounce);
        }
    }

    public ArrayList<String> getList() {
        return streams;
    }

    public boolean addStream(String stream) {
        return streams.add(stream);
    }

    public boolean removeStream(String stream) {
        return streams.remove(stream);
    }

    public boolean isFlavorRandomized() {
        return isFlavorRandomized;
    }

    public void setRandomizeFlavor(boolean isFlavorRandomized) {
        this.isFlavorRandomized = isFlavorRandomized;
    }

    public boolean isAnnouncingOffline() {
        return isAnnouncingOffline;
    }

    public void setAnnouncingOffline(boolean isAnnouncingOffline) {
        this.isAnnouncingOffline = isAnnouncingOffline;
    }

    public String getChannel() {
        return channel;
    }

    public int getFlavor() {
        return flavor;
    }

    public void setFlavor(int flavor) {
        this.flavor = flavor;
    }
}
