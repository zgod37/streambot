package streambot;

import java.util.ArrayList;

public class Announcer {

    private static Announcer announcer;

    private final static String[] FLAVORS = {
        "11icy 10mint",
        "5spicy 4marinara",
        "7peanut 8butter",
        "3garden 9salad",
        "13candy 6hearts",
        "11blue 13raspberry",
        "4christmas 3cookies",
        "9lemon 8lime",
        "6blackberry 15cream",
        "14eggplant 15parmesan",
        "no colors"};

    private String labelColor = "";
    private String infoColor = "";
    private String separator = "14--";

    private boolean randomize = false;
    private boolean announceOffline = true;

    private Announcer() {}

    public static Announcer getAnnouncer() {
        if (announcer == null) {
            announcer = new Announcer();
        }
        return announcer;
    }

    public void announce(StreamChannel channel, ArrayList<StreamInfo> streams) {
        //create and format the announcements based on given StreamChannel and list of changed streams
        
        //System.out.println("Creating announcements for: " + channel.getChannel());
        //System.out.format("Prefs: flavor: %d, rand: %s, offline: %s%n", new Object[]{channel.getFlavor(), channel.isRandomizeFlavor(), channel.isAnnounceOffline()});
        
        setPreferences(channel);
        ArrayList<String> announcements = createAnnouncements(streams);
        announceToChannel(announcements, channel.getChannel());
    }
    
    private void setPreferences(StreamChannel channel) {
        randomize = channel.isFlavorRandomized();
        announceOffline = channel.isAnnouncingOffline();
        setFlavor(channel.getFlavor());
    }
    
    private ArrayList<String> createAnnouncements(ArrayList<StreamInfo> streams) {
        ArrayList<String> announcements = new ArrayList<>();
        for (StreamInfo stream : streams) {
            announcements.add(formatAnnouncement(stream));
        }
        return announcements;
    }

    private void announceToChannel(ArrayList<String> announcements, String chan) {
        for (String announcement : announcements) {
            Bot.sleep(750);
            Bot.say(announcement, chan);
        }
    }

    private String formatAnnouncement(StreamInfo stream) {
        //simple factory method to format announcement based on
        //whether stream is on/offline or has updated status

        if (randomize) {
            randomizeFlavor();
        }

        if (announceOffline && stream.getStatus() == null) {
            return formatOffline(stream);
        } else if (stream.getStatusChanged()) {
            return formatOnline("Status update:", stream);
        } else {
            return formatOnline("Online:", stream);
        }
    }

    private String formatOffline(StreamInfo stream) {
        return separator + " " + labelColor + stream.getName() + infoColor + " went offline. " + separator;
    }

    private String formatOnline(String header, StreamInfo streamInfo) {
        return separator +
                colorizeField(header, streamInfo.getName()) + 
                separator +
                colorizeField("Title:", streamInfo.getStatus()) +
                separator + 
                colorizeField("Game:", streamInfo.getGame()) +
                separator +
                labelColor + " " + streamInfo.getUrl() + " " +
                separator;
    }

    private String colorizeField(String label, String info) {
        return labelColor + " " + label + infoColor + " " + info + " ";
    }

    

    private void setFlavor(int flavorIndex) {
        String flavor = FLAVORS[flavorIndex];
        String[] colors = flavor.replaceAll("[a-z]", "").split(" ");
        labelColor = colors[0];
        infoColor = colors[1];
    }

    private void randomizeFlavor() {
        setFlavor((int) (Math.random()*FLAVORS.length));
    }

    public static String[] getFlavors() {
        return FLAVORS;
    }

    public static String[] createPartyMessage(String message, int offset) {
        //turn message into color-shifting string array

        String[] messages = new String[4];
        for (int i=0; i<messages.length; i++) {
            StringBuilder builder = new StringBuilder();
            int colorOffset = 0;
            for (char c : message.toCharArray()) {
                builder.append(""+((i+colorOffset+offset)%16));
                builder.append(c);
                colorOffset++;
            }
            messages[i] = builder.toString();
        }

        return messages;
    }
}
