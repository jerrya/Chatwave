package app.chatwave.me.FriendsPackage;

import android.content.Context;
import android.os.AsyncTask;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

import java.util.ArrayList;

import app.chatwave.me.ConnectionManager;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;

public class FriendsAdapter extends ArrayAdapter<FriendItem>{

    protected static final String TAG = "FriendsAdapter";

    private class ViewHolder {
        ImageView friendAvatar;
        ImageButton editFriend;
        TextView friendName;
        TextView friendUsername;
    }

    public FriendsAdapter(Context context, ArrayList<FriendItem> friendList) {
        super(context, 0, friendList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final FriendItem friend = getItem(position);


        ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.friend_item, parent, false);
//            viewHolder.friendAvatar = (ImageView) convertView.findViewById(R.id.friendAvatar);
            viewHolder.editFriend = (ImageButton) convertView.findViewById(R.id.editFriend);
            viewHolder.friendName = (TextView) convertView.findViewById(R.id.friendName);
            viewHolder.friendUsername = (TextView) convertView.findViewById(R.id.friendUsername);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.friendName.setText(friend.name.replace(Config.RESOURCE_SUFFIX, ""));
        viewHolder.friendUsername.setText(friend.sender.replace(Config.RESOURCE_SUFFIX, ""));

        ColorGenerator generator = ColorGenerator.MATERIAL;
        int count = getCount() - position;

        convertView.setBackgroundColor(generator.getColor("A" + count));

        viewHolder.editFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new MaterialDialog.Builder(getContext())
                        .title("Edit friend")
                        .titleColorRes(R.color.bootstra_blue)
                        .content("Enter new name")
                        .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                        .positiveColorRes(R.color.bootstra_blue)
                        .input("Name", null, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                // Do something
                                if (input.toString().length() > 0) {
                                    changeName(friend.sender, input.toString());
                                }
                            }
                        })
                        .inputMaxLengthRes(20, R.color.enter_button_bg_red)
                        .negativeText("CANCEL")
                        .negativeColorRes(R.color.bootstra_blue)
                        .show();
            }
        });

        return convertView;
    }

    boolean nameChanged = false;
    public void changeName(String username, final String newFullName) {
        Roster roster = Roster.getInstanceFor(ConnectionManager.connection);
        final RosterEntry friend = roster.getEntry(username);
        if(friend != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        friend.setName(newFullName);
                        nameChanged = true;
                    } catch (Exception e) {
                        if(Config.logginOn) {
                            Log.e(TAG, "Unable to change name: " + e.getMessage());
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    if(nameChanged) {
                        if(Config.logginOn) {
                            Log.e(TAG, "Name changed!");
                        }
                    } else {
                        if(Config.logginOn) {
                            Log.e(TAG, "Unable to change name!");
                        }
                    }
                }
            }.execute();
        }
    }
}
