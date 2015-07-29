package app.chatwave.me.EventPackage;

public class SoundPickedEvent {

    public String soundName, soundUrl;

    public SoundPickedEvent(String soundName, String soundUrl) {
        this.soundName = soundName;
        this.soundUrl = soundUrl;
    }

}
