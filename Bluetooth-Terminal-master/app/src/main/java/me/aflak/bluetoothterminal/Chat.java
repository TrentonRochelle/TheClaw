package me.aflak.bluetoothterminal;
//package com.github.niqdev.ipcam;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.TimeUnit;
import android.hardware.SensorEventListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.graphics.Color;
import android.support.v4.view.ViewPager.DecorView;

import android.widget.Button;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
//import yjkim.mjpegviewer.MjpegView;



import me.aflak.bluetooth.Bluetooth;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import 	android.util.Log;

import static java.lang.Math.round;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback, SensorEventListener, View.OnClickListener {
    private String name;
    private Bluetooth b;
//    private EditText message;
//    private Button send;
//    private TextView text;
//    private ScrollView scrollView;
//    private static SeekBar seek_bar;
    private static final int TIMEOUT = 5;
    public static final String PREF_FLIP_HORIZONTAL = "com.github.niqdev.ipcam.settings.SettingsActivity.FLIP_HORIZONTAL";
    public static final String PREF_FLIP_VERTICAL= "com.github.niqdev.ipcam.settings.SettingsActivity.FLIP_VERTICAL";

    private boolean registered=false;

    private TextView mTextViewAngleLeft;
    private TextView mTextViewStrengthLeft;
    private TextView mTextViewCoordinateLeft;

    private TextView mTextViewAngleRight;
    private TextView mTextViewStrengthRight;
    private TextView mTextViewCoordinateRight;

    private TextView mTextViewGyroLeft;

    private double waistVelocity = 0.0;
    private double shoulderVelocity = 0.0;
    private double wristElVelocity = 0.0;
    private double wristRotVelocity = 0.0;
    private double elbowVelocity = 0.0;
    private double clawVelocity = 0.0;

    private boolean waistLock = false;
    private boolean shoulderLock = false;
    private boolean wristElevationLock = false;
    private boolean wristRotationLock = false;
    private boolean elbowLock = false;
    private boolean clawLock = false;

    private JoystickView joystickRight;
    private JoystickView joystickLeft;

    final private int baud = 30;

    private Context mContext;

    private SensorManager mSensorManager;

    private MjpegView mjpegView;
    private String mIP;
    private boolean mRequestIP = false;
    private boolean loaded = false;
//    private MjpegView mv;

    private Button waistLockButton;
    private Button shoulderLockButton;
    private Button elbowLockButton;
    private Button wristElevationLockButton;
    private Button wristRotationLockButton;
    private Button clawLockButton;

    // Create the Handler object (on the main thread by default)
    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread

            // Repeat this the same runnable code block again another 2 seconds
            String waist_string = String.format("%.3f", waistVelocity).replaceAll("(\\.\\d+?)0*$", "$1");
            String shoulder_string = String.format("%.3f", shoulderVelocity).replaceAll("(\\.\\d+?)0*$", "$1");
            String elbow_string = String.format("%.3f", elbowVelocity).replaceAll("(\\.\\d+?)0*$", "$1");
            String wrist_el_string = String.format("%.3f", wristElVelocity).replaceAll("(\\.\\d+?)0*$", "$1");
            String wrist_rot_string = String.format("%.3f", wristRotVelocity).replaceAll("(\\.\\d+?)0*$", "$1");
            String claw_string = String.format("%.3f", clawVelocity).replaceAll("(\\.\\d+?)0*$", "$1");

            StringBuilder cmd_string = new StringBuilder();
            cmd_string.append("s");
            cmd_string.append(waist_string).append(":");
            cmd_string.append(shoulder_string).append(":");
            cmd_string.append(elbow_string).append(":");
            cmd_string.append(wrist_el_string).append(":");
            cmd_string.append(wrist_rot_string).append(":");

            cmd_string.append(claw_string).append("/");

            if(mRequestIP) {
                mRequestIP = false;
                Log.d("TIMER", "yunk");
                b.send("R");
            }

            if(b.isConnected()){
//                System.out.println("Connected");
                b.send(cmd_string.toString());
            }
//            System.out.println("yunk");
//            Log.d("Handlers", cmd_string.toString());
            handler.postDelayed(runnableCode, baud);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        waistLockButton = findViewById(R.id.waistLock);
        shoulderLockButton = findViewById(R.id.shoulderLock);
        elbowLockButton = findViewById(R.id.elbowLock);
        wristElevationLockButton = findViewById(R.id.wristElevationLock);
        wristRotationLockButton = findViewById(R.id.wristRotationLock);
        clawLockButton = findViewById(R.id.clawLock);
        waistLockButton.setOnClickListener(this);
        shoulderLockButton.setOnClickListener(this);
        elbowLockButton.setOnClickListener(this);
        wristElevationLockButton.setOnClickListener(this);
        wristRotationLockButton.setOnClickListener(this);
        clawLockButton.setOnClickListener(this);

        mjpegView = (MjpegView) findViewById(R.id.surface_view);

//        mjpegView.setScaleType(ScaleType.FIT_XY);

        System.out.println("before start ##################");
//        mv.Start("http://10.17.2.33:8080/?action=stream");
//        loadIpCam();
        System.out.println("after start ##################");

//        View view = (getLayoutInflater().inflate(R.layout.activit, null);
//        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id. Main);
//        view.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        relativeLayout.addView(view);


//        text = (TextView)findViewById(R.id.text);
//        message = (EditText)findViewById(R.id.message);
//        send = (Button)findViewById(R.id.send);
//        scrollView = (ScrollView) findViewById(R.id.scrollView);

//        text.setMovementMethod(new ScrollingMovementMethod());
//        send.setEnabled(false);


        // ------------------------------  SENSOR SETUP ----------------------------- //
        // ##########################################################################//
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Sensor gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (gyroscope != null) {
            mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // ###########################################################################//


        // -------------------------  BLUETOOTH/TIMER SETUP ------------------------- //
        // ###########################################################################//
        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");
        name = b.getPairedDevices().get(pos).getName();

//        Display("Connecting...");
        b.connectToDevice(b.getPairedDevices().get(pos));

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered=true;

//        while(!b.isConnected()){
//            System.out.println("wowowowoowwow");
//        }
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);
        // ###########################################################################//

//        send.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String msg = message.getText().toString();
//                message.setText("");
//                b.send(msg);
//                Display("You: "+msg);
//            }
//        });





        // ----------------------------  JOYSTICK SETUP ----------------------------- //
        // ###########################################################################//
        mTextViewCoordinateLeft = (TextView) findViewById(R.id.textView_coordinate_left);

        joystickLeft = (JoystickView) findViewById(R.id.joystickView_left);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                int bandaidElbow = joystickLeft.getNormalizedX()==52 ? 50 : joystickLeft.getNormalizedX();
                elbowVelocity = (-1.0 + bandaidElbow/50.0) * 1.414;
                elbowVelocity = elbowVelocity > 1 ? 1 : elbowVelocity;
                elbowVelocity = elbowVelocity < -1 ? -1 : elbowVelocity;
                elbowVelocity = .005*(elbowVelocity*elbowVelocity*elbowVelocity);

                int bandaidClaw = joystickLeft.getNormalizedY()==52 ? 50 : joystickLeft.getNormalizedY();
                clawVelocity = (1.0 - bandaidClaw/50.0) * 1.414;
                clawVelocity = clawVelocity > 1 ? 1 : clawVelocity;
                clawVelocity = clawVelocity < -1 ? -1 : clawVelocity;
                clawVelocity = .005*(clawVelocity*clawVelocity*clawVelocity);

                mTextViewCoordinateLeft.setText(
                        String.format("elbow=%.3f: claw=%.3f",
                                elbowVelocity,clawVelocity)
                );
            }


        });


        mTextViewCoordinateRight = (TextView) findViewById(R.id.textView_coordinate_right);

        joystickRight = (JoystickView) findViewById(R.id.joystickView_right);
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onMove(int angle, int strength) {

                int bandaidWristEl = joystickRight.getNormalizedX()==52 ? 50 : joystickRight.getNormalizedX();
                wristElVelocity = (-1.0 + bandaidWristEl/50.0) * 1.414;
                wristElVelocity = wristElVelocity > 1 ? 1 : wristElVelocity;
                wristElVelocity = wristElVelocity < -1 ? -1 : wristElVelocity;
                wristElVelocity = .005*(wristElVelocity*wristElVelocity*wristElVelocity);

                int bandaidWristRot = joystickRight.getNormalizedY()==52 ? 50 : joystickRight.getNormalizedY();
                wristRotVelocity = (1.0 - bandaidWristRot/50.0) * 1.414;
                wristRotVelocity = wristRotVelocity > 1 ? 1 : wristRotVelocity;
                wristRotVelocity = wristRotVelocity < -1 ? -1 : wristRotVelocity;
                wristRotVelocity = .005*(wristRotVelocity*wristRotVelocity*wristRotVelocity);

                mTextViewCoordinateRight.setText(
                        String.format("wrist_el=%.3f: wrist_rot=%.3f",
                                wristElVelocity,wristRotVelocity)
                );
            }
        });

        joystickRight.setEnabled(false);
        joystickLeft.setEnabled(false);
        // ###########################################################################//



    }

    @Override
    public void onResume(){
        super.onResume();
        loadIpCam();

    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId()) {
            case R.id.waistLock:
                // Do something
                waistLock = !waistLock;
                if(waistLock){
                    waistLockButton.setBackgroundResource(R.drawable.button_locked);
                }
                else{
                    waistLockButton.setBackgroundResource(R.drawable.button_unlocked);
                }
                break;
            case R.id.shoulderLock:
                // Do something
                shoulderLock = !shoulderLock;
                if(shoulderLock){
                    shoulderLockButton.setBackgroundResource(R.drawable.button_locked);
                }
                else{
                    shoulderLockButton.setBackgroundResource(R.drawable.button_unlocked);
                }
                break;
            case R.id.elbowLock:
                // Do something
                elbowLock = !elbowLock;
                if(elbowLock){
                    elbowLockButton.setBackgroundResource(R.drawable.button_locked);
                }
                else{
                    elbowLockButton.setBackgroundResource(R.drawable.button_unlocked);
                }
                break;
            case R.id.wristElevationLock:
                // Do something
                wristElevationLock = !wristElevationLock;
                if(wristElevationLock){
                    wristElevationLockButton.setBackgroundResource(R.drawable.button_locked);
                }
                else{
                    wristElevationLockButton.setBackgroundResource(R.drawable.button_unlocked);
                }
                break;
            case R.id.wristRotationLock:
                // Do something
                wristRotationLock = !wristRotationLock;
                if(wristRotationLock){
                    wristRotationLockButton.setBackgroundResource(R.drawable.button_locked);
                }
                else{
                    wristRotationLockButton.setBackgroundResource(R.drawable.button_unlocked);
                }
                break;
            case R.id.clawLock:
                // Do something
                clawLock = !clawLock;
                if(clawLock){
                    clawLockButton.setBackgroundResource(R.drawable.button_locked);
                }
                else{
                    clawLockButton.setBackgroundResource(R.drawable.button_unlocked);
                }
                break;
            default:
                break;
        }
    }

    //
