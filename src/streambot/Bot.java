package streambot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;

public class Bot implements Runnable {

    private static final String versionInfo = "mud shark bot 1.24 - added header to api request per new twitch rule";
    private static final String operChan = "#grigsby";
    
    StreamManager streamManager;
    BotConfig config;

    private Socket socket;
    private boolean isRunning;
    private BufferedReader in;
    private static BufferedWriter out;

    public Bot() {
        System.out.println("initializing bot");
    }

    public static void main(String[] args) {
        System.out.println("Starting Bot..");
        new Bot().start();
    }

    private void start() {
        //initialize logger, config and stream manager
        //create socket and connect to server
        //start thread to handle polling methods/twitch api checker
        //start message listener to process messages

        try {
            
            FileLogger.initializeLogger();
            FileLogger.logInfo("Bot started **************************************");
            
            config = BotConfig.getInstance();
            streamManager = new StreamManager(config.getChannels());

            System.out.println("Starting thread.");
            Thread keepAlive = new Thread(this);
            keepAlive.setDaemon(true);
            keepAlive.start();

            connectToServer();
            startMessageListener();

        } catch (IOException e) {
            System.out.println("Error starting bot! " + e.getMessage());
        }
    }

    private void startMessageListener() {
        //****************************
        //*****  MAIN LOOP HERE  *****
        //****************************

        //call processMessage for each line received
        isRunning = true;
        String line = "";
        while (isRunning) {
            try {
                while ((line = in.readLine()) != null ) {
                    processMessage(line);
                }
            } catch (SocketException e) {

                //********* RECONNECT ***********
                //if code reaches here, we were disconnected
                //keep bot running and try reconnecting
                System.out.println("Disconnected from server!");
                reconnect();
            } catch (Exception e) {

                //catch remaining exceptions that might occur from user commands
                //(aka input validation that i forgot to account for)
                System.out.println("Unexpected error occured: " + e.getMessage());
                e.printStackTrace();
            }
        }

        //if bot was stopped, update files before shutting down
        streamManager.saveData();
        shutdown();
    }

    private void processMessage(String line) {

        //print data received in console
        System.out.println(line);

        //respond to user commands
        if (line.contains("PRIVMSG")) {

            String[] breakup = line.split(" ");

            //grab message credentials
            String user = breakup[0].substring(1, breakup[0].indexOf("!")).toLowerCase();
            String chan = breakup[2];
            String cmd = breakup[3].substring(1);

            //op commands
            if (config.isOp(user)) {

                if (cmd.equals("!close")) {
                    closeSocket();
                }

                if (cmd.equals("SEND")) {
                    StringBuilder sb = new StringBuilder();
                    for (int i=4; i<breakup.length; i++) {
                        sb.append(breakup[i]);
                        sb.append(" ");
                    }
                    send(sb.toString().trim());
                }

                if (cmd.equals("!shutdown")) {
                    say("later friends", chan);
                    shutdown();
                }

                if (cmd.equals("!pt")) {

                    StringBuilder builder = new StringBuilder();
                    for (int i=4; i<breakup.length; i++) {
                        builder.append(breakup[i]);
                        builder.append(" ");
                    }

                    for (int i=0; i<3; i++) {
                        sleep(3000);
                        String[] partyTime = Announcer.createPartyMessage(builder.toString(), (int)(Math.random()*16));
                        for (String pt : partyTime) {
                            say(pt, chan);
                        }
                    }
                }
            }

            //stream chan commands
            if (config.getChannels().contains(chan)) {

                if (cmd.equals("!help")) {
                    say("- bot commands -", chan);
                    say("- !live - show list of who is currently live", chan);
                    say("- !add <twitch_name> - add stream to list using their twitch name", chan);
                    say("- !changeflavor - change display colors", chan);
                    say("- !offline <on/off> - change whether bot notifies when streams go offline", chan);
                }

                if (cmd.equals("!live")) {
                    streamManager.getLiveStreams(chan);
                }

                if (cmd.equals("!add")) {
                    if (breakup.length == 5) {
                        String stream = breakup[4].toLowerCase();
                        streamManager.addStream(stream, chan);
                    }
                }

                if (cmd.equals("!remove")) {
                    if (breakup.length == 5) {
                        String stream = breakup[4].toLowerCase();
                        streamManager.removeStream(stream, chan);
                    }
                }

                if (cmd.equals("!check")) {
                    if (breakup.length == 5) {
                        String stream = breakup[4].toLowerCase();
                        streamManager.checkStream(stream, chan);
                    }
                }

                if (cmd.equals("!changeflavor")) {
                    if (breakup.length == 4) {
                        streamManager.displayFlavors(chan);
                    }
                    if (breakup.length == 5) {
                        streamManager.changeFlavor(breakup[4], chan);
                    }
                }

                if (cmd.equals("!offline")) {
                    if (breakup.length == 4) {
                        say("usage: !offline <on/off>", chan);
                    }
                    if (breakup.length == 5) {
                        streamManager.setOfflineAnnouncements(breakup[4], chan);
                    }
                }
            }
        }
    }

    protected static void say(String msg, String chan) {
        //send message to specific channel on irc server
        //keep static/protected so announcer + stream manager can access

        try {
            out.write("PRIVMSG " + chan + " :" + msg + "\r\n");
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending message to channel ");
            e.printStackTrace();
        }
    }

    private void send(String msg) {
        try {
            out.write(msg + "\r\n");
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending to server " + e.getMessage());
        }
    }

    private void joinChannels() {
        for (String channel : config.getChannels()) {
            send("JOIN :" + channel);
        }
        //show version for confirmation
        FileLogger.logInfo("VERSIONINFO - " + versionInfo);
        say(versionInfo, operChan);
    }

    private void connectToServer() {
        System.out.println("Connecting to server...");
        try {
            socket = new Socket(config.getServer(), config.getPort());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            send("NICK " + config.getNick());
            send("USER " + config.getNick() + " " + config.getNick() + " " + config.getNick() + " :MSDLBot");
            send("PASS " + config.getIdent());

            //read data from server until connected
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("***CONNECTING***:" + line);

                //respond to ping
                if (line.startsWith("PING")) {
                    send("PONG " + line.substring(5));
                }

                //nickname already in use
                if (line.contains("433")) {
                    FileLogger.logWarning("Nickname already in use!");
                    closeSocket();
                    return;
                }
                
                //connected
                if (line.contains("004")) {
                    FileLogger.logInfo("Connected successfully: " + config.getServer());
                    break;
                }
                
                
            }

            joinChannels();
        } catch (IOException e) {
            FileLogger.logInfo("Could not connect to server " + e.getMessage());
        }
    }

    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void reconnect() {
        sleep(30000);
        FileLogger.logInfo("Disconnected from server, attempting reconnect");
        connectToServer();
    }

    private void closeSocket() {
        //***FOR DEBUGGING***
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error closing socket. " + e.getMessage());
        }
    }

    private void shutdown() {
        FileLogger.logInfo("Bot shutting down!");
        isRunning = false;
        System.exit(0);
    }

    @Override
    public void run() {
        //stay connected to server
        //check for stream updates

        while (true) {
            sleep(60000);
            send("PING " + config.getServer());
            streamManager.checkForUpdates();
        }
    }
}
