package com.example.maelle.mdp;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SensorManager sensorManager;
    private Sensor sensor;
    ImageButton forwardBtn;
    ImageButton leftBtn;
    ImageButton backwardBtn;
    ImageButton rightBtn;
//    Button waypoint;
    Button startpoint;
    Button ftpBtn;
//    Button explorationBtn;
    Button manualBtn;
    Switch tiltBtn;
    ToggleButton autoBtn;
    TextView incomingText;
    TextView robotStatus;
    TextView connectionStatusBox;
    TextView md5ExplorationText;
    TextView md5ObstacleText;
    static String connectedDevice;
    boolean connectedState;
    boolean currentActivity;
    BluetoothDevice myBTConnectionDevice;
    static Context context;
    MazeView myMaze;
    boolean autoUpdate;
    boolean tiltNavi;


    //UUID
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(" MainAcitvity:", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myMaze = findViewById(R.id.mapView);

        connectedDevice = null;
        connectedState = false;
        currentActivity = true;
        autoUpdate = true;
//        tiltNavi = false;
        forwardBtn = findViewById(R.id.forwardBtn);
        leftBtn = findViewById(R.id.leftBtn);
        rightBtn = findViewById(R.id.rightBtn);
        backwardBtn = findViewById(R.id.backwardBtn);
        connectionStatusBox = findViewById(R.id.connectionStatus);
//        waypoint = findViewById(R.id.wpBtn);
        startpoint = findViewById(R.id.spBtn);
//        explorationBtn = findViewById(R.id.explorationBtn);
        ftpBtn = findViewById(R.id.ftpBtn);
        robotStatus = findViewById(R.id.robotStatus);
        manualBtn = findViewById(R.id.manualBtn);
        autoBtn = findViewById(R.id.autoBtn);
//        tiltBtn = findViewById(R.id.tiltSwitch);
        incomingText = findViewById(R.id.incomingText);
        md5ExplorationText = findViewById(R.id.md5ExplorationText);
        md5ObstacleText = findViewById(R.id.md5ObstacleText);

        //SET ONCLICKLISTENER FOR NAVIGATION BUTTON
        onClickNavigationForward();
        onClickNavigationBackward();
        onClickNavigationRight();
        onClickNavigationLeft();
//        onClickSetWaypoint();
        onClickSetStartPoint();
        onClickStartSp();
//        onClickStartExploration();
        onClickManualUpdate();
        onClickAutoUpdate();
//        onClickTiltSwitch();

        //MAKE TEXTFIELD SCROLLABLE
        incomingText.setMovementMethod(new ScrollingMovementMethod());
        md5ObstacleText.setMovementMethod(new ScrollingMovementMethod());
        md5ExplorationText.setMovementMethod(new ScrollingMovementMethod());

        //DECLARING SENSOR MANAGER AND SENSOR TYPE
//        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //REGISTER TILT MOTION SENSOR
//        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);


        //REGISTER BROADCAST RECEIVER FOR INCOMING MSG
        LocalBroadcastManager.getInstance(this).registerReceiver(btConnectionReceiver, new IntentFilter("btConnectionStatus"));

        //REGISTER BROADCAST RECEIVER FOR IMCOMING MSG
        LocalBroadcastManager.getInstance(this).registerReceiver(incomingMsgReceiver, new IntentFilter("IncomingMsg"));
    }

    @Override
    public void onBackPressed() {


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    //RESUME ACTIVITY
    @Override
    protected void onResume() {
        super.onResume();

        currentActivity = true;

        //REGISTER TILT MOTION SENSOR
//        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        Log.d(" MainAcitvity:", "OnResume:" + connectedState);

        //CHECK FOR EXISTING CONNECTION
        if (connectedState) {
            Log.d(" MainAcitvity:", "OnResume1");

            //SET TEXTFIELD TO DEVICE NAME
            connectionStatusBox.setText(connectedDevice);
        } else {
            Log.d(" MainAcitvity:", "OnResume2");

            //SET TEXTFIELD TO NOT CONNECTED
            connectionStatusBox.setText(R.string.btStatusOffline);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity: onDestroyed: destroyed");

        //unregisterReceiver(btConnectionReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
        SIDE MENUS ONCLICK
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            //ONCLICK BLUETOOTH SEARCH BUTTON
            case R.id.connect:

                currentActivity = false;

                Intent intent = new Intent(MainActivity.this, Connect.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);

        }

        switch (item.getItemId()) {
            case R.id.reconfigure:

                currentActivity = false;

                Intent intent = new Intent(MainActivity.this, Reconfigure.class);
                // intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    //BROADCAST RECEIVER FOR INCOMING MESSAGE
    BroadcastReceiver incomingMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String msg = intent.getStringExtra("receivingMsg");
            incomingText.setText(msg);

            Log.d(TAG, "Receiving incomingMsg!!!" + msg);

            //FILTER EMPTY AND CONCATENATED STRING FROM RECEIVING CHANNEL
            if (msg.length() > 7 && msg.length() < 345) {

                //CHECK IS STARTING STRING FOR ANDROID
                if (msg.substring(0, 7).equals("Android")) {

                    String[] filteredMsg = delimiterMsg(msg.replaceAll(" ", "").replaceAll("\\n", "").trim(), "\\|");

                    Log.d(TAG, "Stage 1: " + filteredMsg[2]);

                    switch (filteredMsg[2]) {


                        case "arenaupdate":

                            String[] mazeInfo = delimiterMsg(filteredMsg[3], ",");

                            try {
                                //ENSURE ROBOT COORDINATES IS WITHIN RANGE
                                if (Integer.parseInt(mazeInfo[2]) > 0 && Integer.parseInt(mazeInfo[2]) < 14 && Integer.parseInt(mazeInfo[3]) > 0 && Integer.parseInt(mazeInfo[3]) < 19) {

                                    myMaze.updateMaze(mazeInfo, autoUpdate);

                                }
                                //SET ROBOT STATUS TO STOP FOR EXPLORATION WHEN ROBOT RETURN TO ORIGINAL POSITION
                                if (mazeInfo[2].equals("13") && mazeInfo[3].equals("18") && robotStatus.getText().equals(getString(R.string.FastestPath))) {
                                    robotStatus.setText(R.string.Robot_Idle);
                                }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            break;

                        case "robotstatus":
                            Log.d(TAG, "RobotStatus: " + filteredMsg[3]);

                            try {
                                if (filteredMsg[3].equals("stop")) {
                                    robotStatus.setText(R.string.Robot_Idle);
                                } else if (filteredMsg[3].equals("moving")) {
                                    robotStatus.setText(R.string.Robot_Moving);
                                }
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            break;

                        case "hex":

                            try {
                                String[] mapDescriptor = delimiterMsg(filteredMsg[3], ",");
                                mapDescriptor[0] = "Part1: " + mapDescriptor[0].toUpperCase();
                                mapDescriptor[1] = "Part2: " + mapDescriptor[1].toUpperCase();

                                md5ExplorationText.setText(mapDescriptor[0]);
                                md5ObstacleText.setText(mapDescriptor[1]);
                                robotStatus.setText(R.string.Robot_Idle);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            }
        }
    };


    /*
      ONCLICKLISTENER FOR START SHORTEST PATH
  */
    public void onClickStartSp() {

        ftpBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

//                int wayPoint[] = myMaze.getWaypoint();

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                }
                    //START SHORTEST PATH
                     //SEND START FASTEST PATH MSG TO RPI
                        String startSP = "Algorithm|Android|StartFastestPath|1000";
                        byte[] bytes = startSP.getBytes(Charset.defaultCharset());
                        BluetoothChat.writeMsg(bytes);

                        Toast.makeText(MainActivity.this, "STARTING SHORTEST PATH...",
                                Toast.LENGTH_SHORT).show();

                        //SET ROBOT STATUS TO EXPLORATION
                        robotStatus.setText(R.string.FastestPath);

                    }

//            }

        });

    }

    /*
     ONCLICKLISTENER FOR SET START POINT BUTTON
 */
    public void onClickSetStartPoint() {

        startpoint.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                myMaze.setStartPoint(true);
                Toast.makeText(MainActivity.this, "Select Start Point for Robot",
                        Toast.LENGTH_SHORT).show();

            }

        });

    }

    /*
        ONCLICKLISTENER FOR FORWARD MOVEMENT BUTTON
    */
    public void onClickNavigationForward() {

        forwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {

                    String navi = "Arduino|Android|F|01";
                    byte[] bytes = navi.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Toast.makeText(MainActivity.this, "Forward Movement Pressed!!",
                            Toast.LENGTH_SHORT).show();
                }

            }

        });

    }

    /*
        ONCLICKLISTENER FOR BACKWARD MOVEMENT BUTTON
    */
    public void onClickNavigationBackward() {

        backwardBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String navi = "Arduino|Android|T|Nil";
                    byte[] bytes = navi.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Toast.makeText(MainActivity.this, "Backward Movement Pressed!!",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });

    }

    /*
        ONCLICKLISTENER FOR LEFTWARD MOVEMENT BUTTON
    */
    public void onClickNavigationRight() {

        rightBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String navi = "Arduino|Android|R|Nil";
                    byte[] bytes = navi.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Toast.makeText(MainActivity.this, "Right Turn Movement Pressed!!",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });

    }

    /*
        ONCLICKLISTENER FOR RIGHTWARD MOVEMENT BUTTON
    */
    public void onClickNavigationLeft() {

        leftBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                //CHECK IF CONNECTED TO DEVICE FIRST
                if (connectedDevice == null) {
                    Toast.makeText(MainActivity.this, "Please Connect to a Device First!!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String navi = "Arduino|Android|L|Nil";
                    byte[] bytes = navi.getBytes(Charset.defaultCharset());
                    BluetoothChat.writeMsg(bytes);
                    Toast.makeText(MainActivity.this, "Left Turn Movement Pressed!!",
                            Toast.LENGTH_SHORT).show();
                }
            }

        });

    }

    /*
      ONCLICKLISTENER FOR MANUAL MAZE UPDATE BUTTON
  */
    public void onClickManualUpdate() {

        manualBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {


                myMaze.refreshMap();
                Toast.makeText(MainActivity.this, "Maze Manual Update!!",
                        Toast.LENGTH_SHORT).show();

            }

        });

    }

    /*
    ONCLICKLISTENER FOR AUTO MAZE UPDATE BUTTON
   */
    public void onClickAutoUpdate() {

        autoBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    autoUpdate = true;

                    // Make a toast to display toggle button status
                    Toast.makeText(MainActivity.this,
                            "Auto Update Toggle on", Toast.LENGTH_SHORT).show();
                } else {

                    autoUpdate = false;

                    // Make a toast to display toggle button status
                    Toast.makeText(MainActivity.this,
                            "Auto Update Toggle off", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //BROADCAST RECEIVER FOR BLUETOOTH CONNECTION STATUS
    BroadcastReceiver btConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "Receiving btConnectionStatus Msg!!!");

            String connectionStatus = intent.getStringExtra("ConnectionStatus");
            myBTConnectionDevice = intent.getParcelableExtra("Device");
            //myBTConnectionDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //DISCONNECTED FROM BLUETOOTH CHAT
            if (connectionStatus.equals("disconnect")) {

                Log.d("MainActivity:", "Device Disconnected");
                connectedDevice = null;
                connectedState = false;
                connectionStatusBox.setText(R.string.btStatusOffline);

                if (currentActivity) {

                    //RECONNECT DIALOG MSG
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("BLUETOOTH DISCONNECTED");
                    alertDialog.setMessage("Connection with device: '" + myBTConnectionDevice.getName() + "' has ended. Do you want to reconnect?");
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                    //START BT CONNECTION SERVICE
                                    Intent connectIntent = new Intent(MainActivity.this, BluetoothConnectionService.class);
                                    connectIntent.putExtra("serviceType", "connect");
                                    connectIntent.putExtra("device", myBTConnectionDevice);
                                    connectIntent.putExtra("id", myUUID);
                                    startService(connectIntent);
                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();

                }
            }
            //SUCCESSFULLY CONNECTED TO BLUETOOTH DEVICE
            else if (connectionStatus.equals("connect")) {

                connectedDevice = myBTConnectionDevice.getName();
                connectedState = true;
                Log.d("MainActivity:", "Device Connected " + connectedState);
                connectionStatusBox.setText(connectedDevice);
                Toast.makeText(MainActivity.this, "Connection Established: " + myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

            //BLUETOOTH CONNECTION FAILED
            else if (connectionStatus.equals("connectionFail")) {
                Toast.makeText(MainActivity.this, "Connection Failed: " + myBTConnectionDevice.getName(),
                        Toast.LENGTH_LONG).show();
            }

        }
    };

    private String[] delimiterMsg(String msg, String delimiter) {

        return (msg.toLowerCase()).split(delimiter);
    }


}


