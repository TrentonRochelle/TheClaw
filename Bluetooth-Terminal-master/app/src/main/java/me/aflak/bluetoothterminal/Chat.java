package me.aflak.bluetoothterminal;

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


import me.aflak.bluetooth.Bluetooth;
import io.github.controlwear.virtual.joystick.android.JoystickView;

import 	android.util.Log;

import static java.lang.Math.round;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback, SensorEventListener {
    private String name;
    private Bluetooth b;
//    private EditText message;
//    private Button send;
//    private TextView text;
//    private ScrollView scrollView;
//    private static SeekBar seek_bar;
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

    private JoystickView joystickRight;
    private JoystickView joystickLeft;

    final private int baud = 25;

    private Context mContext;

    private SensorManager mSensorManager;




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

            cmd_string.append(waist_string).append(":");
            cmd_string.append(shoulder_string).append(":");
            cmd_string.append(elbow_string).append(":");
            cmd_string.append(wrist_el_string).append(":");
            cmd_string.append(wrist_rot_string).append(":");
            cmd_string.append(claw_string).append("/");

            if(b.isConnected()){
                System.out.println("Connected");
                b.send(cmd_string.toString());
            }
            System.out.println("yunk");
            Log.d("Handlers", cmd_string.toString());
            handler.postDelayed(runnableCode, baud);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();


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
        mTextViewAngleLeft = (TextView) findViewById(R.id.textView_angle_left);
        mTextViewStrengthLeft = (TextView) findViewById(R.id.textView_strength_left);
        mTextViewCoordinateLeft = (TextView) findViewById(R.id.textView_coordinate_left);

        joystickLeft = (JoystickView) findViewById(R.id.joystickView_left);
        joystickLeft.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                mTextViewAngleLeft.setText(angle + "°");
                mTextViewStrengthLeft.setText(strength + "%");

                elbowVelocity = (-1.0 + joystickLeft.getNormalizedX()/50.0) * 1.414 / 100.0;
                elbowVelocity = elbowVelocity > 1 ? 1 : elbowVelocity;
                elbowVelocity = elbowVelocity < -1 ? -1 : elbowVelocity;

                clawVelocity = (1.0 - joystickLeft.getNormalizedY()/50.0) * 1.414 / 100.0;
                clawVelocity = clawVelocity > 1 ? 1 : clawVelocity;
                clawVelocity = clawVelocity < -1 ? -1 : clawVelocity;

                mTextViewCoordinateLeft.setText(
                        String.format("elbow=%.3f: claw=%.3f",
                                elbowVelocity,clawVelocity)
                );
            }


        });


        mTextViewAngleRight = (TextView) findViewById(R.id.textView_angle_right);
        mTextViewStrengthRight = (TextView) findViewById(R.id.textView_strength_right);
        mTextViewCoordinateRight = (TextView) findViewById(R.id.textView_coordinate_right);

        joystickRight = (JoystickView) findViewById(R.id.joystickView_right);
        joystickRight.setOnMoveListener(new JoystickView.OnMoveListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onMove(int angle, int strength) {
                mTextViewAngleRight.setText(angle + "°");
                mTextViewStrengthRight.setText(strength + "%");

                wristElVelocity = (-1.0 + joystickRight.getNormalizedX()/50.0) * 1.414 / 100.0;
                wristElVelocity = wristElVelocity > 1 ? 1 : wristElVelocity;
                wristElVelocity = wristElVelocity < -1 ? -1 : wristElVelocity;

                wristRotVelocity = (1.0 - joystickRight.getNormalizedY()/50.0) * 1.414 / 100.0;
                wristRotVelocity = wristRotVelocity > 1 ? 1 : wristRotVelocity;
                wristRotVelocity = wristRotVelocity < -1 ? -1 : wristRotVelocity;

                mTextViewCoordinateRight.setText(
                        String.format("wrist_el=%.3f: wrist_rot=%.3f",
                                wristRotVelocity,wristElVelocity)
                );
            }
        });

        joystickRight.setEnabled(false);
        joystickLeft.setEnabled(false);
        // ###########################################################################//


//        seek_bar = (SeekBar)findViewById(R.id.seekBar);
//        seek_bar.setProgress(50);
//        seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            double progress_value;
//            double temp;
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                temp = ((double) progress); //0 - 100
//                temp = -.003+(temp/50)*.003; //-.001 - .001
////                progress_value = -1 + (progress_value/50.0);
//                progress_value = -temp;
//                String string = String.format("%.5f", progress_value).replaceAll("(\\.\\d+?)0*$", "$1");
//                b.send(string + "/");
//                try{
//                    TimeUnit.MILLISECONDS.sleep(30);
//                }
//                catch(InterruptedException e) {
//                    System.out.println("got interrupted!");
//                }
////                Toast.makeText(Chat.this,"SeekBar value is: " + String.valueOf(progress_value),Toast.LENGTH_LONG).show();
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
////                    Toast.makeText(Chat.this,"SeekBar Started Tracking",Toast.LENGTH_LONG).show();
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
////                Toast.makeText(Chat.this,"SeekBar Stopped Tracking",Toast.LENGTH_LONG).show();
//                seekBar.setProgress(50);
//                for(int i=0;i<200;i++){
//                    b.send(String.valueOf(0.0) + "/");
//                }
//
//            }
//        });
    }

    private double smoothControlFunction(double x) {

        return (1.5*x);
    }




    @Override
    public void onAccuracyChanged (Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged (SensorEvent event) {
        Sensor sensor = event.sensor;
        mTextViewGyroLeft = (TextView) findViewById(R.id.textView_gyros_left);

        if (sensor.getType() == Sensor.TYPE_GRAVITY) {

            waistVelocity = event.values[1]/1000;
            shoulderVelocity = -event.values[0]/1000;

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
//        Display(name+": "+message);
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
