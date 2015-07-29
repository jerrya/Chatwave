package app.chatwave.me.MessagePackage;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;
import de.greenrobot.event.EventBus;

public class PictureMessage extends Fragment {

    protected static final String TAG = "PictureMessage";
    private MediaPlayer mediaPlayer;
    private String soundName, soundUrl, pictureUrl;
    private ImageView pictureMessage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.picture_message_layout, container, false);

        pictureMessage = (ImageView) view.findViewById(R.id.pictureMessage);
        pictureMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isVolumeOn()) {
                    playAudio(Config.AUDIO_URL_PREFIX + soundUrl);
                } else {
                    checkVolume();
                }
            }
        });

        ImageButton pictureMessageBack = (ImageButton) view.findViewById(R.id.pictureMessageBack);
        pictureMessageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mediaPlayer = new MediaPlayer();

        Bundle args = getArguments();

        if(args != null) {
            soundName = args.getString("soundname");
            soundUrl = args.getString("soundurl");
            pictureUrl = args.getString("pictureurl");

            Callback mCallBack = new Callback() {
                @Override
                public void onSuccess() {
                    if (isVolumeOn()) {
                        playAudio(Config.AUDIO_URL_PREFIX + soundUrl);
                    } else {
                        checkVolume();
                    }
                }

                @Override
                public void onError() {

                }
            };

            Picasso.with(getActivity()).load(Config.PICTURE_URL_PREFIX + pictureUrl + ".jpg").into(pictureMessage, mCallBack);
        }

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
}
