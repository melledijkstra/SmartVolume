package nl.melledijkstra.smartvolume;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();
	// A unique request code which identifies that a result was from a enable bluetooth request
	final int REQUEST_ENABLE_BT = 32456;
	// The bluetooth adapter class which can be used to communicate with the bluetooth module
	// on this device
	private BluetoothAdapter mBluetoothAdapter;
	// The TextView which displays the current noise level
	public TextView lblNoiseLevel;
	// The dialog which pops up when the noise level is above the threshold
	AlertDialog dialogTooLoud;
	// The threshold value which is compared to the noise level
	private int threshold = 100;
	// The unique identifier for the threshold SharedPreference, so it is persistent even if the application is closed
	private static final String PREF_THRESHOLD = "nl.melledijkstra.smartvolume.THRESHOLD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetoothAdapter != null) {
                    if (mBluetoothAdapter.isEnabled()) {
                        selectBluetoothDevice();
                    } else {
                        Snackbar.make(view, "Setting up bluetooth!", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Your device doesn't support bluetooth, sorry!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        lblNoiseLevel = findViewById(R.id.txtNoiseLevel);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        threshold = prefs.getInt(PREF_THRESHOLD, threshold);

        // Dialog that shows when noise is too loud
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Volume too high!!!")
                .setMessage("The noise around the SmartVolume device is too loud! Try to keep it down to save your ears!")
                .setIcon(android.R.drawable.ic_dialog_alert);
        dialogTooLoud = builder.create();
    }

    /**
     * Make the user select a bluetooth device
     */
    private void selectBluetoothDevice() {
        // Get list of paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> btDevices = new ArrayList<>();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            final ArrayList<String> deviceList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                btDevices.add(device);
                deviceList.add(String.format("%s - %s", device.getName(), device.getAddress()));
            }
            // Show them in a dialog to choose from
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Device List")
                    .setItems(deviceList.toArray(new CharSequence[deviceList.size()]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new BluetoothSocketThread(MainActivity.this, btDevices.get(i)).start();
                        }
                    });
            builder.create().show();
        } else {
            Toast.makeText(this, "No paired devices", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the requested bluetooth was correctly enabled, then ask user to select device
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            selectBluetoothDevice();
        } else if (requestCode == RESULT_CANCELED) {
            Toast.makeText(this, "Bluetooth is needed for this application", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Create an AlertDialog
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Choose threshold");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setRawInputType(Configuration.KEYBOARD_12KEY);
            input.setText(String.format(Locale.getDefault(), "%d", threshold));
            // The alert has a single input for the threshold
            alert.setView(input);
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // When user clicks yes, save the new threshold
                    try {
                        int newThreshold = Integer.parseInt(input.getText().toString());
                        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
                        prefs.edit().putInt(PREF_THRESHOLD, newThreshold).apply();
                        threshold = newThreshold;
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Not a valid number", Toast.LENGTH_LONG).show();
                    }
                }
            });
            alert.setNegativeButton("Cancel", null);
            alert.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Check if the measurement is above the threshold
     * @param latestMeasure The latest measurement
     */
    public void checkNoiseLevel(int latestMeasure) {
        // if the measurement is over threshold and dialog isn't already showing to the user
        if(latestMeasure > threshold && !dialogTooLoud.isShowing()) {
            // These different types of alerts need to get attention from the user
            dialogTooLoud.show();
            // Generate a 1 second tone
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);

            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            // Vibrate for 500 milliseconds
            if (v != null) {
                v.vibrate(500);
            }
        }
    }
}
