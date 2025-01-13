package com.example.bleappv3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.UUID;

public class BLEService extends Service {

    private static final String TAG = "BLEService";

    private static final String CHANNEL_ID = "BLEServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    // BLE
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic writableCharacteristic;

    // UUIDs
    private static final UUID SERVICE_UUID = UUID.fromString("895dc926-817a-424d-8736-f582d2dbac8e"); // Replace with your service UUID
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("7953deb4-b2e1-4829-a692-8ec173cc71fc"); // Replace with your characteristic UUID

    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        createNotificationChannel();

        startForegroundServiceWithCompatibility();
        Log.d(TAG, "BLEService created");
    }

    private void startForegroundServiceWithCompatibility() {
        Notification notification = createNotification("BLE Service is running...");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String deviceAddress = intent.getStringExtra("device_address");
            int command = intent.getIntExtra("command", -1);

            if (deviceAddress != null) {
                connectToDevice(deviceAddress);
            } else if (command != -1) {
                sendCommand(command);
            }
        }
        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (bluetoothGatt != null) {
            if (hasBluetoothPermission()) {
                bluetoothGatt.disconnect();
            }
            bluetoothGatt.close();
        }
        Log.d(TAG, "BLEService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    private boolean hasBluetoothPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(String deviceAddress) {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "Permission not granted");
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        bluetoothGatt = device.connectGatt(this, false, gattCallback); // autoConnect = false for initial connection
        Log.d(TAG, "Connecting to device: " + device.getName() + " (" + deviceAddress + ")");

        updateNotification("Connecting to " + device.getName());

        // Timeout if connection doesn't succeed
        handler.postDelayed(() -> {
            if (bluetoothGatt != null && writableCharacteristic == null) {
                Log.e(TAG, "Connection timed out");
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
        }, 10000); // 10-second timeout
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d(TAG, "Connected to GATT server");
                    gatt.discoverServices();
                    updateNotification("Connected to " + gatt.getDevice().getName());
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "Disconnected from GATT server");
                    updateNotification("Disconnected from " + gatt.getDevice().getName());
                    reconnectToDevice(gatt.getDevice());
                }
            } else {
                Log.e(TAG, "Connection failed with status: " + status);
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    writableCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (writableCharacteristic != null) {
                        Log.d(TAG, "Writable characteristic found");
                    } else {
                        Log.e(TAG, "Writable characteristic not found");
                    }
                } else {
                    Log.e(TAG, "Service not found");
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Command sent successfully");
            } else {
                Log.e(TAG, "Failed to send command with status: " + status);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void reconnectToDevice(BluetoothDevice device) {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "Permission not granted");
            return;
        }
        if (device != null) {
            handler.postDelayed(() -> {
                Log.d(TAG, "Reconnecting to device: " + device.getName());
                bluetoothGatt = device.connectGatt(this, true, gattCallback); // autoConnect = true for reconnection
            }, 5000); // 5-second delay before reconnecting
        } else {
            Log.e(TAG, "Device is null. Cannot reconnect.");
        }
    }

    @SuppressLint("MissingPermission")
    private void sendCommand(int command) {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "Permission not granted");
            return;
        }
        if (writableCharacteristic != null) {
            writableCharacteristic.setValue(new byte[]{(byte) command});
            boolean success = bluetoothGatt.writeCharacteristic(writableCharacteristic);
            if (success) {
                Log.d(TAG, "Command sent: " + command);
            } else {
                Log.e(TAG, "Failed to send command: " + command);
            }
        } else {
            Log.e(TAG, "Writable characteristic is null. Cannot send command.");
        }
    }

    private Notification createNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BLE Service")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String contentText) {
        Notification notification = createNotification(contentText);
        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
