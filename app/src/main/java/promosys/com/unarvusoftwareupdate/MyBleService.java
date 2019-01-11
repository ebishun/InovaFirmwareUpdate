package promosys.com.unarvusoftwareupdate;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyBleService extends Service {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    Boolean btScanning = false;
    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<BluetoothDevice>();
    BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic sendCharacteristic;
    private String strDevice = "";

    char ETX = (char)0x03;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public Map<String, String> uuids = new HashMap<String, String>();

    private String CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = "";
    private String CHARACTERISTIC_WRITE_UUID = "";
    private String SERVICE_WRITE_UUID = "";

    public String SCANNED_MAC_ADDRESS = "";

    // Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;

    // This is the object that receives interactions from clients.
    private final IBinder mBinder = new LocalBinder();
    private StringBuffer strBleBuffer = new StringBuffer();

    public boolean isWaitingReply = false;
    public boolean isSendingPartData = false;
    public boolean isWaitingBleReply = false;

    public boolean isFirstConnected = true;

    public boolean isScanForNearbyDevice = false;

    private boolean isBluetoothConnected = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initBle();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        MyBleService getService() {
            return MyBleService.this;
        }
    }

    private void initBle(){
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btScanner = btAdapter.getBluetoothLeScanner();
        }

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Log.i("BleService","Bluetooth is off");

            sendToMainActivity(MainActivity2.mBroadcastBleDisabled,"","");
            //Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //if(result.getDevice().getAddress().equals(strBleName) || result.getDevice().getName().equals(strBleName)){
                    if(result.getDevice().getAddress().equals(SCANNED_MAC_ADDRESS) || result.getDevice().getName().equals(SCANNED_MAC_ADDRESS)){
                        //if(result.getDevice().getName().equals(strBleName)){
                        strDevice = result.getDevice().getName();
                        devicesDiscovered.add(result.getDevice());
                        deviceIndex++;
                        stopScanning();
                        connectToDeviceSelected(0);
                    }
                }

            }catch (NullPointerException error){
                Log.i("MainActivity",error.toString());
            }
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            try {
                if(device.getName() != null){
                    Log.i("MyBleService","deviceName: " + device.getName());
                    Log.i("MyBleService","deviceName: " + device.getAddress());
                }

                if(isScanForNearbyDevice){
                    if(device.getName().contains("0E")){
                        //if(result.getDevice().getName().equals(strBleName)){
                        SCANNED_MAC_ADDRESS = device.getName();
                        startConnecting(device);
                    }

                }else {
                    if(device.getName().equals(SCANNED_MAC_ADDRESS)){
                        //if(result.getDevice().getName().equals(strBleName)){
                        startConnecting(device);
                    }
                }

                /*
                if(device.getAddress().equals(SCANNED_MAC_ADDRESS) || device.getName().equals(SCANNED_MAC_ADDRESS)){
                    //if(result.getDevice().getName().equals(strBleName)){
                    startConnecting(device);
                }
                */
            }catch (NullPointerException error){
                //Log.i("MainActivity",error.toString());
            }
        }
    };

    private void startConnecting(BluetoothDevice device){
        //strDevice = result.getDevice().getName();
        strDevice = device.getName();

        //devicesDiscovered.add(result.getDevice());
        devicesDiscovered.add(device);
        deviceIndex++;

        //0F:F9:37:94:19:00
        btScanning = false;
        stopScanning();
        Log.i("MyBleService","devicesDiscovered: " + devicesDiscovered.size());

        if(devicesDiscovered.size() == 1){
            Log.i("MyBleService","Connecting");
            connectToDeviceSelected(0);
        }
    }


    public void startScanning() {
        System.out.println("start scanning");
        btScanning = true;
        deviceIndex = 0;
        devicesDiscovered.clear();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //btScanner.startScan(leScanCallback);
                    btAdapter.startLeScan(mLeScanCallback);
                }
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(btScanning){
                    Toast.makeText(getApplicationContext(),"Device not found",Toast.LENGTH_SHORT).show();
                    sendToMainActivity(MainActivity2.mBroadcastBleDeviceNotFound,"","");
                    stopScanning();
                }
            }
        }, SCAN_PERIOD);
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        btScanning = false;
        isScanForNearbyDevice = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //btScanner.stopScan(leScanCallback);
                    btAdapter.stopLeScan(mLeScanCallback);
                }
            }
        });
    }

    public void connectToDeviceSelected(int deviceIndex) {
        int deviceSelected = deviceIndex;
        bluetoothGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, false, btleGattCallback);
    }

    public void disconnectDeviceSelected() {
        if(bluetoothGatt != null)
        {
            bluetoothGatt.disconnect();
        }
    }

    private void sendToMainActivity(String whichBroadcast,String extra,String extraKey){
        Intent sendMainActivity = new Intent();
        sendMainActivity.setAction(whichBroadcast);
        if(!(extra.isEmpty())){
            sendMainActivity.putExtra(extraKey,extra);
        }
        sendBroadcast(sendMainActivity);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            sendCharacteristic = characteristic;
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] messageBytes = characteristic.getValue();
            String messageString = null;
            try {
                messageString = new String(messageBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e("MainActivity", "Unable to convert message bytes to string");
            }

            if (messageString.contains(String.valueOf(ETX))){
                strBleBuffer.append(messageString);
                final String strSubMessage = strBleBuffer.toString();
                strBleBuffer.delete(0,strBleBuffer.length()-1);

                //Intent bleReply = new Intent();
                //bleReply.setAction(MainActivity2.mBroadcastBleGotReply);
                //bleReply.putExtra("bleMessage",strSubMessage);
                //sendBroadcast(bleReply);

                sendToMainActivity(MainActivity2.mBroadcastBleGotReply,strSubMessage,"bleMessage");
            }else {
                strBleBuffer.append(messageString);
            }

        }

        @Override
        public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("MainActivity", "MTU changed to: " + mtu);
            } else {
                Log.i("MainActivity", "onMtuChanged error: " + status + ", mtu: " + mtu);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            switch (newState) {
                case 0:
                    //Intent disconnectedIntent = new Intent();
                    //disconnectedIntent.setAction(MainActivity2.mBroadcastBleDisconnected);
                    //sendBroadcast(disconnectedIntent);
                    isBluetoothConnected = false;

                    isSendingPartData = false;
                    isWaitingBleReply = false;

                    sendToMainActivity(MainActivity2.mBroadcastBleDisconnected,"","");
                    bluetoothGatt.close();

                    break;

                case 2:
                    isBluetoothConnected = true;
                    //Intent connectedIntent = new Intent();
                    //connectedIntent.setAction(MainActivity2.mBroadcastBleConnected);
                    //sendBroadcast(connectedIntent);

                    //sendToMainActivity(MainActivity2.mBroadcastBleConnected,"","");
                    bluetoothGatt.discoverServices();

                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            displayGattServices(bluetoothGatt.getServices());
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i("MainActivity","onCharRead: " + characteristic);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(isSendingPartData){
                    isWaitingBleReply = false;
                }
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            final String uuid = gattService.getUuid().toString();
            Log.i("MainActivity","Service discovered: " + uuid);

            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                final String charUuid = gattCharacteristic.getUuid().toString();

                if(gattCharacteristic.getProperties() == 12){
                    CHARACTERISTIC_WRITE_UUID = charUuid;
                    SERVICE_WRITE_UUID = uuid;
                }

                if(gattCharacteristic.getProperties() == 16){
                    for (BluetoothGattDescriptor descriptor:gattCharacteristic.getDescriptors()){
                        CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID = descriptor.getUuid().toString();
                    }
                    setCharacteristicNotification(gattService.getUuid(),gattCharacteristic.getUuid(),true);
                }
            }
        }

    }

    public void setCharacteristicNotification(UUID serviceUuid, UUID characteristicUuid,
                                              boolean enable) {
        BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(serviceUuid).getCharacteristic(characteristicUuid);
        bluetoothGatt.setCharacteristicNotification(characteristic, enable);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR_UUID));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);

        sendToMainActivity(MainActivity2.mBroadcastBleConnected,"","");
    }

    public void sendLongString(String sendString){
        if(isBluetoothConnected){
            if(!isWaitingReply){
                isWaitingReply = true;

                isSendingPartData = true;
                int data_begin = 0;
                int data_end = 15;

                while (isSendingPartData){
                    if(!isWaitingBleReply){
                        if(data_end == sendString.length()){
                            String sendData = sendString.substring(data_begin,data_end)+ "\r\n";
                            writeCustomCharacteristic(sendData);
                            isSendingPartData = false;
                        }else {
                            isWaitingBleReply = true;
                            writeCustomCharacteristic(sendString.substring(data_begin,data_end));
                        }
                        data_begin = data_end;
                        data_end = data_end + 15;
                        if (data_end > sendString.length()){
                            data_end = sendString.length();
                        }
                    }
                }
            }
        }
    }

    public void uploadingBin2(String sendStr){
        if(!isWaitingReply){
            isWaitingReply = true;
            isSendingPartData = true;
            int data_begin = 0;
            int data_end = 15;
            //sendStr = lstSendString.get(currentIdx).sendStr;
            Log.i("MainActivity","sendStr: " + sendStr);
            while (isSendingPartData){
                if(!isWaitingBleReply){
                    if(data_end == sendStr.length()){
                        String sendData = sendStr.substring(data_begin,data_end) + "\r\n";
                        writeCustomCharacteristic(sendData);
                        isSendingPartData = false;
                    }else {
                        isWaitingBleReply = true;
                        writeCustomCharacteristic(sendStr.substring(data_begin,data_end));
                    }
                    data_begin = data_end;
                    data_end = data_end + 15;
                    if (data_end > sendStr.length()){
                        data_end = sendStr.length();
                    }
                }
            }
        }
    }


    public void writeCustomCharacteristic(String message) {
        if(isBluetoothConnected){
            if (btAdapter == null || bluetoothGatt == null) {
                Log.i("MainActivity", "BluetoothAdapter not initialized");
                return;
            }

            BluetoothGattService mCustomService = bluetoothGatt.getService(UUID.fromString(SERVICE_WRITE_UUID));
            if(mCustomService == null){
                Log.w("MainActivity", "Custom BLE Service not found");
                return;
            }

            String originalString = message;
            byte[] b = new byte[message.length()];
            try {
                b = originalString.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(UUID.fromString(CHARACTERISTIC_WRITE_UUID));
            mWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mWriteCharacteristic.setValue(b);
            if(bluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
                Log.i("MainActivity", "Failed to write characteristic");
            }
        }

    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        Log.i("MainActivity","got data available");
        try {
            if(characteristic.getValue()!= null){
                byte[] bytes = characteristic.getValue();
                String str = new String(bytes, "UTF-8");
                Log.i("MainActivity","getValue: " + str);
            }
        }catch (NullPointerException error){
            Log.i("MainActivity","Error: " + error);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


}
