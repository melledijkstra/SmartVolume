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
import java.util.Locale;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Bluetooth communication thread
 * Created by Melle on 28-12-2017.
 */

public class BluetoothSocketThread extends Thread {
    // The bluetooth device (HC-04)
    private final BluetoothDevice bDevice;
    // A socket used to communicate with the device
    private BluetoothSocket socket;
    // The activity on which UI functionality should be run
    private final Activity activity;
    // The unique service identifier for the HC-04, this could be different for other bluetooth devices
    // @see https://stackoverflow.com/questions/14071131/android-find-the-uuid-of-a-specific-bluetooth-device
    private static final UUID HC_04_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothSocketThread(Activity activity, BluetoothDevice device) {
        this.activity = activity;
        this.bDevice = device;
    }

    @Override
    public void run() {
        // Always cancel discovery before connecting (from google android guide)
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        InputStream inputStream = null;
        try {
            // Create the communication socket with the bluetooth device
            socket = bDevice.createInsecureRfcommSocketToServiceRecord(HC_04_UUID);
            socket.connect();
            inputStream = socket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            while (socket.isConnected() && !interrupted()) {
                // This actually reads the new measurement value from the Arduino
                final int latestMeasure = Integer.parseInt(in.readLine()); // blocking call!
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Update the UI with new value
                        ((MainActivity) activity).lblNoiseLevel.setText(String.format(Locale.getDefault(), "%d", latestMeasure));
                        // Check the noise level with threshold and notify if needed
                        ((MainActivity) activity).checkNoiseLevel(latestMeasure);
                    }
                });
            }
        } catch (IOException e) {
            // Show a Toast if we could not connect to device
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, "Could not connect with device", Toast.LENGTH_SHORT).show();
                }
            });
            e.printStackTrace();
        } catch(NumberFormatException e) {
            // When the received number could not be parsed
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket != null) {
                    socket.close();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "Connection closed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "Thread stopped");
    }
}
