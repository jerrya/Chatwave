package app.chatwave.me;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import app.chatwave.me.Database.MessageDatabase;
import app.chatwave.me.EventPackage.MessageEvent;
import app.chatwave.me.FriendsPackage.FriendRequestList;
import app.chatwave.me.MessagePackage.MessageList;
import app.chatwave.me.MessagePackage.MyChatMessageListener;
import app.chatwave.me.UtilityPackage.Config;

import de.greenrobot.event.EventBus;

public class ConnectionManager extends Service {

    protected static final String TAG = "ConnectionManagerTAG";

    public static AbstractXMPPConnection connection;
    private XMPPTCPConnectionConfiguration connectionConfiguration;
    private MessageDatabase mDatabase;

    public static String globalUsername;
    public static String globalPassword;

    boolean startConnected = false;

    private final IBinder binder = new ServiceBinder();
    public class ServiceBinder extends Binder {
        ConnectionManager getService() {
            return ConnectionManager.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return START_STICKY;
    }

    protected void onHandleIntent(Intent intent) {
        disconnect();

        if(mDatabase != null) {
            mDatabase.close();
        }

        mDatabase = new MessageDatabase(getBaseContext());

        if(sContext != null) {
            sContext.unregisterReceiver(ALARM_BROADCAST_RECEIVER);
            sAlarmManager.cancel(sPendingIntent);
            sContext = null;
        }

        if(intent == null) {

            if(Config.logginOn) {
                Log.e(TAG, "Stopped service");
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            String user = sharedPreferences.getString("username", null);
            String pw = sharedPreferences.getString("password", null);

            globalUsername = user;
            globalPassword = pw;

            startLogin(user, pw);
            return;
        }

        int event = intent.getIntExtra("event", 2);
        if(event == 0) { // Login
            String username = intent.getStringExtra("username");
            String password = intent.getStringExtra("password");

            globalUsername = username;
            globalPassword = password;

            if(username != null && password != null) {
                startLogin(username, password);
            }

        } else if(event == 1) { // Create account
            String username = intent.getStringExtra("username");
            String password = intent.getStringExtra("password");
            String email = intent.getStringExtra("email");

            globalUsername = username;
            globalPassword = password;

            if(username != null && password != null) {
                createAccount(username, password, email);
            }

        } else if(event == 2) { // Logout
            mDatabase.close();
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private static final BroadcastReceiver ALARM_BROADCAST_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final PingManager pingManager = PingManager.getInstanceFor(connection);
            pingManager.registerPingFailedListener(new PingFailedListener() {
                @Override
                public void pingFailed() {

                    if(globalUsername != null && globalPassword != null && LoginScreen.connectionManager != null) {
                        LoginScreen.connectionManager.startLogin(globalUsername, globalPassword);
                    }

                    if(Config.logginOn) {
                        Log.e(TAG, "Ping failed!");
                    }
                }
            });

            Async.go(new Runnable() {
                @Override
                public void run() {
                    try {
                        pingManager.pingMyServer();
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }
//                    pingManager.pingServerIfNecessary();
                }
            }, "PingServerIfNecessary (" + connection.getConnectionCounter() + ')');
        }
    };

    private static Context sContext;
    private static PendingIntent sPendingIntent;
    private static AlarmManager sAlarmManager;

    public void createPingAlarm(Context context) {
        sContext = context;
        String PING_ALARM_ACTION = "org.igniterealtime.smackx.ping.ACTION";
        context.registerReceiver(ALARM_BROADCAST_RECEIVER, new IntentFilter(PING_ALARM_ACTION));
        sAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        sPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(PING_ALARM_ACTION), 0);
        sAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, sPendingIntent);
    }

    public void startLogin(final String username, final String password) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                connectionConfiguration = XMPPTCPConnectionConfiguration.builder()
                        .setUsernameAndPassword(username, password)
                        .setServiceName(Config.SERVICE_NAME)
                        .setHost(Config.HOST_NAME)
                        .setPort(5222)
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                        .build();

                connection = new XMPPTCPConnection(connectionConfiguration);

                createPacketListener();

                try {
                    connection.connect();
                    startConnected = true;
                } catch (Exception e) {
                }

                if(Config.logginOn) {
                    Log.e(TAG, "Connected");
                }

