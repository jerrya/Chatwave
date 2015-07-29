package app.chatwave.me.MessagePackage;

import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import app.chatwave.me.ConnectionManager;
import app.chatwave.me.Database.MessageDatabase;
import app.chatwave.me.EventPackage.MessageReplyEvent;
import app.chatwave.me.EventPackage.OpenPictureMessage;
import app.chatwave.me.FriendsPackage.FriendItem;
import app.chatwave.me.FriendsPackage.FriendsList;
import app.chatwave.me.MainActivity;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;
import app.chatwave.me.UtilityPackage.SoundList;
import app.chatwave.me.UtilityPackage.UtilityClass;
import de.greenrobot.event.EventBus;

public class MessageList extends ListFragment {

    private ImageButton messageBack, messageReplyExit;
    private BootstrapButton messageReplySend;
    private String messageRecipient;

    private SoundItem soundItem;

    private MessageDatabase mDatabase;

    public static SlidingUpPanelLayout mSlidingPanel;
    private GridView gridView;
    protected static final String TAG = "MessageList";

    private MediaPlayer mediaPlayer;
    public MessageAdapter messageAdapter;
    private SoundAdapter soundAdapter;

    public static ArrayList<MessageItem> newMessagesList = new ArrayList<>();
    private ArrayList<SoundItem> soundList = new ArrayList<>();

    private SwipeRefreshLayout mSwipeRefresh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.message_list_layout, container, false);

        mSlidingPanel = (SlidingUpPanelLayout) view.findViewById(R.id.sliding_layout);
        mSlidingPanel.setTouchEnabled(false);

        gridView = (GridView) view.findViewById(R.id.gridview);

        messageBack = (ImageButton) view.findViewById(R.id.messageBack);
        messageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        messageReplyExit = (ImageButton) view.findViewById(R.id.messageReplyExit);
        messageReplyExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                UtilityClass.hideKeyboard(getActivity());
            }
        });

        messageReplySend = (BootstrapButton) view.findViewById(R.id.messageReplySend);
        messageReplySend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(messageRecipient);
            }
        });

        mSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.friends_list_refresh);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                messageAdapter.notifyDataSetChanged();
                mSwipeRefresh.setRefreshing(false);
            }
        });
        mSwipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        initiateDatabase();

        setUpSoundList();

