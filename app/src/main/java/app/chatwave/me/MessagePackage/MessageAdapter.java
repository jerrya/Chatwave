package app.chatwave.me.MessagePackage;

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

import java.util.ArrayList;

import app.chatwave.me.EventPackage.MessageEvent;
import app.chatwave.me.EventPackage.MessageReplyEvent;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.UtilityClass;
import de.greenrobot.event.EventBus;

public class MessageAdapter extends ArrayAdapter<MessageItem>{

    private static class ViewHolder {
        ImageButton messageReply;
        TextView messageSender;
        TextView messageDate;
    }

    public MessageAdapter(Context context, ArrayList<MessageItem> messageItemList) {
        super(context, 0, messageItemList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final MessageItem messageItem = getItem(position);

        ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.message_item, parent, false);
            viewHolder.messageSender = (TextView) convertView.findViewById(R.id.messageSender);
            viewHolder.messageDate = (TextView) convertView.findViewById(R.id.messageDate);
            viewHolder.messageReply = (ImageButton) convertView.findViewById(R.id.messageReply);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ColorGenerator generator = ColorGenerator.MATERIAL;
        String timeAgo = UtilityClass.getTimeAgo(UtilityClass.getDateInMillis(messageItem.date));

        int count = getCount() - position;

        convertView.setBackgroundColor(generator.getColor("A" + count));

        if(messageItem.messageUrl.length() > 0) {
            viewHolder.messageSender.setText(messageItem.name);
            viewHolder.messageDate.setText(timeAgo);
        } else {
            viewHolder.messageSender.setText("");
            viewHolder.messageDate.setText("");
        }

        viewHolder.messageReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(new MessageReplyEvent(messageItem.sender));
            }
        });

        return convertView;
    }
}
