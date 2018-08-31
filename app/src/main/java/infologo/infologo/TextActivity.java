package infologo.infologo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

public class TextActivity extends AppCompatActivity implements Serializable, MediaController.MediaPlayerControl {
    static String logoName = "";
    static ArrayList<String> logoDataType = new ArrayList<>();
    static ArrayList<String> logoDataContent = new ArrayList<>();
    static String logoAudioDir = "";
    private MediaPlayerService player;
    boolean serviceBound = false;
    private MusicController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_text);
        ((TextView)findViewById(R.id.logoName)).setText(logoName);
        setDataTable();
        Toolbar toolbar = findViewById(R.id.toolbarText);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        LinearLayout layout = findViewById(R.id.textLayout);

        ViewTreeObserver vto = layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new  ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //Call your controller set-up now that the layout is loaded
                setController();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_text, menu);
        return true;
    }

    public void onBackAction(MenuItem mi) {
        Intent intent = new Intent(this, Main_window.class);
        startActivity(intent);
    }

    private void setDataTable() {
        ((TextView)findViewById(R.id.dataType1)).setText(logoDataType.get(0));
        ((TextView)findViewById(R.id.dataContent1)).setText(logoDataContent.get(0));
        TableLayout table = (findViewById(R.id.table));
        for(int i=1; i<logoDataType.size(); i++)
            table.addView(newRow(i));
    }

    private TableRow newRow(int number) {
        TableRow row = new TableRow(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        row.setLayoutParams(params);
        row.setBackgroundResource(R.drawable.border_bottom);

        TextView typeView = new TextView(this);
        params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.40f);
        params.setMargins(DipToPx(3), DipToPx(1), DipToPx(3), DipToPx(1));
        typeView.setLayoutParams(params);
        typeView.setGravity(Gravity.START);
        typeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        typeView.setSingleLine(false);
        typeView.setTypeface(typeView.getTypeface(), Typeface.BOLD);
        typeView.setText(logoDataType.get(number));
        row.addView(typeView);

        TextView contentView = new TextView(this);
        params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 0.60f);
        params.setMargins(DipToPx(3), DipToPx(1), DipToPx(3), DipToPx(1));
        contentView.setLayoutParams(params);
        contentView.setGravity(Gravity.END);
        contentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        contentView.setSingleLine(false);
        contentView.setText(logoDataContent.get(number));
        row.addView(contentView);

        return row;
    }

    private int DipToPx(int value) {
        return (int)(value * getResources().getDisplayMetrics().density);
    }

    /****************************Audio code****************************/

    @Override
    protected void onStart() {
        super.onStart();
        playAudio(TextActivity.logoAudioDir);
    }

    // Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void playAudio(String media) {
        // Check if service is active
        if(!serviceBound) {
            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            playerIntent.putExtra("media", media);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(serviceBound) {
            unbindService(serviceConnection);
            // Service is active
            player.stopSelf();
        }
    }

    private void setController(){
        //set the controller up
        controller = new MusicController(this);
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.textLayout));
        controller.setEnabled(true);
        controller.show();
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        player.mediaPlayer.seekTo(pos);
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public void start() {
        player.startAudio();
    }

    @Override
    public void pause() {
        player.pauseAudio();
    }

    @Override
    public int getCurrentPosition() {
        if(player!=null && player.mediaPlayer.isPlaying())
            return player.mediaPlayer.getCurrentPosition();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        return player!=null && player.mediaPlayer.isPlaying();
    }

    @Override
    public int getDuration() {
        if(player!=null && player.mediaPlayer.isPlaying())
            return player.mediaPlayer.getDuration();
        else return 0;
    }
}
