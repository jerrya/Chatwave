package app.chatwave.me.FriendsPackage;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import app.chatwave.me.ConnectionManager;
import app.chatwave.me.MainActivity;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;
import app.chatwave.me.UtilityPackage.UtilityClass;

public class AddFriend extends Fragment {

    private EditText add_friend_edit_text;
    private Button add_friend_button, add_friend_back;
    private ProgressDialog progressDialog;
    protected static final String TAG = "AddFriend";
    private VCard vCard;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_friend, container, false);

        add_friend_edit_text = (EditText) view.findViewById(R.id.add_friend_edit_text);
        add_friend_edit_text.setFilters(new InputFilter[]{inputFilter, new InputFilter.LengthFilter(20)});

        add_friend_button = (Button) view.findViewById(R.id.add_friend_button);
        add_friend_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = add_friend_edit_text.getText().toString().trim() + Config.RESOURCE_SUFFIX;
                String fullName = add_friend_edit_text.getText().toString().trim();
                MainActivity.instance().sendTracker("AddFriend", "Add friend button", "Adding a friend", "Button click");
                if(fullName.length() > 0 && username.length() > 0) {
                    if(Config.logginOn) {
                        Log.e(TAG, "Typed: " + username + " name: " + fullName);
                    }

                    checkIfFriendExists(username, fullName);
                    UtilityClass.hideKeyboard(getActivity());
                }
            }
        });

        add_friend_back = (Button) view.findViewById(R.id.add_friend_back);
        add_friend_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
    }

    private String blockCharacterSet = "@)(~#^|$%&*!/";
    private InputFilter inputFilter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if(source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }
            return null;
        }
    };

    public void checkIfFriendExists(final String username, final String fullName) {
        progressDialog.setMessage("Adding...");
        progressDialog.show();

        final VCardManager vCardManager = VCardManager.getInstanceFor(ConnectionManager.connection);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    vCard = vCardManager.loadVCard(username);
                } catch (Exception e) {
                    if(Config.logginOn) {
                        Log.e(TAG, "Friend Exists error: " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(vCard != null) {
                    createRosterRequest(username, fullName);
                } else {
                    Toast.makeText(getActivity(), "That user does not exist", Toast.LENGTH_LONG).show();
                    progressDialog.hide();

                    addFriendDialog("No such user");
                }
                vCard = null;
            }
        }.execute();
    }

    public void createRosterRequest(String username, String fullName) {
        Roster roster = Roster.getInstanceFor(ConnectionManager.connection);
        final RosterEntry newEntry = roster.getEntry(username);

        progressDialog.hide();

        if(newEntry == null) {
            try {
                if(Config.logginOn) {
                    Log.e(TAG, "Sent to: " + username);
                }
                // This creates the entry and sends subscribe packet
                roster.createEntry(username, fullName, null);

                if(Config.logginOn) {
                    Log.e(TAG, "Created roster entry: " + username);
                }

                addFriendDialog("Friend request sent");

                Toast.makeText(getActivity(), "Friend request sent!", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Unable to send friend request", Toast.LENGTH_LONG).show();

                addFriendDialog("Unable to send friend request");
            }
        } else {
            Toast.makeText(getActivity(), "This person is already in your friends list", Toast.LENGTH_LONG).show();

            addFriendDialog("This person is already in your friends list");
        }
    }

    public void addFriendDialog(String message) {
        new MaterialDialog.Builder(getActivity())
                .title("Add Friend")
                .titleColorRes(R.color.bootstra_blue)
                .content(message)
                .positiveColorRes(R.color.bootstra_blue)
                .positiveText("OK")
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