                if(startConnected) {
                    connectionLogin(username, password);
                } else {
                    MainActivity.instance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.instance(), "Unable to connect", Toast.LENGTH_LONG).show();
                        }
                    });
                }

            }
        }).start();
    }

    boolean loggedIn = true;
    public void connectionLogin(final String username, final String password) {

        try {
            connection.login();
        } catch (Exception e) {
            if(Config.logginOn) {
                Log.e(TAG, "Unable to login: " + e);
            }
            loggedIn = false;
        }

        if(!loggedIn) {
            MainActivity.instance().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.instance(), "Unable to login", Toast.LENGTH_LONG).show();
                }
            });

            disconnect();
            loggedIn = true;
        } else {
            createPingAlarm(getBaseContext());

            MainActivity.username = username.replace(Config.RESOURCE_SUFFIX, "");
            createDatabase(username.replace(Config.RESOURCE_SUFFIX, ""));
            saveLoginDetails(username, password);
            createChatListener();
            EventBus.getDefault().post(new MessageEvent(true));
            if(Config.logginOn) {
                Log.e(TAG, "Logged in");
            }
        }
    }

    private MyChatMessageListener mChatMessageListener;
    public void createChatListener() {
        if(connection != null) {
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            chatManager.setNormalIncluded(false);
            chatManager.addChatListener(new ChatManagerListener() {
                @Override
                public void chatCreated(Chat chat, boolean createdLocally) {
                    if (!createdLocally) {
                        if (mChatMessageListener != null) {
                            chat.removeMessageListener(mChatMessageListener);
                            if(Config.logginOn) {
                                Log.e(TAG, "Removed chat listener");
                            }
                        }
                        mChatMessageListener = new MyChatMessageListener(getBaseContext(), mDatabase);
                        chat.addMessageListener(mChatMessageListener);
                    }
                }
            });
        }
    }

    private void saveLoginDetails(String username, String password) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        if(Config.logginOn) {
            Log.e(TAG, "Saved details");
        }
        editor.apply();
    }

    boolean canConnect = false;
    public void createAccount(final String username, final String password, final String email) {
        final Map<String, String> emailMap = new HashMap<>();
        emailMap.put("email", email);

        XMPPTCPConnectionConfiguration conf = XMPPTCPConnectionConfiguration.builder()
                .setServiceName(Config.SERVICE_NAME)
                .setHost(Config.HOST_NAME)
                .setPort(5222)
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled) // security settings?
                .build();

        connection = new XMPPTCPConnection(conf);

        createPacketListener();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    connection.connect();
                    canConnect = true;
                } catch (Exception e) {
                    canConnect = false;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(canConnect) {
                    initializeAccount(username, password, emailMap, connection);
                    if(Config.logginOn) {
                        Log.e(TAG, "Connected");
                    }
                } else {
                    MainActivity.instance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.instance(), "Unable to connect", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }.execute();
    }

    boolean accountCreated = false;
    public void initializeAccount(final String username, final String password, final Map<String, String> emailMap, final AbstractXMPPConnection con) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                AccountManager accountManager = AccountManager.getInstance(con);
                accountManager.sensitiveOperationOverInsecureConnection(true);
                try {
                    accountManager.createAccount(username, password, emailMap);
                    accountCreated = true;
                    if(Config.logginOn) {
                        Log.e(TAG, "Account created");
                    }
                    MainActivity.instance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.instance(), "Account created", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    accountCreated = false;
                    if(Config.logginOn) {
                        Log.e(TAG, "Unable to create account: " + e.getMessage());
                    }
                    if(e.getMessage().equals("XMPPError: conflict - cancel")) {
                        MainActivity.instance().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.instance(), "That username already exists", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(accountCreated) {
                    createAccountLogin(username, password, con);
                } else {
                    con.disconnect();
                }
            }
        }.execute();
    }

    boolean accountLoggedIn = false;
    public void createAccountLogin(final String username, final String password, final AbstractXMPPConnection con) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    con.login(username, password);
                    accountLoggedIn = true;
                    if(Config.logginOn) {
                        Log.e(TAG, "Logged in");
                    }
                } catch (Exception e) {
                    accountLoggedIn = false;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(accountLoggedIn) {

                    MainActivity.username = username.replace(Config.RESOURCE_SUFFIX, "");
                    createPingAlarm(getBaseContext());

                    createVCard(username, con);
                    createDatabase(username.replace(Config.RESOURCE_SUFFIX, ""));
                    saveLoginDetails(username, password);
                    createChatListener();
                    EventBus.getDefault().post(new MessageEvent(true));
                } else {
                    MainActivity.instance().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.instance(), "Unable to log in", Toast.LENGTH_LONG).show();
                        }
                    });
                    con.disconnect();
                }
            }
        }.execute();
    }

    public void createVCard(String username, AbstractXMPPConnection con) {
        VCard vCard = new VCard();
        VCardManager vCardManager = VCardManager.getInstanceFor(con);
        vCard.setField("fullname", username);
        vCard.setField("sharecount", "0");
        try {
            vCardManager.saveVCard(vCard);
            if(Config.logginOn) {
                Log.e(TAG, "VCard saved");
            }
        } catch (Exception e) {
        }
    }

    public void createPacketListener() {
        // Clearing the list before anything comes in
        FriendRequestList.friendRequestList.clear();

        final Roster roster = Roster.getInstanceFor(connection);
        roster.addRosterListener(new RosterListener() {
            @Override
            public void entriesAdded(Collection<String> addresses) {
                if(Config.logginOn) {
                    Log.e(TAG, "Entry added");
                }
            }

            @Override
            public void entriesUpdated(Collection<String> addresses) {
                if(Config.logginOn) {
                    Log.e(TAG, "Entry updated");
                }
            }

            @Override
            public void entriesDeleted(Collection<String> addresses) {
                if(Config.logginOn) {
                    Log.e(TAG, "Entry deleted");
                }
            }

            @Override
            public void presenceChanged(Presence presence) {
                if(Config.logginOn) {
                    Log.e(TAG, presence.getFrom() + " presence changed: " + presence.getStatus() + ", " + presence.getMode());
                    Log.e(TAG, "Available? " + presence.isAvailable());
                }
            }
        });

        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);

        connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processPacket(Stanza packet) throws SmackException.NotConnectedException {

                Presence presence = (Presence) packet;

                final String fromAddress = presence.getFrom();
                final RosterEntry newEntry = roster.getEntry(fromAddress.toLowerCase());

                if (Config.logginOn) {
                    Log.e(TAG, "Received: " + fromAddress + " and " + fromAddress.toLowerCase());
                }

                if (presence.getType() == Presence.Type.subscribe) {
                    if (Config.logginOn) {
                        Log.e(TAG, "Subscribe type from: " + packet.getFrom() + " or " + presence.getFrom());
                    }


                    if (newEntry == null) {
                        FriendRequestList.friendRequestList.add(fromAddress);

                        createNotification("You have a new friend request!", getBaseContext());

                        if(MainActivity.instance() != null) {

                            MainActivity.instance().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MainScreen.instance().updateRequestCount();
                                    Toast.makeText(MainActivity.instance(), "You have received a friend request", Toast.LENGTH_LONG).show();

                                    if (MainActivity.instance().getSupportFragmentManager().findFragmentByTag("maintabcontainer") != null
                                            && MainActivity.instance().getSupportFragmentManager().findFragmentByTag("maintabcontainer").isVisible()) {
                                        FriendRequestList.instance().adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }

                        if (Config.logginOn) {
                            Log.e(TAG, fromAddress + " added to friend request list");
                        }
                    } else {
                        // Send subscribed packet back
                        sendSubscribedPacket(fromAddress);
                    }
                } else if (presence.getType() == Presence.Type.subscribed) {
                    if (Config.logginOn) {
                        Log.e(TAG, "SUBSCRIBED packet from " + packet.getFrom());
                    }

                    if (FriendRequestList.instance() != null && FriendRequestList.instance().isVisible()) {
                        MainActivity.instance().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FriendRequestList.instance().removeAndUpdate(fromAddress);
                            }
                        });
                    } else {
                        if (FriendRequestList.friendRequestList.contains(fromAddress)) {
                            FriendRequestList.friendRequestList.remove(fromAddress);
                        }
                    }
                } else if (presence.getType() == Presence.Type.unsubscribe) {
                    if (Config.logginOn) {
                        Log.e(TAG, "unsubscribe packet received");
                    }

                    try {
                        roster.removeEntry(newEntry);
                        if (Config.logginOn) {
                            Log.e(TAG, "removed entry");
                        }

                    } catch (Exception e) {

                    }
                } else if (presence.getType() == Presence.Type.unsubscribed) {
                    if (Config.logginOn) {
                        Log.e(TAG, "unsubscribeD packet received");
                    }

                    try {
                        roster.removeEntry(newEntry);
                        if (Config.logginOn) {
                            Log.e(TAG, "removed entry");
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }, new StanzaTypeFilter(Presence.class));
    }

    public void sendSubscribedPacket(String username) {
        Presence response = new Presence(Presence.Type.subscribed);
        response.setTo(username);

        if(Config.logginOn) {
            Log.e(TAG, "Sent response to: " + username);
        }

        try {
            connection.sendStanza(response);
        } catch (SmackException.NotConnectedException e) {

        }
    }

    NotificationManager notificationManager;
    int notifID = 33;
    public void createNotification(String message, Context context) {
        NotificationCompat.Builder notificBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(Config.APP_NAME)
                .setContentText(message)
                .setTicker("Friend Request")
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

    public void createDatabase(final String sender) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mDatabase.getReadableDatabase().execSQL("CREATE TABLE IF NOT EXISTS c" +
                        sender + " (id integer primary key, sender text, messageurl text, picture text, date text);");
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
//                mDatabase.close();
            }
        }.execute();
    }

    public void disconnect() {
        if(connection != null && connection.isConnected()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    connection.disconnect();
                    if(Config.logginOn) {
                        Log.e(TAG, "Connection disconnected");
                    }
                    return null;
                }
            }.execute();
        }
    }
}
