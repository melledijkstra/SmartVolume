package nl.melledijkstra.smartvolume;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.PrintStream;
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
        try {
            socket = this.bDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
            PrintStream out = new PrintStream(socket.getOutputStream());
            while (socket.isConnected()) {
                try {
                    out.print('0');
                    out.flush();
                    Thread.sleep(1000);
                    out.print('1');
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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
        Log.d(TAG, "Thread stopped");
    }
}
