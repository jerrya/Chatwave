package app.chatwave.me.FriendsPackage;

public class FriendItem {

    public String sender;
    public String name;
    public String avatarUrl;
    public boolean isSelected;

    public FriendItem(String sender, String name, String avatarUrl) {
        this.sender = sender;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.isSelected = false;
    }
}
