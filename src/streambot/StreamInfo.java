package streambot;

public class StreamInfo {

    private String name = "";
    private String status = "";
    private String game = "";
    private String url = "";

    private int offlineChecks;
    private boolean statusChanged = false;


    public StreamInfo(String name) {
        this.name = name;
        this.status = null;
        this.url = "https://twitch.tv/" + name;
        this.offlineChecks = 0;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getUrl() {
        return url;
    }

    public void setStatusChanged(boolean statusChanged) {
        this.statusChanged = statusChanged;
    }

    public boolean getStatusChanged() {
        return statusChanged;
    }

    public int getOfflineChecks() {
        return offlineChecks;
    }

    public void resetOfflineChecks() {
        offlineChecks = 0;
    }

    public void incrementOfflineChecks() {
        offlineChecks++;
    }
}