//    final Handler MjpegViewHandler = new Handler(){
//        @Override
//        public void handleMessage(Message msg){
//            Log.d("State : ", msg.obj.toString());
//
//            switch (msg.obj.toString()){
//                case "DISCONNECTED" :
//                    // TODO : When video stream disconnected
//                    break;
//                case "CONNECTION_PROGRESS" :
//                    // TODO : When connection progress
//                    break;
//                case "CONNECTED" :
//                    // TODO : When video streaming connected
//                    break;
//                case "CONNECTION_ERROR" :
//                    // TODO : When connection error
//                    break;
//                case "STOPPING_PROGRESS" :
//                    // TODO : When MjpegViewer is in stopping progress
//                    break;
//            }
//
//        }
//    };






    //#########################################################################################
    // TRY 1
    private SharedPreferences getSharedPreferences() {
        return PreferenceManager
                .getDefaultSharedPreferences(this);
    }

    private String getPreference(String key) {
        return getSharedPreferences()
                .getString(key, "");
    }

    private Boolean getBooleanPreference(String key) {
        return getSharedPreferences()
                .getBoolean(key, false);
    }

    private DisplayMode calculateDisplayMode() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_LANDSCAPE ?
                DisplayMode.FULLSCREEN : DisplayMode.BEST_FIT;
    }


    private void loadIpCam() {
        StringBuilder url = new StringBuilder();
        url.append("http://");
        url.append(mIP);
        url.append(":8080/?action=stream");

        Mjpeg.newInstance()
//                .credential(getPreference(PREF_AUTH_USERNAME), getPreference(PREF_AUTH_PASSWORD))
//                .open("http://10.17.2.33:8080/?action=stream", TIMEOUT)
//                .open("http://169.231.85.222:8080/?action=stream", TIMEOUT)

                .open(url.toString(),TIMEOUT)
                .subscribe(
                        inputStream -> {
                            mjpegView.setSource(inputStream);
                            mjpegView.setDisplayMode(calculateDisplayMode());
//                            mjpegView.flipHorizontal(getBooleanPreference(PREF_FLIP_HORIZONTAL));
                            mjpegView.flipHorizontal(getBooleanPreference(PREF_FLIP_HORIZONTAL));
                            mjpegView.flipVertical(getBooleanPreference(PREF_FLIP_VERTICAL));
//                            mjpegView.setRotate(Float.parseFloat(getPreference(PREF_ROTATE_DEGREES)));
                            mjpegView.showFps(true);
                            mjpegView.setCustomBackgroundColor(Color.DKGRAY);
//                            mjpegView.setTransparentBackground();
                        },
                        throwable -> {
                            Log.e(getClass().getSimpleName(), "mjpeg error", throwable);
                            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
                        });

//        .subscribe(inputStream -> {
//                    mjpegView.setSource(inputStream);
//                    mjpegView.setDisplayMode(DisplayMode.BEST_FIT);
//                    mjpegView.showFps(true);
//                    mjpegView.setTransparentBackground();
//                });
    }


    //#########################################################################################
    // MATH FUNCTIONS

    private double smoothControlFunction(double x) {

        return (1.5*x);
    }