//        getFriends();
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
        newMessagesList.clear();
        EventBus.getDefault().register(this);
        mediaPlayer = new MediaPlayer();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mediaPlayer.release();
        ((AudioManager)getActivity().getSystemService(
                Context.AUDIO_SERVICE)).requestAudioFocus(
                new AudioManager.OnAudioFocusChangeListener() {
                    @Override
                    public void onAudioFocusChange(int focusChange) {
                    }
                }, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    public void onEventMainThread(final MessageReplyEvent replyEvent){
        if(replyEvent != null && !replyEvent.messageReceipient.isEmpty()) {
            messageRecipient = replyEvent.messageReceipient + Config.RESOURCE_SUFFIX;
            mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            if(Config.logginOn) {
                Log.e(TAG, "Received: " + replyEvent.messageReceipient);
            }
        }
    }

    private static MessageList inst;
    public static MessageList instance() {
        return inst;
    }

    public void setUpSoundList() {
        for(int i = 0; i< SoundList.mSoundList.length; i++) {
            soundList.add(new SoundItem(SoundList.mSoundList[i][0], SoundList.mSoundList[i][1]));
        }

        if(getActivity() != null) {
            soundAdapter = new SoundAdapter(getActivity(), soundList);
            gridView.setAdapter(soundAdapter);
            gridView.setClickable(true);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    soundItem = soundAdapter.getItem(position);

                    if(isVolumeOn()) {
                        playAudio(Config.AUDIO_URL_PREFIX + soundItem.url);
                    } else {
                        checkVolume();
                    }
                }
            });
        }
    }

    public void playAudio(String url) {
        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Toast.makeText(getActivity(), "Unable to play audio", Toast.LENGTH_SHORT).show();
        }

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });
    }

    public void checkVolume() {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title("Warning")
                .content("Your volume is currently turned off. Would you like to change it?")
                .positiveText("OK")
                .negativeText("Cancel")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        dialog.dismiss();
                        AudioManager audio = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dialog.dismiss();
                    }
                }).show();
    }

    public boolean isVolumeOn() {
        AudioManager mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int mVolumeMusic = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(mVolumeMusic == 0) {
            return false;
        }
        return true;
    }


    private Collection<RosterEntry> rosterCollection;
    private Roster roster = Roster.getInstanceFor(ConnectionManager.connection);
    public void initiateDatabase() {
        mDatabase = new MessageDatabase(getActivity());
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mDatabase.getReadableDatabase()
                        .execSQL("CREATE TABLE IF NOT EXISTS c" + MainActivity.username + " (id integer primary key, sender text, messageurl text, picture text, date text);");
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                retrieveMessagesFromDB();
            }
        }.execute();
    }

    private Cursor cursor;
    public void retrieveMessagesFromDB() {
        MainActivity.messageList.clear();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mDatabase = new MessageDatabase(getActivity());
                cursor = mDatabase.getReadableDatabase().rawQuery("SELECT * FROM c" + MainActivity.username + " ORDER BY id DESC", null);
                while(cursor.moveToNext()) {
                    String sender = cursor.getString(cursor.getColumnIndex("sender"));
                    String messageUrl = cursor.getString(cursor.getColumnIndex("messageurl"));
                    String picture = cursor.getString(cursor.getColumnIndex("picture"));
                    String date = cursor.getString(cursor.getColumnIndex("date"));

                    if(cursor.getCount() > 0) {
                        RosterEntry mEntry = roster.getEntry(sender.toLowerCase() + Config.RESOURCE_SUFFIX);
                        MainActivity.messageList.add(new MessageItem(sender, mEntry.getName(), messageUrl, picture, date));
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                messageAdapter = new MessageAdapter(getActivity(), MainActivity.messageList);
                getListView().setAdapter(messageAdapter);
                getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                        MessageItem messageItem = messageAdapter.getItem(position);

                        MainActivity.instance().sendTracker("MessageList", "Message button", "Clicked a message", "Button click");

                        if(messageAdapter.getItem(position).picture.equals("none")) {
                            if(Config.logginOn) {
                                Log.e(TAG, "Does NOT have picture");
                            }
                            if(isVolumeOn()) {
                                playAudio(Config.AUDIO_URL_PREFIX + messageAdapter.getItem(position).messageUrl);
                            } else {
                                checkVolume();
                            }
                        } else {
                            if(Config.logginOn) {
                                Log.e(TAG, "Has picture!");
                            }
                            EventBus.getDefault().post(new OpenPictureMessage(messageItem.name, messageItem.messageUrl, messageItem.picture));
                        }
                    }
                });
                messageAdapter.notifyDataSetChanged();
                mDatabase.close();
                cursor.close();
            }
        }.execute();
    }

    private boolean messageSent = false;
    public void sendMessage(String address) {
        MainActivity.instance().sendTracker("MessageList", "Reply button", "Sent a reply", "Button click");
        ChatManager chatManager = ChatManager.getInstanceFor(ConnectionManager.connection);
        final Chat newChat = chatManager.createChat(address);

        final Message message = new Message();
        message.addSubject("name", soundItem.name);
        message.addSubject("picture", "none");
        message.setBody(soundItem.url);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    newChat.sendMessage(message);
                    messageSent = true;
                } catch (SmackException.NotConnectedException e) {
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
                    Toast.makeText(getActivity(), "Sent!", Toast.LENGTH_LONG).show();
                    mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                } else {
                    Toast.makeText(getActivity(), "Unable to send", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }
}
