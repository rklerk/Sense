package nl.sense.android.demo.Activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import nl.sense.android.demo.R;
import nl.sense.android.demo.Controllers.SenseController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import nl.sense_os.platform.SenseApplication;

public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_OAUTH = 1;

    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    View curView;
    private ListView listView;
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<BluetoothDevice> adapter;

    public static final String TAG = "Demo";

    private SenseApplication mApplication;
    private nl.sense.android.demo.Controllers.SenseController senseController;

    // Bluetooth related variables
    private BluetoothAdapter mBluetoothAdapter;
    private UUID selectedDeviceUUID;
    private final static int REQUEST_ENABLE_BT = 1;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                addDeviceToList(device);
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Put application specific code here.

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        setContentView(R.layout.activity_main);

        curView = getWindow().getDecorView().findViewById(android.R.id.content);
        listView = (ListView) curView.findViewById(R.id.listView);

        adapter = new ArrayAdapter<BluetoothDevice>(this,
                android.R.layout.simple_list_item_1,
                devices);
        listView.setAdapter(adapter);
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
//                Intent intent = new Intent(MainActivity.this, SendMessage.class);
//                String message = "abc";
//                intent.putExtra(EXTRA_MESSAGE, message);
//                startActivity(intent);
//                  BluetoothDevice selectedDevice = (BluetoothDevice) listView.getItemAtPosition(position);
//                  selectedDevice.getUuids()[0].getUuid();
//                //TODO: Check for unknown device type?
//
//                  if(selectedDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE || selectedDevice.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
//                      //Connect the LE way
//                  }
//                  else {
//                      //Connect the classic way
//                      new AcceptThread();
//                  }

            }
        });


        mApplication = (SenseApplication) getApplication();
        senseController = new SenseController(mApplication);
        senseController.startSense();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getApplicationContext().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : pairedDevices) {
            // Add the name and address to an array adapter to show in a ListView
            Log.d("TAG", device.getName() + "\n" + device.getAddress());
        }

        mBluetoothAdapter.startDiscovery();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Connecting...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            mApplication.getSensePlatform().logout();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        senseController.stopSense();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().unregisterReceiver(mReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }

    public void addDeviceToList(BluetoothDevice device) {
        devices.add(device);
    }

    public void getDataClick(View v) {
        senseController.getLocalData();

    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private final UUID defaultUUID = UUID.fromString("bb35ed3f-7d4c-45f3-a072-a3745ea04d35");

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(TAG, selectedDeviceUUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void manageConnectedSocket(BluetoothSocket socket) {

        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }
    }
}
