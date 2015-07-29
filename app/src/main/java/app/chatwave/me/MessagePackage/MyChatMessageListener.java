package app.chatwave.me.MessagePackage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

import java.io.IOException;

import app.chatwave.me.ConnectionManager;
import app.chatwave.me.Database.MessageDatabase;
import app.chatwave.me.MainActivity;
import app.chatwave.me.MainScreen;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;
import app.chatwave.me.UtilityPackage.UtilityClass;

public class MyChatMessageListener implements ChatMessageListener {

    protected static final String TAG = "MyChatMessageListener";
    private Roster roster = Roster.getInstanceFor(ConnectionManager.connection);
    private NotificationManager notificationManager;
    private int notifID = 33;

    private Context mContext;
    private MessageDatabase mDatabase;
    public MyChatMessageListener(Context mContext, MessageDatabase mDatabase) {
        this.mContext = mContext;
        this.mDatabase = mDatabase;
    }

    @Override
    public void processMessage(Chat chat, Message message) {
//        final String fromAddress = message.getFrom().replace("/Smack", "");
        final String fromAddress = message.getFrom();
        String picture = message.getSubject("picture");

        RosterEntry mEntry = roster.getEntry(fromAddress.replace("/Smack", "").toLowerCase());

        if(mEntry != null) {
            String saveDbAddress = fromAddress.replace(Config.RESOURCE_SUFFIX + "/Smack", "");

            if(Config.logginOn) {
                Log.e(TAG, "Message from " + chat.getParticipant() + ": " + message.getBody() + " url: " + picture);
            }

            // Save all our messages into this list
            MainActivity.messageList.add(0, new MessageItem(saveDbAddress, mEntry.getName(), message.getBody(), picture, UtilityClass.getCurrentDateAndTime()));

            // Save new messages into this list
            MessageList.newMessagesList.add(0, new MessageItem(saveDbAddress, mEntry.getName(), message.getBody(), picture, UtilityClass.getCurrentDateAndTime()));

            insertIntoDB(saveDbAddress, message.getBody(), picture, UtilityClass.getCurrentDateAndTime());

            if(MainActivity.instance() != null) {

                MainActivity.instance().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainScreen.instance().updateMessageCount();
                    }
                });

                if(MainActivity.instance().getSupportFragmentManager().findFragmentByTag("messagelist") != null
                        && MainActivity.instance().getSupportFragmentManager().findFragmentByTag("messagelist").isVisible()) {
                    MainActivity.instance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessageList.instance().messageAdapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    createNotification(mEntry.getName().replace(Config.RESOURCE_SUFFIX,"") + " sent you something", mContext);
                }
            } else {
                createNotification(message.getBody(), mContext);
            }
        }
    }

    public void insertIntoDB(final String sender, final String messageUrl, final String picture, final String date) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mDatabase.getReadableDatabase().execSQL("INSERT INTO c" + MainActivity.username + " (sender, messageurl, picture, date) VALUES ('"
                        + sender + "', '" + messageUrl + "', '" + picture + "', '" + date + "');");
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
            }
        }.execute();
    }

    public void playAudio(String url) {
        final MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Toast.makeText(mContext, "Unable to play audio", Toast.LENGTH_SHORT).show();
        }

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });

        mediaPlayer.release();
    }

    public void createNotification(String message, Context context) {
        NotificationCompat.Builder notificBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(Config.APP_NAME)
                .setContentText(message)
                .setTicker("You have new messages")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true);

        notificBuilder.setDefaults(Notification.DEFAULT_ALL);

        Intent friendIntent = new Intent(context, MainActivity.class);

        android.support.v4.app.TaskStackBuilder taskStackBuilder = android.support.v4.app.TaskStackBuilder.create(context);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(friendIntent);

        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificBuilder.setContentIntent(pendingIntent);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notifID, notificBuilder.build());
    }
}