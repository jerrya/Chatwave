package app.chatwave.me.MessagePackage;

public class MessageItem {

    String sender, messageUrl, picture, date, name;

    public MessageItem(String sender, String name, String messageUrl, String picture, String date) {
        this.sender = sender;
        this.name = name;
        this.messageUrl = messageUrl;
        this.picture = picture;
        this.date = date;
    }
}
