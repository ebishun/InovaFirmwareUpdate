package promosys.com.unarvusoftwareupdate;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.renderscript.ScriptGroup;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.blikoon.qrcodescanner.QrCodeActivity;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity2 extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 2;
    private static final int REQUEST_CODE_QR_SCAN = 101;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_TURN_ON_BLUETOOTH = 300;
    private static final int REQUEST_SWITCH_ON_LOCATION = 200;

    public static final String mBroadcastBleOff = "promosys.com.unarvusettings.bleoff";
    public static final String mBroadcastBleDisabled = "promosys.com.unarvusettings.bledisabled";
    public static final String mBroadcastBleConnected = "promosys.com.unarvusettings.bleconnected";
    public static final String mBroadcastBleDisconnected = "promosys.com.unarvusettings.bledisconnected";
    public static final String mBroadcastBleGotReply = "promosys.com.unarvusettings.blegotreply";
    public static final String mBroadcastBleDeviceNotFound = "promosys.com.unarvusettings.blenotfound";
    public static final String mBroadcastConnectionEstablished = "promosys.com.unarvusettings.bleestablished";
    public static final String mBroadcastFailedCharacteristics = "promosys.com.unarvusettings.failedcharacteristics";
    public static final String mBroadcastCheckAlive = "promosys.com.unarvusettings.checkalive";
    private IntentFilter mIntentFilter;

    private FragmentBleScanning2 fragmentScanning;
    private FragmentTransaction fragmentTransaction;
    private FragmentConnected fragmentConnectedMain;

    private boolean isFragmentScanning = false;
    private boolean isFragmentConnectedMain = false;

    private String FRAGMENT_SCANNING = "fragmentScanning";
    private String FRAGMENT_CONNECTED_MAIN = "fragmentConnectedMain";

    private MyBleService mBleService;
    private boolean mIsBound = false;

    private Toolbar toolbar;
    private long timeElapsed;
    private long startTime = 100;
    private long interval = 100;

    private MyRefreshTimer refreshTimer;
    char ETX = (char)0x03;

    private boolean isRequestFirmware = false;

    byte[] tempBytes;

    private int binSize = 0;
    private int begin = 0;
    private int end = 0;

    private boolean isUploadingBin = false;
    long startUpload = 0;
    long endUpload = 0;
    long elapsedMilliSecondsUpload = 0;

    int currentIdx = 0;

    StringBuffer strbuffBinFile = new StringBuffer();
    private int intDataLength = 128;
    private int portionByteLength = 100;
    public boolean isFlashStart = false;

    StringBuffer strBleBuffer = new StringBuffer();
    private boolean isTimerBleReply = false;
    private int intTimerCounter = 0;

    InputStream inputFile, inputA,inputB;
    String fileName = "";
    String fileNameA = "unlv1_0e03_v0_17a";
    String fileNameB = "unlv1_0e03_v0_17b";


    private boolean isCurrentFirmwareA = false;
    private boolean isWaitFirmware = false;
    private boolean isBleConnected = false;
    private boolean isRequestDisconnect = false;

    ArrayList<StringObject> lstSendString;
    private int totalMessage = 0;

    private boolean isBroadcastRegitered = false;
    private Menu myMenu;

    private int targetedStartTime = 20;
    private int targetedInterval = 20;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setSubtitleTextColor(Color.WHITE);
        getSupportActionBar().hide();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        refreshTimer = new MyRefreshTimer(startTime, interval);

        inputA = getResources().openRawResource(R.raw.unlv1_0e03_v0_17a);
        inputB = getResources().openRawResource(R.raw.unlv1_0e03_v0_17b);

        initIntentFilter();
        initPermission();
        initFragment();
        initLocation();
        //checkLocationSettings();

        //0E030601
    }

    private void initIntentFilter(){
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcastBleDisabled);
        mIntentFilter.addAction(mBroadcastBleConnected);
        mIntentFilter.addAction(mBroadcastBleDisconnected);
        mIntentFilter.addAction(mBroadcastBleGotReply);

        mIntentFilter.addAction(mBroadcastBleDeviceNotFound);
        mIntentFilter.addAction(mBroadcastConnectionEstablished);
        mIntentFilter.addAction(mBroadcastFailedCharacteristics);
        mIntentFilter.addAction(mBroadcastCheckAlive);
    }

    private void initFragment(){

        fragmentConnectedMain = new FragmentConnected();
        fragmentScanning = new FragmentBleScanning2();

        fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.container,fragmentScanning,"fragmentScanning");
        fragmentTransaction.add(R.id.container,fragmentConnectedMain,"fragmentConnectedMain");
        fragmentTransaction.hide(fragmentConnectedMain);
        fragmentTransaction.commitAllowingStateLoss();

        isFragmentScanning = true;
    }

    private void initLocation() {
        //checkLocationProviders();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API).build();
        mGoogleApiClient.connect();
    }

    private void checkLocationSettings(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100000);
        mLocationRequest.setFastestInterval(100000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                //final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i("MainActivity","Location is on");
                        break;

                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity2.this, REQUEST_SWITCH_ON_LOCATION);
                        } catch (IntentSender.SendIntentException e) {}
                        break;

                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        //...
                        break;
                }
            }
        });
    }



    @Override
    protected void onDestroy() {
        doUnbindService();
        unregisterReceiver(mReceiver);
        if (isMyServiceRunning(MyBleService.class)){
            stopService(new Intent(this, MyBleService.class));
        }
        super.onDestroy();
    }

    void doBindService() {

        bindService(new Intent(this, MyBleService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Toast.makeText(getApplicationContext(),"Service is connected",Toast.LENGTH_SHORT).show();
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    //Check whether the listener service is running or not in the background
    //If the service not running, the apps will restart the service on launch
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBleService = ((MyBleService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBleService = null;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if(!isBroadcastRegitered){
            isBroadcastRegitered = true;
            registerReceiver(mReceiver, mIntentFilter);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.myMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(isFragmentConnectedMain){
            isRequestDisconnect = true;
            if(isBleConnected){
                if(!isUploadingBin){
                    dialogDisconnect("Disconnect?");
                }else{
                    dialogDisconnect("Firmware uploading is in progress. Disconnect anyway?");
                }
            }else {
                isRequestDisconnect = true;
                changeFragment(FRAGMENT_SCANNING);
            }
        }else {
            this.finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            Log.i("MainActivity","back button is clicked");
            Log.i("MainActivity","isBleConnected: " + isBleConnected);
            if(isBleConnected){
                if(!isUploadingBin && !mBleService.isWaitingReply){
                    isRequestDisconnect = true;
                    mBleService.disconnectDeviceSelected();
                }else {
                    dialogDisconnect("Firmware uploading is in progress. Disconnect anyway?");
                }
            }else {
                isRequestDisconnect = true;
                changeFragment(FRAGMENT_SCANNING);
            }

        }else {
            if(!isBleConnected){
                reconnectToBluetooth();
            }

        }

        return super.onOptionsItemSelected(item);
    }

    private void reconnectToBluetooth(){
        toolbar.setSubtitle("Connecting...");
        mBleService.startScanning();
    }

    private void dialogDisconnect(String strMessage){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity2.this).create();
        alertDialog.setMessage(strMessage);
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        refreshTimer.cancel();

                        Handler myhandler = new Handler();
                        myhandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isRequestDisconnect = true;
                                mBleService.disconnectDeviceSelected();

                            }
                        }, 100);
                        dialog.dismiss();
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "RESUME", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    public void startScanningQRcode(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Camera access");
                builder.setMessage("Camera access is needed in order to scan the QR code.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
                        }
                    }
                });
                builder.show();
            }else {
                Intent i = new Intent(MainActivity2.this,QrCodeActivity.class);
                startActivityForResult(i,REQUEST_CODE_QR_SCAN);
            }
        }
    }

    //QR code scanner activity callback
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_SWITCH_ON_LOCATION){
            Log.i("MainActivity","REQUEST_SWITCH_ON_LOCATION");
            if(resultCode == Activity.RESULT_OK){
                Log.i("MainActivity","User switch on location");
                //initLocation();
            }else {
                Log.i("MainActivity","User didn't switch on location");
            }

        } else if(resultCode != Activity.RESULT_OK)
        {
            Log.i("MainActivity","COULD NOT GET A GOOD RESULT.");
            if(data==null)
                return;
            //Getting the passed result
            String result = data.getStringExtra("com.blikoon.qrcodescanner.error_decoding_image");
            if( result!=null)
            {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity2.this).create();
                alertDialog.setTitle("Scan Error");
                alertDialog.setMessage("QR Code could not be scanned");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
            return;
        }

        if(requestCode == REQUEST_CODE_QR_SCAN)
        {
            if(data==null)
                return;
            String result = data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
            Log.i("MainActivity","Have scan result in your app activity :"+ result);
            fragmentScanning.edtMacAdress.setText(result);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("This app needs location access");
                    builder.setMessage("Please grant location access so this app can detect BLE devices.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[]{ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_COARSE_LOCATION);
                            }
                        }
                    });
                    builder.show();

                }else{
                    fragmentScanning.simulateProgress2();
                }
            }
        }
    }

    //Broadcast receiver from bluetooth service
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())){

                case mBroadcastBleDisabled:
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                    break;

                case mBroadcastBleConnected:
                    /*
                    Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
                    changeFragment(FRAGMENT_CONNECTED_MAIN);
                    fragmentConnectedMain.setButtonStatus(true);
                    fragmentConnectedMain.hideButton(false);
                    myMenu.getItem(0).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.ic_action_bluetooth_on));
                    toolbar.setSubtitle("Status: Connected");
                    isRequestFirmware = true;
                    isBleConnected = true;
                    refreshTimer.start();
                    */

                    inputA = getResources().openRawResource(R.raw.unlv1_0e03_v0_17a);
                    inputB = getResources().openRawResource(R.raw.unlv1_0e03_v0_17b);

                    binSize = 0;
                    begin = 0;
                    end = 0;

                    currentIdx = 0;

                    Log.i("MainActivity","mBroadcastBleConnected");
                    toolbar.setSubtitle("Connected");
                    myMenu.getItem(0).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.ic_action_bluetooth_on));

                    isBleConnected = true;
                    isRequestDisconnect = false;
                    mBleService.isWaitingReply = false;
                    mBleService.isSendingPartData = false;
                    isRequestFirmware = true;

                    changeFragment(FRAGMENT_CONNECTED_MAIN);

                    fragmentConnectedMain.setButtonStatus(true);
                    fragmentConnectedMain.hideButton(false);

                    strbuffBinFile = new StringBuffer();

                    Handler myhandler = new Handler();
                    myhandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            refreshTimer.start();
                        }
                    }, 100);

                    break;

                case mBroadcastBleDisconnected:
                    /*
                    Toast.makeText(getApplicationContext(),"Disconnected",Toast.LENGTH_SHORT).show();
                    toolbar.setSubtitle("Status: Disconnected");

                    fragmentConnectedMain.setCurrentFirmware("N/A","N/A");
                    fragmentConnectedMain.setButtonStatus(false);

                    refreshTimer.cancel();

                    myMenu.getItem(0).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.ic_action_bluetooth_off));
                    changeFragment(FRAGMENT_SCANNING);
                    isBleConnected = false;
                    */

                    binSize = 0;
                    begin = 0;
                    end = 0;

                    currentIdx = 0;

                    Toast.makeText(getApplicationContext(),"Disconnected",Toast.LENGTH_SHORT).show();
                    myMenu.getItem(0).setIcon(ContextCompat.getDrawable(getApplicationContext(), R.mipmap.ic_action_bluetooth_off));

                    refreshTimer.cancel();
                    intTimerCounter = 0;
                    isTimerBleReply = false;

                    isBleConnected = false;
                    toolbar.setSubtitle("Disconnected");

                    mBleService.isSendingPartData = false;
                    mBleService.isWaitingBleReply = false;

                    //fragmentScanning.hideLayoutConnected();
                    fragmentScanning.enableBtnConnectBle(true);
                    fragmentScanning.refreshButton();

                    /*
                    if(initializingDialog != null){
                        initializingDialog.dismiss();
                    }
                    */

                    mBleService.disconnectDeviceSelected();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(isRequestDisconnect){
                                isRequestDisconnect = false;
                                changeFragment(FRAGMENT_SCANNING);
                                //disconnectingDialog.dismiss();
                                //disconnectDialog.dismiss();
                            }
                        }
                    }, 20);

                    break;

                case mBroadcastBleGotReply:
                    processBleReply(intent.getStringExtra("bleMessage"));
                    break;

                case mBroadcastBleDeviceNotFound:
                    //fragmentScanning.hideLayoutConnected();
                    fragmentScanning.enableBtnConnectBle(true);
                    fragmentScanning.deviceNotFound();

                    toolbar.setSubtitle("Device Not Found");

                    fragmentScanning.refreshButton();

                    mBleService.disconnectDeviceSelected();
                    fragmentScanning.refreshButton();
                    break;

                case mBroadcastConnectionEstablished:
                    if(isFragmentScanning){
                        changeFragment(FRAGMENT_CONNECTED_MAIN);
                    }
                    break;

                case mBroadcastFailedCharacteristics:
                    Log.i("MainActivity","failed to write characteristics");
                    refreshTimer.cancel();
                    break;

                case mBroadcastBleOff:
                    Toast.makeText(getApplicationContext(),"Bluetooth is off. Please enable phone's bluetooth.",Toast.LENGTH_SHORT).show();
                    fragmentScanning.enableBtnConnectBle(true);
                    //progressLoading.setVisibility(View.GONE);

                    Intent eintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(eintent, REQUEST_TURN_ON_BLUETOOTH);
                    fragmentScanning.refreshButton();

                    break;
            }

        }
    };

    private void processBleReply(String bleReply){
        Log.i("MainActivity","bleReply: " + bleReply);
        if(bleReply.contains("|")){
            final String[] parts = bleReply.split("\\|");
                    //if(isUploadingBin){
            if(bleReply.contains("FLASHWRITE")){

                Log.i("MainActivity","Received Index: " + parts[2]);

                try {
                    int receivedIndex = Integer.parseInt(parts[2]);
                    buildBinFile(receivedIndex);
                    if (receivedIndex == currentIdx){
                        currentIdx = currentIdx + 1;
                    }

                    mBleService.isWaitingReply = false;
                    isTimerBleReply = false;
                    isUploadingBin = true;
                    intTimerCounter = 0;
                    totalMessage = totalMessage + 1;

                    if (currentIdx ==lstSendString.size()){
                        Log.i("MainActivity","Finish sending");

                        //fragmentConnectedMain.progressUpload.setProgress(currentIdx);
                        //fragmentConnectedMain.txtProgressUpload.setText("100%");
                        //convertToBinFile();
                        new BuildBinFile().execute("");
                        isUploadingBin = false;
                        Log.i("MainActivity","Done converting");
                        fragmentConnectedMain.setButtonStatus(true);
                        refreshTimer.cancel();

                    }else {
                        begin = end;
                        end = end + 100;
                        if(end>binSize){
                            end = binSize;
                            portionByteLength = end - begin;
                        }
                        fragmentConnectedMain.progressUpload.setProgress(currentIdx);
                        String progressPercentage = String.valueOf(convertToPercentage((currentIdx-1), lstSendString.size())).replace(".0","") + " %";
                        fragmentConnectedMain.txtProgressUpload.setText(progressPercentage);
                    }
                }catch (NumberFormatException e){
                    mBleService.isWaitingReply = false;
                    isTimerBleReply = false;
                    isUploadingBin = true;
                    intTimerCounter = 0;
                }


            }else if(bleReply.contains("ERROR")){
                mBleService.isWaitingReply = false;

            }else if(bleReply.contains("GETFULLUID")){
                Log.i("MainActivity","GETFULLUID: " + parts[2]);
                intTimerCounter = 0;
                isRequestFirmware = false;
                mBleService.isWaitingReply = false;
                fragmentConnectedMain.txtDeviceUid.setText(parts[2]);

            }else if(bleReply.contains("GETVERSION")){
                isWaitFirmware = false;
                intTimerCounter = 0;
                isRequestFirmware = false;
                mBleService.isWaitingReply = false;
                if(parts[2].contains("_")){
                    final String[] subParts = parts[2].split("_");
                    for (int i = 0;i<subParts.length;i++){
                        Log.i("MainActivity","subParts "+ i +": "+ subParts[i]);
                    }
                    int whichPart = subParts.length - 1;
                    Log.i("SubParts","subPart: " + subParts[whichPart]);
                    if(subParts[whichPart].contains("a")){
                        isCurrentFirmwareA = true;
                        fragmentConnectedMain.setCurrentFirmware(parts[2],fileNameB);
                    }else {
                        isCurrentFirmwareA = false;
                        fragmentConnectedMain.setCurrentFirmware(parts[2],fileNameA);
                    }
                }
                //String strData = buildStringToDevice("|GETVERSION|");
                String strData = buildStringToDevice("|GETFULLUID|");
                mBleService.sendLongString(strData);

            }else if(bleReply.contains("FLASHSTART")){
                if(bleReply.contains("OK")){
                    fragmentConnectedMain.hideButton(true);
                    isFlashStart = false;
                    mBleService.isWaitingReply = false;
                    startUploadingBin();
                }
            }else if(bleReply.contains("FLASHBOOT")){
                if(bleReply.contains("OK")){
                    fragmentConnectedMain.progressUpload.setProgressDrawable(getResources().getDrawable(R.drawable.progress_complete));
                    fragmentConnectedMain.progressUpload.setProgress(currentIdx);
                    fragmentConnectedMain.txtProgressUpload.setText("100%");
                    fragmentConnectedMain.txtElapsed.setText("Update Completed");
                    currentIdx = 0;
                }
            }
        }
    }

    public void requestToUploadBin(){
        isFlashStart = true;
        if(isCurrentFirmwareA){
            fileName = fileNameB;
            inputFile = inputB;
        }else {
            fileName = fileNameA;
            inputFile = inputA;
        }

        try {
            binSize = (int) inputFile.available();
        } catch (Exception e) {
            e.printStackTrace();
        }
        tempBytes = new byte[binSize];
        refreshTimer.start();
    }

    public void startUploadingBin(){
        startTime = targetedStartTime;
        interval = targetedInterval;
        refreshTimer = new MyRefreshTimer(startTime, interval);
        lstSendString = new ArrayList<StringObject>();
        mBleService.isWaitingReply = false;
        try {
            BufferedInputStream buf = new BufferedInputStream(inputFile);
            buf.read(tempBytes,0,tempBytes.length);
            buf.close();
            Log.i("MainActivity","BinFile Length: " + tempBytes.length);
            new LongOperation().execute("");

            begin = 0;
            end = intDataLength;
            portionByteLength = intDataLength;
            isUploadingBin = true;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        checkLocationSettings();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            insertStringIntoList();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("MainActivity","Done inserting list");
            fragmentConnectedMain.progressUpload.setMax(lstSendString.size()+1);
            fragmentConnectedMain.progressUpload.setProgress(0);
            refreshTimer.start();
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private void insertStringIntoList(){
        boolean isAdding = true;
        int idxString = 0;

        int lstBegin = 0;
        int lstEnd = intDataLength;
        int lstPortionByteLength = intDataLength;
        startUpload = SystemClock.elapsedRealtime();

        while (isAdding){
            byte[] portionByte = new byte[lstPortionByteLength];
            int counter = 0;
            StringBuffer sbStr = new StringBuffer();
            for(int i = lstBegin; i<lstEnd;i++){
                portionByte[counter] = tempBytes[i];
                String thisByte = "".format("%02x", tempBytes[i]);
                sbStr.append(thisByte);
                counter = counter + 1;
            }

            String hexLength = getLength("|FLASHWRITE|" +idxString +"|"+ sbStr.toString() + "|");
            String getCrc = "$" + hexLength + "|FLASHWRITE|" + idxString +"|"+ sbStr.toString() + "|";
            String strCrc = ModRTU_CRC(getCrc.getBytes());
            String sendStr = "$" + hexLength + "|FLASHWRITE|" + idxString +"|" + sbStr.toString() + "|" + strCrc + ETX;

            StringObject strObject = new StringObject(idxString,sendStr);
            lstSendString.add(strObject);

            idxString = idxString + 1;
            if(lstEnd == binSize){
                isAdding = false;
            }else {
                lstBegin = lstEnd;
                lstEnd = lstEnd + intDataLength;
                if(lstEnd>binSize){
                    lstEnd = binSize;
                    lstPortionByteLength = lstEnd - lstBegin;
                }
            }
        }
        Log.i("MainActivity","ListSize: " + lstSendString.size());

    }

    private void elapsedTime(){
        endUpload = SystemClock.elapsedRealtime();
        elapsedMilliSecondsUpload = endUpload - startUpload;

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.setTimeInMillis(c.getTimeInMillis() + elapsedMilliSecondsUpload);
        DateFormat df = new SimpleDateFormat("HH:mm:ss");
        fragmentConnectedMain.setElapsedTime(df.format(c.getTime()));
    }

    private float convertToPercentage(int current,int total){
        float f_current = (float) current;
        float f_total = (float) total;
        int percentage = (int) ((f_current/f_total)*100);
        return percentage;
    }

    private void buildBinFile(int currentIndex){
        String strData = lstSendString.get(currentIndex).sendStr;
        //String strData = currentIndex;
        final String[] parts = strData.split("\\|");
        strbuffBinFile.append(parts[3]);
    }

    private void convertToBinFile(){
        Log.i("MainActivity","strbuffBinFile: " + strbuffBinFile.toString());
        byte[] portionByte = hexStringToByteArray(strbuffBinFile.toString());

        Log.i("MainActivity","binSize: " + binSize);
        Log.i("MainActivity","portionByte.length: " + portionByte.length);
        if(portionByte.length == binSize){
            String strData = buildStringToDevice("|FLASHBOOT|");
            mBleService.sendLongString(strData);
        }else {
            Toast.makeText(this,"Error while uploading. Please try again.",Toast.LENGTH_SHORT).show();
            fragmentConnectedMain.hideButton(false);
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private class BuildBinFile extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            convertToBinFile();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("MainActivity","Bin File Ready");
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


    private void initPermission(){
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();
            }else {
                Log.i("MainActivity","isServiceRunning: " +isMyServiceRunning(MyBleService.class));
                if (!(isMyServiceRunning(MyBleService.class))){
                    startService(new Intent(this, MyBleService.class));
                }
                doBindService();
            }
        }
    }

    private void changeFragment(String whichFragment){
        fragmentTransaction = getFragmentManager().beginTransaction();

        switch (whichFragment){
            case "fragmentScanning":
                Log.i("MainActivity","isFragmentScanning: " + isFragmentScanning);
                if(!isFragmentScanning){
                    getSupportActionBar().hide();

                    mBleService.SCANNED_MAC_ADDRESS = "";
                    fragmentTransaction.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
                    isFragmentConnectedMain = false;
                    isFragmentScanning = true;

                    fragmentScanning.enableBtnConnectBle(true);
                    //progressLoading.setVisibility(View.GONE);
                    fragmentTransaction.hide(fragmentConnectedMain);
                    fragmentTransaction.show(fragmentScanning);
                }

                break;

            case "fragmentConnectedMain":
                fragmentTransaction.setCustomAnimations(R.animator.slide_in_left, R.animator.slide_out_right);
                isFragmentScanning = false;
                isFragmentConnectedMain = true;

                getSupportActionBar().show();

                fragmentTransaction.hide(fragmentScanning);
                fragmentTransaction.show(fragmentConnectedMain);
                break;
        }

        fragmentTransaction.commitAllowingStateLoss();
    }

    public void connectToBluetooth(String macAddress){
        mBleService.SCANNED_MAC_ADDRESS = macAddress;
        mBleService.startScanning();
    }

    private String buildStringToDevice(String strCommand){
        String finalString = "";
        String length = "$" + getLength(strCommand);
        String strGetCrc = length + strCommand;
        String strCrc = ModRTU_CRC(strGetCrc.getBytes());

        finalString = strGetCrc + strCrc + ETX;
        Log.i("MainActivity","finalString: " + finalString);

        return finalString;
    }

    public void scanForNearbyDevice(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect BLE devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();
            }else{
                mBleService.isScanForNearbyDevice = true;
                connectToBluetooth(fragmentScanning.edtMacAdress.getText().toString());
            }
        }
    }

    //auto-refresh timer class
    public class MyRefreshTimer extends CountDownTimer
    {
        public MyRefreshTimer(long startTime, long interval)
        {
            super(startTime, interval);
        }

        @Override
        public void onFinish()
        {
            refreshTimer.cancel();

            if(isUploadingBin){
                Log.i("MainActivity","Uploading bin");
                mBleService.uploadingBin2(lstSendString.get(currentIdx).sendStr);
                isUploadingBin = false;
                isTimerBleReply = true;
                elapsedTime();
                refreshTimer.start();

            }else if(isRequestFirmware){
                isRequestFirmware = false;
                isWaitFirmware = true;
                String strData = buildStringToDevice("|GETVERSION|");
                //String strData = buildStringToDevice("|GETFULLUID|");
                mBleService.sendLongString(strData);
                refreshTimer.start();

            }else if(isFlashStart){
                //String flashRequest = "|FLASHSTART|" + "SmartCity_Bin_V2_v0.08" + "|" + String.valueOf(binSize) + "|";
                String flashRequest = "|FLASHSTART|" + fileName + "|" + String.valueOf(binSize) + "|";
                String strData = buildStringToDevice(flashRequest);
                mBleService.sendLongString(strData);
                fragmentConnectedMain.setElapsedTime("Initializing...");

            }else if(isWaitFirmware){
                if(intTimerCounter == 5){
                    intTimerCounter = 0;
                    mBleService.isWaitingReply = false;

                    isRequestFirmware = true;
                    isWaitFirmware = false;
                }else {
                    intTimerCounter = intTimerCounter + 1;
                }
                refreshTimer.start();

            }else if(isTimerBleReply){
                if(intTimerCounter == 50){
                    intTimerCounter = 0;
                    mBleService.isWaitingReply = false;

                    isUploadingBin = true;
                    isTimerBleReply = false;
                }else {
                    intTimerCounter = intTimerCounter + 1;
                }
                refreshTimer.start();
            }
        }

        @Override
        public void onTick(long millisUntilFinished)
        {
            timeElapsed = startTime - millisUntilFinished;
        }
    }
