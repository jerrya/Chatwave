package app.chatwave.me.FriendsPackage;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

import java.util.ArrayList;
import java.util.Collection;

import app.chatwave.me.ConnectionManager;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;

public class FriendsList extends ListFragment {

    private static final String TAG = "FriendsList";

    public static ArrayList<FriendItem> friendsList = new ArrayList<>();
    public static FriendsAdapter friendsAdapter;

    private SwipeRefreshLayout mSwipeRefresh;

    private Collection<RosterEntry> rosterCollection;

    private ImageButton friendBack;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.friends_list_layout, container, false);

        mSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.friends_list_refresh);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getFriends();
            }
        });
        mSwipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);


        friendBack = (ImageButton) view.findViewById(R.id.friendBack);
        friendBack.setOnClickListener(new View.OnClickListener() {
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
        friendsList.clear();
        getFriends();
    }

    public void createRosterRequest(String username, String fullName) {
        Roster roster = Roster.getInstanceFor(ConnectionManager.connection);
        username = username + Config.RESOURCE_SUFFIX;
        try {
            if(Config.logginOn) {
                Log.e(TAG, "Sent to: " + username);
            }
            // This creates the entry and sends subscribe packet
            roster.createEntry(username, fullName, null);
            Log.e(TAG, "Created roster entry: " + username);

            Toast.makeText(getActivity(), "Friend request sent!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Unable to send friend request", Toast.LENGTH_LONG).show();
        }
    }

    public void getFriends() {
        friendsList.clear();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Roster roster = Roster.getInstanceFor(ConnectionManager.connection);
                rosterCollection = roster.getEntries();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                for(RosterEntry entry : rosterCollection) {
                    friendsList.add(new FriendItem(entry.getUser(), entry.getName(), ""));

                    if(Config.logginOn) {
                        Log.e(TAG, "Friend: " + entry.getUser() + " name: " + entry.getName());
                    }

                }
                setupAdapter();
            }
        }.execute();
    }

    public void setupAdapter() {
        if(getActivity() != null) {
            friendsAdapter = new FriendsAdapter(getActivity(), friendsList);
            getListView().setAdapter(friendsAdapter);
        }
        mSwipeRefresh.setRefreshing(false);
    }
}
