package app.chatwave.me.CreateMessagePackage;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import app.chatwave.me.EventPackage.PictureAddedEvent;
import app.chatwave.me.MainActivity;
import app.chatwave.me.MessagePackage.SoundAdapter;
import app.chatwave.me.MessagePackage.SoundItem;
import app.chatwave.me.R;
import app.chatwave.me.UtilityPackage.Config;
import app.chatwave.me.UtilityPackage.SoundList;
import app.chatwave.me.UtilityPackage.UtilityClass;
import de.greenrobot.event.EventBus;

public class AddPicture extends Fragment {

    protected static final String TAG = "AddPicture";

    private ImageView picture;
    private ImageButton addTextRegular, picRegular, picCamera, picSounds;
    private GridView addpic_gridview;

    private SoundItem soundItem;
    private SoundAdapter soundAdapter;
    private ArrayList<SoundItem> soundList = new ArrayList<>();
    private MediaPlayer mediaPlayer;

    private SlidingUpPanelLayout mSlidingPanel;

    private ProgressDialog progressDialog;
    private String soundName, soundUrl;
    private String imgPath;

    public static Bitmap correctedImage;

    private static int RESULT_LOAD_IMAGE = 1;
    private Canvas canvas;
    private Bitmap bitmapToSend;

    private String trueId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_picture_layout, container, false);

        mSlidingPanel = (SlidingUpPanelLayout) view.findViewById(R.id.addpic_sliding_layout);
        mSlidingPanel.setTouchEnabled(false);

        ImageButton addpic_panel_exit = (ImageButton) view.findViewById(R.id.addpic_panel_exit);
        addpic_panel_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        });

        addpic_gridview = (GridView) view.findViewById(R.id.addpic_gridview);

        final ImageButton picExit = (ImageButton) view.findViewById(R.id.picExit);
        picExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        picCamera = (ImageButton) view.findViewById(R.id.picCamera);
        picCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCamera();
            }
        });

        picSounds = (ImageButton) view.findViewById(R.id.picSounds);
        picSounds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });

        picRegular = (ImageButton) view.findViewById(R.id.picRegular);
        picRegular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadImagefromGallery();
            }
        });

        addTextRegular = (ImageButton) view.findViewById(R.id.addTextRegular);
        addTextRegular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (correctedImage != null) {
                    addTextDialog();
                } else {
                    Toast.makeText(getActivity(), "Please add an image", Toast.LENGTH_LONG).show();
                }
            }
        });

        BootstrapButton picRegularNext = (BootstrapButton) view.findViewById(R.id.picRegularNext);
        picRegularNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (correctedImage != null) {
                if (MainActivity.correctedImage != null) {
                    if(soundName != null && soundUrl != null) {
                        EventBus.getDefault().post(new PictureAddedEvent(soundUrl, soundName));
                    } else {
                        Toast.makeText(getActivity(), "Please add a sound", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), "Please add an image", Toast.LENGTH_LONG).show();
                }
            }
        });

        picture = (ImageView) view.findViewById(R.id.addImage);
        MainActivity.correctedImage = null;
        /*
        Picasso.with(getActivity()).load("http://getcampuschat.com/chatwave/8ZK0CFW0.jpg").into(picture);
        picture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float[] coords = getPointerCoords(picture, event);
                float x = coords[0];
                float y = coords[1];

                Log.e(TAG, "Coords x: " + x + " y: " + y);

                Matrix imageMatrix = picture.getImageMatrix();


                DisplayMetrics metrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                float logicalDensity = metrics.density;

                float newX = 135.6567f/logicalDensity;
                float newY = 188.87271f/logicalDensity;

                float resizedX = 67.82835f*logicalDensity;
                float resizedY = 94.436356f*logicalDensity;

//                Log.e(TAG, "New coords x: " + 135.6567f/density + " y: " + 188.87271f/density);
                Log.e(TAG, "New coords x: " + newX + " y: " + newY);
                correctedImage = ((BitmapDrawable)picture.getDrawable()).getBitmap();
                tagBitmap(correctedImage, resizedX, resizedY);
//                tagBitmap(correctedImage, otherX, otherY);
                return true;
            }
        });
        */

        return view;
    }

    public float[] getPointerCoords(ImageView view, MotionEvent e)
    {
        final int index = e.getActionIndex();
        final float[] coords = new float[] { e.getX(index), e.getY(index) };
        Matrix matrix = new Matrix();
        view.getImageMatrix().invert(matrix);
        matrix.postTranslate(view.getScrollX(), view.getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }

    public float[] translateCoords(ImageView view, MotionEvent e, float x, float y)
    {
        final int index = e.getActionIndex();
        final float[] coords = new float[] { x, y };
        Matrix matrix = new Matrix();
        view.getImageMatrix().invert(matrix);
        matrix.postTranslate(view.getScrollX(), view.getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }

    public void tagBitmap(Bitmap bitmap, float x, float y) {
        // Clears any previous drawings
        if(canvas != null) {
//            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }

        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(bitmap);

        // START
        Paint myPaint = new Paint();
        myPaint.setColor(getResources().getColor(R.color.bg_trans));
        myPaint.setStrokeWidth(10);
        canvas.drawCircle(x, y, 50, myPaint);
        // END

        bitmapToSend = bitmap;
        MainActivity.correctedImage = bitmapToSend;

        picture.setImageBitmap(bitmap);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
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

    public void setUpSoundList() {
        for(int i = 0; i< SoundList.mSoundList.length; i++) {
            soundList.add(new SoundItem(SoundList.mSoundList[i][0], SoundList.mSoundList[i][1]));
        }

        if(getActivity() != null) {
            soundAdapter = new SoundAdapter(getActivity(), soundList);
            addpic_gridview.setAdapter(soundAdapter);
            addpic_gridview.setClickable(true);
            addpic_gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    soundItem = soundAdapter.getItem(position);

                    soundName = soundItem.name;
                    soundUrl = soundItem.url;

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
        setUpSoundList();
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

    public void loadCamera() {
        final Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, RESULT_LOAD_IMAGE);
    }

    public void loadImagefromGallery() {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
    }

    int orientation;
    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMAGE && resultCode == getActivity().RESULT_OK && null != data) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        // Get the Image from data
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = { MediaStore.Images.Media.DATA };

                        // Get the cursor
                        Cursor cursor = getActivity().getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);
                        // Move to first row
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        imgPath = cursor.getString(columnIndex);
                        cursor.close();

                        // For larger files, we fix orientation
                        File pictureFile = new File(imgPath);
                        try {
                            orientation = resolveBitmapOrientation(pictureFile);
                        } catch (IOException e) {
//                            e.printStackTrace();
                        }

                        Bitmap resizedBitmap = resizeBitmap(640, imgPath);
                        correctedImage = applyOrientation(resizedBitmap, orientation);

                        MainActivity.correctedImage = correctedImage;

                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        picture.setImageBitmap(correctedImage);

                        // Put file name in Async Http Post Param which will used in Php web app
                        // use fileName for path or create own
                        trueId = UtilityClass.randomString(8);
//                        randomName = trueId + ".jpg";
                    }
                }.execute();

            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }
    }

    public Bitmap resizeBitmap(int desiredWidth, String STRING_PATH_TO_FILE) {
        // Get the source image's dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(STRING_PATH_TO_FILE, options);

        int srcWidth = options.outWidth;
        int srcHeight = options.outHeight;

        // Only scale if the source is big enough. This code is just trying to fit a image into a certain width.
        if(desiredWidth > srcWidth) {
            desiredWidth = srcWidth;
        }

        // Calculate the correct inSampleSize/scale value. This helps reduce memory use. It should be a power of 2
        // from: http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue/823966#823966
        int inSampleSize = 1;
        while(srcWidth / 2 > desiredWidth){
            srcWidth /= 2;
            srcHeight /= 2;
            inSampleSize *= 2;
        }

        float desiredScale = (float) desiredWidth / srcWidth;

        // Decode with inSampleSize
        options.inJustDecodeBounds = false;
        options.inDither = false;
        options.inSampleSize = inSampleSize;
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap sampledSrcBitmap = BitmapFactory.decodeFile(STRING_PATH_TO_FILE, options);

        // Resize
        Matrix matrix = new Matrix();
        matrix.postScale(desiredScale, desiredScale);
        Bitmap scaledBitmap = Bitmap.createBitmap(sampledSrcBitmap, 0, 0, sampledSrcBitmap.getWidth(), sampledSrcBitmap.getHeight(), matrix, true);
        sampledSrcBitmap = null;
        return scaledBitmap;
    }

    public void addTextDialog() {
        new MaterialDialog.Builder(getActivity())
                .title("Picture caption")
                .titleColorRes(R.color.bootstra_blue)
                .content("Enter text")
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .positiveColorRes(R.color.bootstra_blue)
                .input("Name", null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        // Do something
                        if (input.toString().length() > 0) {
                            String message = input.toString();
                            addBitmapText2(message, correctedImage);
                        }
                    }
                })
                .negativeText("CANCEL")
                .negativeColorRes(R.color.bootstra_blue)
                .show();
    }

    private int resolveBitmapOrientation(File bitmapFile) throws IOException {
        ExifInterface exif = null;
        exif = new ExifInterface(bitmapFile.getAbsolutePath());
        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    }

    private Bitmap applyOrientation(Bitmap bitmap, int orientation) {
        int rotate = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
            default:
                return bitmap;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix mtx = new Matrix();
        mtx.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public void addBitmapText2(String text, Bitmap bitmap) {
        // Clears any previous drawings
        if(canvas != null) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        }

        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        canvas = new Canvas(bitmap);

        int bWidth = bitmap.getWidth();
        int bHeight = (bitmap.getHeight()/4) * 3;

        // START
        TextView textView = new TextView(getActivity());
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(getResources().getColor(R.color.bg_trans));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20);
        textView.setTextColor(Color.WHITE);
        textView.setText(text);

        Rect bounds = new Rect();
        textView.getPaint().getTextBounds(textView.getText().toString(), 0, textView.getText().toString().length(), bounds);

        double doubleBWidth = bitmap.getWidth();
        int textWidth = (int) (Math.ceil(bounds.width()/doubleBWidth) * 30);
        textView.layout(0, 0, bitmap.getWidth(), textWidth);

        textView.setDrawingCacheEnabled(true);
        canvas.drawBitmap(textView.getDrawingCache(), 0, bHeight, null); //text box top left position 50,50
        // END

        bitmapToSend = bitmap;
        MainActivity.correctedImage = bitmapToSend;

        picture.setImageBitmap(bitmap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
