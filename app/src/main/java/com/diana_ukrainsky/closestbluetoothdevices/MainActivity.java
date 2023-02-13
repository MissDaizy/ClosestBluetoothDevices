package com.diana_ukrainsky.closestbluetoothdevices;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
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

import static android.bluetooth.BluetoothAdapter.*;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private IntentFilter intentFilter;
    private TextView tv, tv_devices;
    private Button btn_bluetoothScan;
    private Boolean isLocationPermission;

    private SharedPreferences sharedPref;
    private boolean isShowMessage;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private final int REQUEST_CODE_PERMISSION_BLUETOOTH_CONNECT = 500;
    private final int REQUEST_CODE_PERMISSION_BLUETOOTH_ACCESS_FINE_LOCATION = 501;
    private static final int MANUALLY_LOCATION_PERMISSION_REQUEST_CODE = 124;



//    ActivityResultCallback<Boolean> nearbyPermissionCallBack = new ActivityResultCallback<Boolean>() {
//        @Override
//        public void onActivityResult(Boolean isGranted) {
//
//            if (isGranted) {
//                requestNearby();
//            }else {
//                requestPermissionWithRationaleCheck();
//            }
//
//        }
//    };
//    ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), nearbyPermissionCallBack);


    //common callback for location and nearby
    ActivityResultCallback<Boolean> permissionCallBack = new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean isGranted) {
            if(isLocationPermission ==null) {
                requestPermissionWithRationaleCheck();
            }
            else {
                if (isGranted && isLocationPermission) {//location permission ok
                    requestNearby();

                }else if(isGranted && !isLocationPermission){//nearby permission ok
                    //todo BLUETOOTH_SCAN

                }else {
                    requestPermissionWithRationaleCheck();// if current (location/nearby) no permission
                }
            }
        }
    };
    ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), permissionCallBack);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        setSharedPreferences();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createIntentFilter();

//        checkBluetoothPermission();

        setListener();
    }

    private void setSharedPreferences() {
        sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    private void setListener() {

        btn_bluetoothScan.setOnClickListener(v -> {


            checkBluetoothPermission();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_SCAN}, 2);
                        return;
                    }
                }
            }
            registerReceiver(bluetoothScanReceiver, intentFilter);

            bluetoothAdapter.startDiscovery();

            tv.setText("receiver registered 1");
        });
    }

    private void checkBluetoothPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        String str = "Bluetooth nearby permission= " + result;
        str += "\nShould Show Message= " + ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_CONNECT);
        tv.setText(str);

        boolean resultNearby = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
        boolean resultLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;

        if(resultLocation) {
            requestLocation();
        }else if (resultNearby){
            requestNearby();
        }
        else {
//            isShowMessage = true;
//            SharedPreferences.Editor editor = sharedPref.edit();
//            editor.putBoolean(getString(R.string.is_show_message), isShowMessage);
//            editor.apply();
//            tv.setTextColor( getResources().getColor(R.color.purple_200));
//            tv.setText(R.string.showMessage);
            openPermissionSettingDialog();
        }


//        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
//                //  return;
//            }
//        }
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                    REQUEST_LOCATION_PERMISSION);
//        }
    }

    private void requestNearby() {
        isLocationPermission = false;
        requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
    }


    private void requestPermissionWithRationaleCheck() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)) {
            openPermissionSettingDialog();

        } else if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)){
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
            AlertDialog alertDialog =
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(message)
                            .setPositiveButton(getString(android.R.string.ok),
                                    (dialog, which) -> {
                                        openSettingsManually();
                                        dialog.cancel();
                                    }).show();
            alertDialog.setCanceledOnTouchOutside(true);

    }
    private ActivityResultLauncher<Intent> manuallyPermissionResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //todo
                        //  requestLocation();
                    }
                }
            });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANUALLY_LOCATION_PERMISSION_REQUEST_CODE) {
            boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            if (result) {
                requestNearby();
                return;
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
        tv = findViewById(R.id.tv_broadcast);
        tv_devices = findViewById(R.id.tv_devices);
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
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
            } else if (ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismiss progress dialog
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

//                showToast("Found device " + device.getName());
                if (device.getName() != null) {
                    showDevices("Found device " + device.getName());
                }
            }
        }
    };

    private void showDevices(String deviceFounded) {
        tv_devices.append("\n" + deviceFounded);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        if(bluetoothScanReceiver!=null)

             unregisterReceiver(bluetoothScanReceiver);

    }
}