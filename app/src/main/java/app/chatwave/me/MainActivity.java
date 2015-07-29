package app.chatwave.me;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.jivesoftware.smack.SmackConfiguration;

import java.util.ArrayList;

import app.chatwave.me.CreateMessagePackage.SelectFriends;
import app.chatwave.me.EventPackage.OpenPictureMessage;
import app.chatwave.me.EventPackage.PictureAddedEvent;
import app.chatwave.me.EventPackage.SoundPickedEvent;
import app.chatwave.me.MessagePackage.MessageItem;
import app.chatwave.me.MessagePackage.MessageList;
import app.chatwave.me.MessagePackage.PictureMessage;
import app.chatwave.me.UtilityPackage.Config;
import de.greenrobot.event.EventBus;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends FragmentActivity implements FragmentManager.OnBackStackChangedListener {

    private static final String TAG = "MainActivity";
    public static String username;
    public static ArrayList<MessageItem> messageList = new ArrayList<>();

    public static Bitmap correctedImage;

    private static MainActivity inst;
    public static MainActivity instance() {
        return inst;
    }

    @Override
    protected void onStart() {
        super.onStart();
        inst = this;
        EventBus.getDefault().register(this);
    }

    public void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()   // or .detectAll() for all detectable problems
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().hide();

        setContentView(R.layout.activity_main);

        if(Config.logginOn) {
            enableStrictMode();
            SmackConfiguration.DEBUG = true;
        }

        GoogleAnalytics.getInstance(this).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);

        Tracker t = ((MainApplication) getApplication()).getTracker(MainApplication.TrackerName.APP_TRACKER);
            t.setScreenName("MainActivity");
            t.send(new HitBuilders.AppViewBuilder().build());

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        shouldDisplayHomeUp();
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        if(findViewById(R.id.fragment_container) != null) {
            if(savedInstanceState != null) {
                return;
            }
            if(ConnectionManager.connection != null && ConnectionManager.connection.isConnected() && ConnectionManager.connection.isAuthenticated()) {
                MainScreen mainScreen = new MainScreen();
                mainScreen.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mainScreen, "mainscreen").commit();
            } else {
                LoginScreen loginScreen = new LoginScreen();
                loginScreen.setArguments(getIntent().getExtras());
                getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, loginScreen, "loginscreen").commit();
            }
        }

//        getContacts();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    public void getContacts() {
        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        while (phones.moveToNext())
        {
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            String formattedNumber = phoneNumber.trim().replace("-","");
            formattedNumber = formattedNumber.replace(" ","");
            formattedNumber = formattedNumber.replace("(","");
            formattedNumber = formattedNumber.replace(")","");

            if(Config.logginOn) {
                Log.e(TAG, "Name: " + name + " number: " + formattedNumber);
            }

        }
        phones.close();
    }

    public void sendTracker(String screenName, String category, String message, String label) {
        if(ConnectionManager.globalUsername != null) {
            label = ConnectionManager.globalUsername;
        }
        Tracker t = ((MainApplication) getApplication()).getTracker(MainApplication.TrackerName.APP_TRACKER);
        t.setScreenName(screenName);
        t.send(new HitBuilders.ScreenViewBuilder().build());
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(message)
                .setLabel(label)
                .build());
        t.setScreenName(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(PictureAddedEvent event) {
        SelectFriends selectFriends = new SelectFriends();

        Bundle args = new Bundle();
        args.putString("soundname", event.soundName);
        args.putString("soundurl", event.soundUrl);
        args.putBoolean("haspicture", true);

        selectFriends.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, selectFriends);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // Called in Android UI's main thread
    public void onEventMainThread(SoundPickedEvent event) {
        SelectFriends selectFriends = new SelectFriends();

        Bundle args = new Bundle();
        args.putString("soundname", event.soundName);
        args.putString("soundurl", event.soundUrl);

        selectFriends.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, selectFriends);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onEventMainThread(OpenPictureMessage event) {
        PictureMessage pictureMessage = new PictureMessage();

        Bundle args = new Bundle();
        args.putString("soundname", event.soundName);
        args.putString("soundurl", event.soundUrl);
        args.putString("pictureurl", event.pictureUrl);

        pictureMessage.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, pictureMessage);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onBackStackChanged() {
        if(MessageList.mSlidingPanel != null) {
            MessageList.mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp(){
        //Enable Up button only  if there are entries in the back stack
        boolean canback = getSupportFragmentManager().getBackStackEntryCount()>0;
        if(getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(canback);
        }
    }

    @Override
    public boolean onNavigateUp() {
        //This method is called when the up button is pressed. Just the pop back stack.
        getSupportFragmentManager().popBackStack();
        return true;
    }
}
