package app.chatwave.me;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import app.chatwave.me.CreateMessagePackage.AddPicture;
import app.chatwave.me.CreateMessagePackage.SelectSound;
import app.chatwave.me.FriendsPackage.AddFriend;
import app.chatwave.me.FriendsPackage.FriendRequestList;
import app.chatwave.me.FriendsPackage.FriendsList;
import app.chatwave.me.MessagePackage.MessageList;
import app.chatwave.me.UtilityPackage.UtilityClass;

public class MainScreen extends Fragment {

    protected static final String TAG = "MainScreen";

    private ImageView mainMessagesCount, mainRequestsCount;
    private Button mainMessages, mainFriends, mainRequests, mainAddFriend;
    private ImageButton mainLogout;
    private Button createPicture, createSound;

    private ColorGenerator generator = ColorGenerator.MATERIAL;
    private int color = generator.getColor("countcolors");

    private static MainScreen inst;
    public static MainScreen instance() {
        return inst;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_screen_layout, container, false);

        mainMessagesCount = (ImageView) view.findViewById(R.id.mainMessagesCount);
        mainRequestsCount = (ImageView) view.findViewById(R.id.mainRequestsCount);

        createPicture = (Button) view.findViewById(R.id.createPicture);
        createPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance().sendTracker("MainScreen", "Create pic button", "Clicked create pic button", "Button click");

                AddPicture addPicture = new AddPicture();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, addPicture);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        createSound = (Button) view.findViewById(R.id.createSound);
        createSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance().sendTracker("MainScreen", "Sound button", "Clicked sound button", "Button click");

                SelectSound selectSound = new SelectSound();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, selectSound);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });


        mainMessages = (Button) view.findViewById(R.id.mainMessages);
        mainMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance().sendTracker("MainScreen", "Messages button", "Clicked messages button", "Button click");

                MessageList messageList = new MessageList();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, messageList, "messagelist");
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        mainFriends = (Button) view.findViewById(R.id.mainFriends);
        mainFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance().sendTracker("MainScreen", "Friends button", "Clicked friends button", "Button click");
                FriendsList friendsList = new FriendsList();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, friendsList);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        mainRequests = (Button) view.findViewById(R.id.mainRequests);
        mainRequests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance().sendTracker("MainScreen", "Requests button", "Clicked requests button", "Button click");

                FriendRequestList friendRequestList = new FriendRequestList();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, friendRequestList);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        mainAddFriend = (Button) view.findViewById(R.id.mainAddFriend);
        mainAddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.instance().sendTracker("MainScreen", "Add friend button", "Clicked to add friend", "Button click");

                AddFriend addFriend = new AddFriend();
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, addFriend);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;

        UtilityClass.hideKeyboard(getActivity());

        // This method updates the textdrawable with new requests
        updateRequestCount();

        // This method updates the message count with a new message count
        updateMessageCount();
    }

    public void askLogout() {
        MaterialDialog.Builder materialDialog = new MaterialDialog.Builder(getActivity());

        materialDialog.title("Logout");
        materialDialog.content("Are you sure you want to logout? You won't be able to receive new message or friend request notifications.");
        materialDialog.titleColorRes(R.color.bootstra_blue);
        materialDialog.positiveText("OK");

        materialDialog.positiveColorRes(R.color.bootstra_blue);

        materialDialog.negativeText("CANCEL");
        materialDialog.negativeColorRes(R.color.bootstra_blue);

        materialDialog.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                super.onPositive(dialog);
                dialog.dismiss();
                MainActivity.instance().sendTracker("MainScreen", "Logout button", "Successfully logged out", "Button click");

                if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                } else {
                    LoginScreen loginScreen = new LoginScreen();
                    FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment_container, loginScreen, "loginscreen");
                    transaction.commit();
                }

                Intent mServiceIntent = new Intent(getActivity(), ConnectionManager.class);
                mServiceIntent.putExtra("event", 2);
                getActivity().startService(mServiceIntent);
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
                super.onNegative(dialog);
                dialog.dismiss();
                UtilityClass.hideKeyboard(getActivity());
            }
        });

        materialDialog.show();
    }

    public void updateRequestCount() {
        int requestSize = FriendRequestList.friendRequestList.size();
        if(requestSize > 0) {
            TextDrawable drawable = TextDrawable.builder().buildRound("" + requestSize, color);
            mainRequestsCount.setImageDrawable(drawable);
            mainRequestsCount.setVisibility(View.VISIBLE);
        } else {
            mainRequestsCount.setVisibility(View.GONE);
        }
    }

    public void updateMessageCount() {
        int requestSize = MessageList.newMessagesList.size();
        if(requestSize > 0) {
            TextDrawable drawable = TextDrawable.builder().buildRound("" + requestSize, color);
            mainMessagesCount.setImageDrawable(drawable);
            mainMessagesCount.setVisibility(View.VISIBLE);
        } else {
            mainMessagesCount.setVisibility(View.GONE);
        }
    }
}