package nl.melledijkstra.smartvolume;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Bluetooth communication thread
 * Created by Melle on 28-12-2017.
 */

public class BluetoothSocketThread extends Thread {

    private final BluetoothDevice bDevice;
    private BluetoothSocket socket;

    private final Activity activity;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothSocketThread(Activity activity, BluetoothDevice device) {
        this.activity = activity;
        this.bDevice = device;
    }

    @Override
    public void run() {
        // Always cancel discovery before connecting
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        InputStream inputStream = null;
        try {
            socket = this.bDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Could not connect with device", Toast.LENGTH_SHORT).show();
                }
            });
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

        if(inputStream != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            while (socket.isConnected() && !interrupted()) {
                try {
                    final int latestMeasure = Integer.parseInt(in.readLine());
                    Log.d(TAG, Integer.toString(latestMeasure));
                    this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity)activity).lblDecibel.setText(Integer.toString(latestMeasure));
                            ((MainActivity)activity).checkDecibels(latestMeasure);
                        }
                    });
                } catch (IOException | NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.d(TAG, "Thread stopped");
        try {
            socket.close();
            this.activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Connection closed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
