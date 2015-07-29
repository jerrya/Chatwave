package app.chatwave.me.FriendsPackage;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.packet.RosterPacket;

import java.util.ArrayList;
import java.util.List;

import app.chatwave.me.ConnectionManager;
import app.chatwave.me.MainActivity;
import app.chatwave.me.MainScreen;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;

public class FriendRequestList extends ListFragment implements FriendRequestAdapter.AdapterInterface {

    public static List<String> friendRequestList = new ArrayList<>();
    protected static final String TAG = "FriendRequestFragment";

    public FriendRequestAdapter adapter;

    private ImageButton requestBack;

    private static FriendRequestList inst;
    public static FriendRequestList instance() {
        return inst;
    }
    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.friend_request_layout, container, false);

        requestBack = (ImageButton) view.findViewById(R.id.requestBack);
        requestBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adapter = new FriendRequestAdapter(getActivity(), friendRequestList, this);
        getListView().setAdapter(adapter);
    }

    public void denyFriendRequest(String username) {

        MainActivity.instance().sendTracker("FriendRequestList", "Deny button", "Denied a friend", "Button click");

        RosterPacket packet = new RosterPacket();
        packet.setType(IQ.Type.set);
        RosterPacket.Item item  = new RosterPacket.Item(username, null);
        item.setItemType(RosterPacket.ItemType.remove);
        packet.addRosterItem(item);
        try {
            ConnectionManager.connection.sendStanza(packet);
        } catch (SmackException.NotConnectedException e) {
            if(Config.logginOn) {
                e.printStackTrace();
            }
        }

        Presence unsubscribe = new Presence(Presence.Type.unsubscribe);
        unsubscribe.setTo(username);
        try {
            ConnectionManager.connection.sendStanza(unsubscribe);
            if(Config.logginOn) {
                Log.e(TAG, "Unsubscribe packet sent");
            }
        } catch (SmackException.NotConnectedException e) {
            if(Config.logginOn) {
                e.printStackTrace();
            }
        }

        Presence unsubscribed = new Presence(Presence.Type.unsubscribed);
        unsubscribed.setTo(username);
        try {
            ConnectionManager.connection.sendStanza(unsubscribed);
            if(Config.logginOn) {
                Log.e(TAG, "UnsubscribeD packet sent");
            }
        } catch (SmackException.NotConnectedException e) {
            if(Config.logginOn) {
                e.printStackTrace();
            }
        }

        removeAndUpdate(username);
    }

    public void acceptFriendRequest(String username) {

        MainActivity.instance().sendTracker("FriendRequestList", "Accept button", "Accepted a friend", "Button click");

        Roster roster = Roster.getInstanceFor(ConnectionManager.connection);
        try {
            String dbUsername = username.replace(Config.RESOURCE_SUFFIX,"");
            dbUsername = dbUsername.replace("/Smack","");

            // Sends subscribe packet
            roster.createEntry(username, dbUsername, null);

//            MainActivity.instance().createNewChatDB(dbUsername);
//            Log.e(TAG, "Created db for: " + dbUsername);

//            Log.e(TAG, "Entry created and sub packet sent to " + username);
        } catch (Exception e){

        }

        // Sends subscribeD packet
        Presence subscribed = new Presence(Presence.Type.subscribed);
        subscribed.setTo(username);
        try {
            ConnectionManager.connection.sendStanza(subscribed);
            if(Config.logginOn) {
                Log.e(TAG, "Subscribed packet sent");
            }
        } catch (SmackException.NotConnectedException e) {
            if(Config.logginOn) {
                Log.e(TAG, "Error: " + e.getMessage());
            }
        }

        removeAndUpdate(username);
    }

    public void removeAndUpdate(String username) {
        // Called only if the fragment is visible and not null
        if(friendRequestList.contains(username)) {
            friendRequestList.remove(username);
            adapter.notifyDataSetChanged();
            if(Config.logginOn) {
                Log.e(TAG, "Removed " + username + " from friend request list");
            }
        }
    }

    @Override
    public void buttonClicked(String username, boolean accepted) {
        MainScreen.instance().updateRequestCount();
        if(accepted) {
            acceptFriendRequest(username);
        } else {
            denyFriendRequest(username);
        }
    }
}