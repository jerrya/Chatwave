package app.chatwave.me.FriendsPackage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.List;

import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;

public class FriendRequestAdapter extends ArrayAdapter<String>{

    protected static final String TAG = "FriendRequestAdapter";

    AdapterInterface buttonListener;
    public interface AdapterInterface {
        public void buttonClicked(String username, boolean accepted);
    }

    private static class ViewHolder {
        TextView requestName;
        ImageButton acceptRequestBtn;
        ImageButton denyRequestBtn;
    }

    public FriendRequestAdapter(Context context, List<String> requestList, AdapterInterface buttonListener) {
        super(context, 0, requestList);
        this.buttonListener = buttonListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String address = getItem(position);

        ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.friend_request_item, parent, false);
            viewHolder.requestName = (TextView) convertView.findViewById(R.id.requestName);
            viewHolder.acceptRequestBtn = (ImageButton) convertView.findViewById(R.id.acceptRequestBtn);
            viewHolder.denyRequestBtn = (ImageButton) convertView.findViewById(R.id.denyRequestBtn);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String humanAddress = address.replace(Config.RESOURCE_SUFFIX, "");

        viewHolder.requestName.setText(humanAddress);

        viewHolder.acceptRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonListener.buttonClicked(address, true);
            }
        });

        viewHolder.denyRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonListener.buttonClicked(address, false);
            }
        });

        return convertView;
    }
}

