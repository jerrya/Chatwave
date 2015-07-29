package app.chatwave.me.EventPackage;

public class MessageReplyEvent {

    public String messageReceipient;

    public MessageReplyEvent(String messageReceipient) {
        this.messageReceipient = messageReceipient;
    }

}
