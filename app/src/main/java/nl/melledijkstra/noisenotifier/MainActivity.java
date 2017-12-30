package nl.melledijkstra.noisenotifier;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final String TAG = getClass().getSimpleName();
    final int REQUEST_ENABLE_BT = 32456;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Setting up bluetooth!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                MainActivity.this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if(mBluetoothAdapter != null) {
                    if(mBluetoothAdapter.isEnabled()) {
                        selectBluetoothDevice();
                    } else {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Your device doesn't support bluetooth, sorry!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Make the user select a bluetooth device
     */
    private void selectBluetoothDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> btDevices = new ArrayList<>();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            final ArrayList<String> deviceList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                btDevices.add(device);
                deviceList.add(String.format("%s - %s", device.getName(), device.getAddress()));
            }
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
        if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
