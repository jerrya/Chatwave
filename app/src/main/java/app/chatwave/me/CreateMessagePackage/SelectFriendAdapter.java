package app.chatwave.me.CreateMessagePackage;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.ArrayList;

import app.chatwave.me.FriendsPackage.FriendItem;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;

public class SelectFriendAdapter extends ArrayAdapter<FriendItem> {
    protected static final String TAG = "SelectFriendAdapter";

    private class ViewHolder {
        TextView selectFriendName;
        TextView selectFriendUsername;
        CheckBox selectFriendCheckbox;
    }

    public SelectFriendAdapter(Context context, ArrayList<FriendItem> friendList) {
        super(context, 0, friendList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final FriendItem friend = getItem(position);

        final ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.select_friend_item, parent, false);
            viewHolder.selectFriendCheckbox = (CheckBox) convertView.findViewById(R.id.selectFriendCheckbox);
            viewHolder.selectFriendName = (TextView) convertView.findViewById(R.id.selectFriendName);
            viewHolder.selectFriendUsername = (TextView) convertView.findViewById(R.id.selectFriendUsername);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (position % 2 == 1) {
            convertView.setBackgroundColor(getContext().getResources().getColor(R.color.bg_register));
        } else {
            convertView.setBackgroundColor(getContext().getResources().getColor(R.color.bg_register_light));
        }

        viewHolder.selectFriendName.setText(friend.name);
        viewHolder.selectFriendUsername.setText(friend.sender.replace(Config.RESOURCE_SUFFIX,""));

        if(friend.isSelected) {
            viewHolder.selectFriendCheckbox.setChecked(true);
        } else {
            viewHolder.selectFriendCheckbox.setChecked(false);
        }

        viewHolder.selectFriendCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewHolder.selectFriendCheckbox.isChecked()) {
                    SelectFriends.toSendFriends.add(friend.sender);
                    friend.isSelected = true;
                    if (Config.logginOn) {
                        Log.e(TAG, "Added: " + friend.sender);
                    }
                } else {
                    SelectFriends.toSendFriends.remove(friend.sender);
                    friend.isSelected = false;
                    if (Config.logginOn) {
                        Log.e(TAG, "Removed: " + friend.sender);
                    }
                }
            }
        });

        return convertView;
    }
}
