package app.chatwave.me.CreateMessagePackage;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.beardedhen.androidbootstrap.BootstrapButton;

import java.io.IOException;
import java.util.ArrayList;

import app.chatwave.me.EventPackage.SoundPickedEvent;
import app.chatwave.me.MessagePackage.SoundAdapter;
import app.chatwave.me.MessagePackage.SoundItem;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;
import app.chatwave.me.UtilityPackage.SoundList;
import de.greenrobot.event.EventBus;

public class SelectSound extends Fragment {

    protected static final String TAG = "SelectSound";
    private GridView gridView;
    private SoundItem soundItem;
    private SoundAdapter soundAdapter;
    private ArrayList<SoundItem> soundList = new ArrayList<>();
    private MediaPlayer mediaPlayer;

    private BootstrapButton createMessageNext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.select_sound_layout, container, false);

        gridView = (GridView) view.findViewById(R.id.gridview);

        ImageButton select_sound_panel_exit = (ImageButton) view.findViewById(R.id.select_sound_panel_exit);
        select_sound_panel_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        createMessageNext = (BootstrapButton) view.findViewById(R.id.createMessageNext);
        createMessageNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (soundItem != null) {
                    EventBus.getDefault().post(new SoundPickedEvent(soundItem.name, soundItem.url));
                }
            }
        });

        return view;
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUpSoundList();
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
//                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume_level, 0);

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

    @Override
    public void onStart() {
        super.onStart();
        mediaPlayer = new MediaPlayer();
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
}
