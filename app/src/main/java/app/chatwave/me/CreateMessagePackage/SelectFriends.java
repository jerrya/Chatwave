package app.chatwave.me.CreateMessagePackage;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.afollestad.materialdialogs.MaterialDialog;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import app.chatwave.me.ConnectionManager;
import app.chatwave.me.FriendsPackage.FriendItem;
import app.chatwave.me.LoginScreen;
import app.chatwave.me.MainActivity;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;
import app.chatwave.me.UtilityPackage.UtilityClass;

public class SelectFriends extends ListFragment {

    private static final String TAG = "SelectFriends";

    public static ArrayList<String> toSendFriends = new ArrayList<>();
    public static ArrayList<FriendItem> friendsList = new ArrayList<>();
    private BootstrapButton createMessageSend;
    private SelectFriendAdapter selectFriendAdapter;

    private String soundName;
    private String soundUrl;
    private boolean hasPicture;

    private Collection<RosterEntry> rosterCollection;

    private ProgressDialog progressDialog;

    private String encodedString;
    private Response response;
    private String trueId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.select_friend_layout, container, false);

        ImageButton select_friends_panel_exit = (ImageButton) view.findViewById(R.id.select_friends_panel_exit);
        select_friends_panel_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        createMessageSend = (BootstrapButton) view.findViewById(R.id.createMessageSend);
        createMessageSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(toSendFriends.size() > 0) {
                    if(hasPicture) {
                        MainActivity.instance().sendTracker("SelectFriends", "Send button", "Sent a picture", "Button click");
                        encodeImage();
                    } else {
                        MainActivity.instance().sendTracker("SelectFriends", "Send button", "Sent a sound", "Button click");
                        sendSounds("none");
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();
        if(args != null) {
            soundName = args.getString("soundname");
            soundUrl = args.getString("soundurl");
            hasPicture = args.getBoolean("haspicture", false);
        }

        getFriends();

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
    }

    public void encodeImage() {
        progressDialog.setMessage("Sending");
        progressDialog.show();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                // Must compress the Image to reduce image size to make upload easy

                MainActivity.correctedImage.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byte_arr = stream.toByteArray();
                // Encode Image to String
                encodedString = Base64.encodeToString(byte_arr, 0);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                uploadImage();
            }
        }.execute();
    }

    public void uploadImage() {
        trueId = UtilityClass.randomString(8);
        String imageName = trueId + ".jpg";

        final OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormEncodingBuilder()
                .add("filename", imageName)
                .add("image", encodedString).build();

        final Request request = new Request.Builder().url(Config.queryPrefix + "uimage.php").post(formBody).build();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                MainActivity.correctedImage = null;
                if(response.isSuccessful()) {
                    sendSounds(trueId);
                    if(Config.logginOn) {
                        Log.e(TAG, "Image uploaded");
                    }
                } else {
                    progressDialog.hide();
                    if(Config.logginOn) {
                        Log.e(TAG, "Image uploaded");
                    }
                }
            }
        }.execute();
    }

    public void getFriends() {
        toSendFriends.clear();
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
            selectFriendAdapter = new SelectFriendAdapter(getActivity(), friendsList);
            getListView().setAdapter(selectFriendAdapter);
        }
    }

    public void sendSounds(final String trueId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                for(String address : toSendFriends) {
                    sendMessage(address, trueId);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                progressDialog.hide();
                if(messageSent) {
                    successDialog("Message sent");
                    if(Config.logginOn) {
                        Log.e(TAG, "Message sent");
                    }
                } else {
                    askDialog("Unable to send message");
                    if(Config.logginOn) {
                        Log.e(TAG, "Unable to send message");
                    }
                }
            }
        }.execute();
    }

    boolean messageSent = false;
    public void sendMessage(final String address, String trueId) {
        ChatManager chatManager = ChatManager.getInstanceFor(ConnectionManager.connection);
        final Chat newChat = chatManager.createChat(address);

        final Message message = new Message();
        message.addSubject("name", soundName);
        message.addSubject("picture", trueId);
        message.setBody(soundUrl);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                if(Config.logginOn) {
                    Log.e(TAG, "Sending to: " + address);
                }

                try {
                    newChat.sendMessage(message);
                    messageSent = true;
                } catch (SmackException.NotConnectedException e) {
                    messageSent = false;
                    if(Config.logginOn) {
                        Log.e(TAG, "Unable to send msg: " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(messageSent) {
                    if(Config.logginOn) {
                        Log.e(TAG, "Message sent");
                    }
                } else {
                    if(Config.logginOn) {
                        Log.e(TAG, "Unable to send message");
                    }
                }
            }
        }.execute();
    }

    public void successDialog(String message) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title("Chatwave")
                .content(message)
                .titleColorRes(R.color.bootstra_blue)
                .positiveText("OK")
                .positiveColorRes(R.color.bootstra_blue)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                            getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        } else {
                            LoginScreen loginScreen = new LoginScreen();
                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragment_container, loginScreen, "loginscreen");
                            transaction.commit();
                        }
                        dialog.dismiss();
                    }
                }).show();
    }

    public void askDialog(String message) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title("Chatwave")
                .content(message)
                .positiveText("OK")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        dialog.dismiss();
                    }
                }).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
