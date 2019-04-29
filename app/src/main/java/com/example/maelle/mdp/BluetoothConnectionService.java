package com.example.maelle.mdp;

import android.app.Activity;
import android.app.IntentService;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by darks on 07-Feb-18.
 */


public class BluetoothConnectionService extends IntentService {

    private static final String TAG = "BTConnectionAService";
    private static final String appName = "Mdp";

    //UUID
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter myBluetoothAdapter;

    private AcceptThread myAcceptThread;
    private ConnectThread myConnectThread;
    public  BluetoothDevice myDevice;
    private UUID deviceUUID;
    private Handler mHandler;

    Context myContext;
    ProgressDialog myProgressDialog;

    //CONSTRUCTOR
    public BluetoothConnectionService() {

        super("BluetoothConnectionService");
        // mHandler = new Handler(Looper.getMainLooper());
    }


    //HANDLE INTENT FOR SERVICE. START THIS METHOD WHEN THE SERVICE IS CREATED
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        myContext = getApplicationContext();
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (intent.getStringExtra("serviceType").equals("listen")) {

            myDevice = (BluetoothDevice) intent.getExtras().getParcelable("device");

            Log.d(TAG, "Service Handle: startAcceptThread");

            startAcceptThread();
        } else {
            myDevice = (BluetoothDevice) intent.getExtras().getParcelable("device");
            deviceUUID = (UUID) intent.getSerializableExtra("id");

            Log.d(TAG, "Service Handle: startClientThread");

            startClientThread(myDevice, deviceUUID);
        }

    }

    /*
         A THREAD THAT RUNS WHILE LISTENING FOR INCOMING CONNECTIONS. IT BEHAVES LIKE
         A SERVER-SIDE CLIENT. IT RUNS UNTIL A CONNECTION IS ACCEPTED / CANCELLED
    */
    private class AcceptThread extends Thread {

        //Local server socket
        private final BluetoothServerSocket myServerSocket;

        public AcceptThread() {
            BluetoothServerSocket temp = null;

            //Create a new listening server socket
            try {
                temp = myBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);
                Log.d(TAG, "AcceptThread: Setting up server using: " + myUUID);

            } catch (IOException e) {

            }

            myServerSocket = temp;
        }

        public void run() {

            Log.d(TAG, "AcceptThread: Running");

            BluetoothSocket socket = null;
            Intent connectionStatusIntent;

            try {

                Log.d(TAG, "Run: RFCOM server socket start....");

                //Blocking call which will only return on a successful connection / exception
                socket = myServerSocket.accept();

                //BROADCAST CONNECTION MSG
                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connect");
                connectionStatusIntent.putExtra("Device", Connect.getBluetoothDevice());
                LocalBroadcastManager.getInstance(myContext).sendBroadcast(connectionStatusIntent);

                //Successfully connected
                Log.d(TAG, "Run: RFCOM server socket accepted connection");

                //START BLUETOOTH CHAT
                BluetoothChat.connected(socket, myDevice, myContext);


            } catch (IOException e) {

                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connectionFail");
                connectionStatusIntent.putExtra("Device",  Connect.getBluetoothDevice());

                Log.d(TAG, "AcceptThread: Connection Failed ,IOException: " + e.getMessage());
            }


            Log.d(TAG, "Ended AcceptThread");

        }

        public void cancel() {

            Log.d(TAG, "Cancel: Canceling AcceptThread");

            try {
                myServerSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Cancel: Closing AcceptThread Failed. " + e.getMessage());
            }
        }


    }

    /*
        THREAD RUNS WHILE ATTEMPTING TO MAKE AN OUTGOING CONNECTION WITH A DEVICE.
        IT RUNS STRAIGHT THROUGH, MAKING EITHER A SUCCESSFULLY OR FAILED CONNECTION
    */
    private class ConnectThread extends Thread {

        private BluetoothSocket mySocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {

            Log.d(TAG, "ConnectThread: started");
            myDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket temp = null;
            Intent connectionStatusIntent;

            Log.d(TAG, "Run: myConnectThread");

            /*
            Get a BluetoothSocket for a
            connection with given BluetoothDevice
            */
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRFcommSocket using UUID: " + myUUID);
                temp = myDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {

                Log.d(TAG, "ConnectThread: Could not create InsecureRFcommSocket " + e.getMessage());
            }

            mySocket = temp;

            //Cancel discovery to prevent slow connection
            myBluetoothAdapter.cancelDiscovery();

            try {

                Log.d(TAG, "Connecting to Device: " + myDevice);
                //Blocking call and will only return on a successful connection / exception
                mySocket.connect();


                //BROADCAST CONNECTION MSG
                connectionStatusIntent = new Intent("btConnectionStatus");
                connectionStatusIntent.putExtra("ConnectionStatus", "connect");
                connectionStatusIntent.putExtra("Device", myDevice);
                LocalBroadcastManager.getInstance(myContext).sendBroadcast(connectionStatusIntent);

                Log.d(TAG, "run: ConnectThread connected");

                //START BLUETOOTH CHAT
                BluetoothChat.connected(mySocket, myDevice, myContext);

                //CANCEL ACCEPT THREAD FOR LISTENING
                if (myAcceptThread != null) {
                    myAcceptThread.cancel();
                    myAcceptThread = null;
                }

            } catch (IOException e) {

                //Close socket on error

                try {
                    mySocket.close();

                    connectionStatusIntent = new Intent("btConnectionStatus");
                    connectionStatusIntent.putExtra("ConnectionStatus", "connectionFail");
                    connectionStatusIntent.putExtra("Device", myDevice);

                    LocalBroadcastManager.getInstance(myContext).sendBroadcast(connectionStatusIntent);
                    Log.d(TAG, "run: Socket Closed: Connection Failed!! " + e.getMessage());

                } catch (IOException e1) {
                    Log.d(TAG, "myConnectThread, run: Unable to close socket connection: " + e1.getMessage());
                }

            }

            try {
                //Dismiss Progress Dialog when connection established
                //myProgressDialog.dismiss();
                //mHandler.post(new DisplayToast(getApplicationContext(),"Connection Established With: "+myDevice.getName()));


            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        }

        public void cancel() {

            try {
                Log.d(TAG, "Cancel: Closing Client Socket");
                mySocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Cancel: Closing mySocket in ConnectThread Failed " + e.getMessage());
            }
        }
    }

    //START ACCEPTTHREAD AND LISTEN FOR INCOMING CONNECTION
    public synchronized void startAcceptThread() {

        Log.d(TAG, "start");

        //Cancel any thread attempting to make a connection
        if (myConnectThread != null) {
            myConnectThread.cancel();
            myConnectThread = null;
        }
        if (myAcceptThread == null) {
            myAcceptThread = new AcceptThread();
            myAcceptThread.start();
        }
    }

    /*
    // CONNECTTHREAD STARTS AND ATTEMPT TO MAKE A CONNECTION WITH THE OTHER DEVICES
    */
    public void startClientThread(BluetoothDevice device, UUID uuid) {

        Log.d(TAG, "startClient: Started");

        //Progress Dialog
        //myProgressDialog = ProgressDialog.show(this, "Connecting Bluetooth", "Please Wait...", true);

        myConnectThread = new ConnectThread(device, uuid);
        myConnectThread.start();

    }


}