/*
    private void sendLongString(String sendString){
        if(!isWaitingReply){
            isWaitingReply = true;

            isSendingPartData = true;
            int data_begin = 0;
            int data_end = 15;

            while (isSendingPartData){
                if(!isWaitingBleReply){
                    if(data_end == sendString.length()){
                        String sendData = sendString.substring(data_begin,data_end)+ "\r\n";
                        mBleService.writeCustomCharacteristic(sendData);
                        isSendingPartData = false;
                    }else {
                        isWaitingBleReply = true;
                        mBleService.writeCustomCharacteristic(sendString.substring(data_begin,data_end));
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
*/
    private String getLength(String strLength){
        String hexLength = "";
        hexLength = String.format("%04X", strLength.length());
        return hexLength;
    }

    private static String ModRTU_CRC(byte[] buf)
    {
        int crc = 0xFFFF;

        for (int pos = 0; pos < buf.length; pos++) {
            crc ^= (int)buf[pos] & 0xFF;   // XOR byte into least sig. byte of crc

            for (int i = 8; i != 0; i--) {    // Loop over each bit
                if ((crc & 0x0001) != 0) {      // If the LSB is set
                    crc >>= 1;                    // Shift right and XOR 0xA001
                    crc ^= 0xA001;
                }
                else                            // Else LSB is not set
                    crc >>= 1;                    // Just shift right
            }
        }
        return Integer.toHexString(crc);
    }


}
