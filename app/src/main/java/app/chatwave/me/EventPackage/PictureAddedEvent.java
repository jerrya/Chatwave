package app.chatwave.me.EventPackage;

public class PictureAddedEvent {

    public String encodedString;
    public boolean isCamera;
    public String soundName, soundUrl;

    public PictureAddedEvent(String soundUrl, String soundName) {
        this.soundUrl = soundUrl;
        this.soundName = soundName;
    }

}
