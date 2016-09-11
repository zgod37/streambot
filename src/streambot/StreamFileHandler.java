package streambot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

public class StreamFileHandler {

    private String globalFilePath = "";
    private String channelFilePath = "";

    public StreamFileHandler() {
        globalFilePath = Paths.get("data" + File.separator + "streamlists" + File.separator + "global.txt").toString();
        channelFilePath = Paths.get("data" + File.separator + "streamlists" + File.separator + "#").toString();
    }

    private ArrayList<String> getList(String filePath) {
        //get list of twitch streams from file

        ArrayList<String> channels = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                channels.add(line);
            }
            reader.close();
        } catch (IOException e) {
            System.out.println("Error reading file " + filePath);
            e.printStackTrace();
        }

        return channels;
    }

    public ArrayList<String> getGlobalList() {
        return getList(globalFilePath);
    }

    public ArrayList<String> getChannelList(String channel) {
        return getList(channelFilePath + channel + ".txt");
    }

    private void rewriteFile(ArrayList<String> streams, String filePath) {

        File currentFile = new File(filePath);
        File tempFile = new File(filePath + "temp.txt");

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile, true));
            for (String stream : streams) {
                writer.write(stream + System.getProperty("line.separator"));
            }
            writer.close();

            long lengthChanged = currentFile.length() - tempFile.length();

            if (!currentFile.delete()) System.out.println("Problem deleting " + filePath);
            if (!tempFile.renameTo(currentFile)) System.out.println("Couldn't rename " + filePath);

            if (lengthChanged != 0) {
                System.out.println(filePath + " changed " + Math.abs(lengthChanged) + " bytes.");
            } else {
                System.out.println(filePath + " unchanged.");
            }
        } catch (IOException e) {
            System.out.println("Error rewriting file " + filePath);
            e.printStackTrace();
        }

    }

    public void rewriteChannelFile(ArrayList<String> streams, String channel) {
        rewriteFile(streams, channelFilePath + channel + ".txt");
    }

    public void rewriteGlobalFile(ArrayList<String> streams) {
        rewriteFile(streams, globalFilePath);
    }


}
