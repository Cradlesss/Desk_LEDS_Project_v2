package com.example.bleappv3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // BLE
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    // UI
    private Button selectDeviceButton, openAnimationsButton, offButton, twinkleButton, jinxButton, transitionButton, bluebButton, rainbowButton, beatrButton, runredButton, rawNoiseButton, movingRainbowButton, waveButton, blightbButton;
    private ProgressBar progressBar;
    private TextView statusText;

    // Device Selection
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private ArrayAdapter<String> deviceAdapter;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final UUID SERVICE_UUID = UUID.fromString("895dc926-817a-424d-8736-f582d2dbac8e");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        registerUI();
        openAnimationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, Animations.class);
            startActivity(intent);
        });

        // Request permissions
        if (!checkPermissions()) {
            requestPermissions();
        }

        // Initialize Bluetooth
        bluetoothAdapter = ((android.bluetooth.BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Check Bluetooth availability
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Button listeners
        selectDeviceButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                startDeviceScan();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUI(){
        selectDeviceButton = findViewById(R.id.selectDeviceBtn);
        openAnimationsButton = findViewById(R.id.goToAnimations);
        setButtonsEnabled(false);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
    }



    private boolean checkPermissions() {
        boolean hasFineLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        Log.d("Permissions", "Fine Location: " + hasFineLocation + ", Coarse Location: " + hasCoarseLocation);

        return hasFineLocation || hasCoarseLocation;
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
        } else {
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.FOREGROUND_SERVICE);

        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean permissionsGranted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = true;
                    break;
                }
            }

            if (permissionsGranted) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied. The app may not function properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startDeviceScan() {
        if (!checkPermissions()) {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Scanning for devices...");
        deviceList.clear();

        includePairedDevices(); // Include already paired devices

        try {
            List<ScanFilter> filters = new ArrayList<>();
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(SERVICE_UUID)) // Use your specific service UUID
                    .build();
            filters.add(filter);
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build();

            bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);

            // Stop scan after 5 seconds
            new Handler().postDelayed(() -> {
                try {
                    bluetoothLeScanner.stopScan(scanCallback);
                } catch (SecurityException e) {
                    Log.e(TAG, "Permission denied for stopping scan: " + e.getMessage());
                }
                showDeviceSelectionDialog();
            }, 5000);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for starting scan: " + e.getMessage());
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device != null && !deviceList.contains(device)) {
                deviceList.add(device);
                Log.d(TAG, "Device found: " + device.getName() + " (" + device.getAddress() + ")");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed with error: " + errorCode);
            Toast.makeText(MainActivity.this, "Scan failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    };

    @SuppressLint("MissingPermission")
    private void includePairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices == null || pairedDevices.isEmpty()) {
            Log.e(TAG, "No paired devices found");
            return;
        }

        if (!checkPermissions()) {
            Log.e(TAG, "Missing permissions to interact with paired devices.");
            return;
        }

        List<BluetoothDevice> pairedDeviceList = new ArrayList<>(pairedDevices);
        processNextDevice(pairedDeviceList, 0);
    }

    @SuppressLint("MissingPermission")
    private void processNextDevice(List<BluetoothDevice> devices, int index) {
        if (index >= devices.size()) {
            Log.d(TAG, "All paired devices processed");
            return; // All devices processed
        }

        BluetoothDevice device = devices.get(index);
        boolean[] isProcessingComplete = {false}; // Use an array to allow modification inside inner classes

        BluetoothGatt gatt = device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to paired device: " + device.getName());
                    gatt.discoverServices(); // Check UUID
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    if (!isProcessingComplete[0]) {
                        Log.d(TAG, "Disconnected from paired device: " + device.getName());
                        gatt.close();
                        isProcessingComplete[0] = true;
                        processNextDevice(devices, index + 1); // Proceed to the next device
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(SERVICE_UUID);
                    if (service != null) {
                        Log.d(TAG, "Service found on device: " + device.getName());
                        if (!deviceList.contains(device)) {
                            deviceList.add(device);
                        }
                    } else {
                        Log.d(TAG, "Service not found on device: " + device.getName());
                    }
                } else {
                    Log.e(TAG, "Failed to discover services on device: " + device.getName());
                }

                if (!isProcessingComplete[0]) {
                    gatt.disconnect(); // Disconnect and process the next device
                    isProcessingComplete[0] = true;
                }
            }
        });

        // Timeout to prevent hanging
        new Handler().postDelayed(() -> {
            if (!isProcessingComplete[0] && gatt != null) {
                Log.e(TAG, "Timeout while connecting to device: " + device.getName());
                gatt.disconnect();
                gatt.close();
                processNextDevice(devices, index + 1); // Proceed to the next device
            }
        }, 10000); // 10-second timeout
    }




    @SuppressLint("MissingPermission")
    private void showDeviceSelectionDialog() {
        progressBar.setVisibility(View.GONE);
        if (deviceList.isEmpty()) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Device");

        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        for (BluetoothDevice device : deviceList) {
            deviceAdapter.add(device.getName() + " (" + device.getAddress() + ")");
        }

        builder.setAdapter(deviceAdapter, (dialog, which) -> connectToDevice(deviceList.get(which)));
        builder.show();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if (!checkPermissions()) {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("device_address", device.getAddress());
        startService(intent); // Start foreground service to maintain connection
        Toast.makeText(this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
        setButtonsEnabled(true);
    }

    private void sendCommand(int command) {
        Intent intent = new Intent(this, BLEService.class);
        intent.putExtra("command", command);
        startService(intent); // Send command via BLE service
    }

    private void setButtonsEnabled(boolean enabled) {
        openAnimationsButton.setEnabled(enabled);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, BLEService.class)); // Stop the foreground service
    }
}
