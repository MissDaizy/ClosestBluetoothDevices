package com.diana_ukrainsky.closestbluetoothdevices;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Button;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothAdapter.*;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private AlertDialog alertDialog;
    private Device device;
    private BluetoothAdapter bluetoothAdapter;
    private IntentFilter intentFilter;
    private Button btn_bluetoothScan;
    private Boolean isLocationPermission;

    private static final int MANUALLY_LOCATION_PERMISSION_REQUEST_CODE = 124;

    //    private List<BluetoothDevice> mDevices;
    private List<Device> bluetoothDevices;
    //common callback for location and nearby
    ActivityResultCallback<Boolean> permissionCallBack = new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean isGranted) {
            if (isLocationPermission == null) {
                requestPermissionWithRationaleCheck();
            } else {
                if (isGranted && isLocationPermission) {//location permission ok
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestNearby();
                    }
                } else {
                    openPermissionSettingDialog();
                }
            }
        }
    };
    ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), permissionCallBack);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setData();
        findViews();
        setRecyclerView();
        setViewAdapter();

        setBluetoothAdapter();
        createIntentFilter();
        registerReceiver(bluetoothScanReceiver, intentFilter);

        setListeners();
    }

    private void setRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    private void setViewAdapter() {
        deviceAdapter = new DeviceAdapter(this, bluetoothDevices);
        recyclerView.setAdapter(deviceAdapter);
    }

    private void setData() {
        bluetoothDevices = new ArrayList<>();
        device = new Device();
    }

    private void setBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void setListeners() {

        btn_bluetoothScan.setOnClickListener(v -> {

            // tv_devices.setText("");
            checkPermissions();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, 2);
                        return;
                    }
                }
            }
//            registerReceiver(bluetoothScanReceiver, intentFilter);
            if (!bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.startDiscovery();
            }
        });
    }

    private void checkPermissions() {

        boolean resultNearby = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
        boolean resultLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        if (resultLocation) {
            requestLocation();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (resultNearby) {
                requestNearby();
            }
        }
    }

    private void requestNearby() {
        isLocationPermission = false;
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
    }

    private void requestPermissionWithRationaleCheck() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)) {
            openPermissionSettingDialog();

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            openPermissionSettingDialog();
//            requestNearby();
        }

    }

    private void requestLocation() {
        isLocationPermission = true;
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void openPermissionSettingDialog() {

        String message = "Location and Nearby permissions are important for app functionality. You will be transported to Setting screen because the permissions are permanently disable. Please manually allow them.";
        alertDialog =
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(getString(android.R.string.ok),
                                (dialog, which) -> {
                                    openSettingsManually();
                                    dialog.cancel();
                                }).show();
        alertDialog.setCanceledOnTouchOutside(true);

    }

    private final ActivityResultLauncher<Intent> manuallyPermissionResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    //The broadcast is start working
                }
            });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANUALLY_LOCATION_PERMISSION_REQUEST_CODE) {
            boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            if (result) {
                requestNearby();

            }
        }
    }

    private void openSettingsManually() {

        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        manuallyPermissionResultLauncher.launch(intent);
    }

    private void findViews() {
        recyclerView = findViewById(R.id.activityMain_RV_recyclerView);
//        tv_devices = findViewById(R.id.tv_devices);
        btn_bluetoothScan = findViewById(R.id.btn_bluetoothScan);
    }

    private void createIntentFilter() {
        intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(ACTION_STATE_CHANGED);
        intentFilter.addAction(ACTION_SCAN_MODE_CHANGED);
    }


    private final BroadcastReceiver bluetoothScanReceiver = new BroadcastReceiver() {
        @SuppressLint({"MissingPermission", "NotifyDataSetChanged"})
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // Discovery starts

                // Disable button
                btn_bluetoothScan.setBackgroundColor(getColor(R.color.grey));
                btn_bluetoothScan.setClickable(false);

                // Clear the recycler view
                bluetoothDevices.clear();
                deviceAdapter.setList(bluetoothDevices);
                deviceAdapter.notifyDataSetChanged();

            } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Enable button
                btn_bluetoothScan.setBackgroundColor(getColor(R.color.purple_500));
                btn_bluetoothScan.setClickable(true);

                //discovery finishes, dismiss progress dialog
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (name != null) {
                    device.setName(name);
                    device.setDistance(calculateDistance(rssi));

//                    if (bluetoothDevices.size() == 0 || !device.getName().equals(bluetoothDevices.get(bluetoothDevices.size() - 1).getName())) {
                        // Add device to list
                        bluetoothDevices.add(device);

                        // Put the device into recycler view that will show the devices
                        deviceAdapter.setList(bluetoothDevices);
                        deviceAdapter.notifyDataSetChanged();
//                    }
                }

            }
        }
    };

    private double calculateDistance(int rssi) {
        int txPower = -59; // Hardcoded tx power value, can be obtained from the scanRecord
        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            registerReceiver(bluetoothScanReceiver, intentFilter);
            //  tv.setText("receiver registered 2");

        } catch (Exception e) {
            // already registered
            //   tv.setText("Receiver is already received");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bluetoothScanReceiver != null)
            unregisterReceiver(bluetoothScanReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }
}