//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            hideSystemUI();
//        }
//    }
//
//    private void hideSystemUI() {
//        // Enables regular immersive mode.
//        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
//        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_IMMERSIVE
//                        // Set the content to appear under the system bars so that the
//                        // content doesn't resize when the system bars hide and show.
//                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        // Hide the nav bar and status bar
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
//    }
//
//    // Shows the system bars by removing all the flags
//    // except for the ones that make the content appear under the system bars.
//    private void showSystemUI() {
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
//    }
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            decorView.setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
//    }



    @Override
    public void onAccuracyChanged (Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged (SensorEvent event) {
        Sensor sensor = event.sensor;
        mTextViewGyroLeft = (TextView) findViewById(R.id.textView_gyros_left);

        if (sensor.getType() == Sensor.TYPE_GRAVITY) {

            waistVelocity = event.values[1]/10;
            shoulderVelocity = -event.values[0]/10;
//            waistVelocity = waistVelocity * .4;
            waistVelocity = .4*(waistVelocity*waistVelocity*waistVelocity*waistVelocity*waistVelocity);
            shoulderVelocity = .02*(shoulderVelocity*shoulderVelocity*shoulderVelocity);

            waistVelocity = waistVelocity > .04 ? .04 : waistVelocity;
            waistVelocity = waistVelocity < -.04 ? -.04 : waistVelocity;
            shoulderVelocity = shoulderVelocity > .005 ? .005 : shoulderVelocity;
            shoulderVelocity = shoulderVelocity < -.005 ? -.005 : shoulderVelocity;

            mTextViewGyroLeft.setText(
                    String.format("waist=%.3f: shoulder=%.3f",
                            waistVelocity,shoulderVelocity)
            );

//            System.out.println(stringBuilder.toString());
//            Log.d("On Gyro Change", stringBuilder.toString());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(registered) {
            unregisterReceiver(mReceiver);
            registered=false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.close:
                b.removeCommunicationCallback();
                b.disconnect();
                Intent intent = new Intent(this, Select.class);
                startActivity(intent);
                finish();
                return true;

            case R.id.rate:
                Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    public void Display(final String s){
//        this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                text.append(s + "\n");
//                scrollView.fullScroll(View.FOCUS_DOWN);
//            }
//        });
//    }

    @Override
    public void onConnect(BluetoothDevice device) {
//        Display("Connected to "+device.getName()+" - "+device.getAddress());
//        this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                send.setEnabled(true);
//            }
//        });
        joystickRight.setEnabled(true);
        joystickLeft.setEnabled(true);
        mRequestIP = true;
//        Toast.makeText(mContext, "Connected to The Claw", Toast.LENGTH_SHORT);
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Status = Connected to The Claw", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
//        Display("Disconnected!");
//        Display("Connecting again...");
        b.connectToDevice(device);
    }

    @Override
    public void onMessage(String message) {
        mIP = message;
        if(!loaded){
            Log.d("onMessage",message);
            loadIpCam();
            loaded=true;
        }
//        Display(name+": "+message);
//        Log.d("onMessage",message);
//        Toast.makeText(getApplicationContext(), "RECEIVED FROM PI", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String message) {
//        Display("Error: "+message);
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
//        Display("Error: "+message);
//        Display("Trying again in 3 sec.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler error_handler = new Handler();
                error_handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.connectToDevice(device);
                    }
                }, 2000);
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(Chat.this, Select.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }
    };

//    public void seekbarr(Bluetooth b){
//        seek_bar = (SeekBar)findViewById(R.id.seekBar);
//
//        seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//
//                int progress_value;
//                @Override
//                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                    progress_value = progress;
//                    progress_value = 600 + (progress_value*14);
//                    b.send("e"+String.valueOf(progress_value));
//                    Toast.makeText(Chat.this,"SeekBar value is: " + String.valueOf(progress_value),Toast.LENGTH_LONG).show();
//                }
//
//                @Override
//                public void onStartTrackingTouch(SeekBar seekBar) {
////                    Toast.makeText(Chat.this,"SeekBar Started Tracking",Toast.LENGTH_LONG).show();
//                }
//
//                @Override
//                public void onStopTrackingTouch(SeekBar seekBar) {
////                    Toast.makeText(Chat.this,"SeekBar Stopped Tracking",Toast.LENGTH_LONG).show();
//                }
//            }
//        );
//
//    }
}
