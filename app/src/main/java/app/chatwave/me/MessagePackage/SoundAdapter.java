package app.chatwave.me.MessagePackage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.ArrayList;

import app.chatwave.me.R;
import me.grantland.widget.AutofitTextView;

public class SoundAdapter extends ArrayAdapter<SoundItem>{

    private class ViewHolder {
        AutofitTextView image_view_sound_grid;
    }

    public SoundAdapter(Context context, ArrayList<SoundItem> soundList) {
        super(context, 0, soundList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SoundItem soundItem = getItem(position);

        final ViewHolder viewHolder;
        if(convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.sound_item, parent, false);
            viewHolder.image_view_sound_grid = (AutofitTextView) convertView.findViewById(R.id.image_view_sound_grid);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ColorGenerator generator = ColorGenerator.MATERIAL;
        final int color = generator.getColor(soundItem.name);
        TextDrawable drawable = TextDrawable.builder().buildRect("", color);
        viewHolder.image_view_sound_grid.setBackgroundDrawable(drawable);
        viewHolder.image_view_sound_grid.setText(soundItem.name);

        return convertView;
    }
}
