package promosys.com.inovafirmwareupdate;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.IntegerRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.dd.morphingbutton.MorphingButton;
import com.dd.morphingbutton.impl.IndeterminateProgressButton;

public class FragmentBleScanning2 extends Fragment {

    private View rootView;
    public EditText edtMacAdress;
    private TextView txtMainName;
    private MainActivity2 mainActivity;
    private Context context;

    private IndeterminateProgressButton btnConnectBle;
    private IndeterminateProgressButton btnScanQr;
    private IndeterminateProgressButton btnScanNearby;

    private boolean isConnectBtnPressed = false;
    private boolean isScanQrBtnPressed = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_ble_scanning2,container,false);
        context = rootView.getContext();
        mainActivity = (MainActivity2) context;
        txtMainName = (TextView)rootView.findViewById(R.id.txt_title_name);

        String appName = "Firmware Update\n v" + context.getResources().getString(R.string.app_version);
        txtMainName.setText(appName);

        edtMacAdress = (EditText)rootView.findViewById(R.id.edt_ble_name);
        //btnConnectBle = (Button)rootView.findViewById(R.id.btn_connect_ble);
        btnConnectBle = (IndeterminateProgressButton) rootView.findViewById(R.id.btn_connect_ble);
        btnConnectBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                morphToSquare(btnScanQr,false,"Scan QR",R.color.color_disabled);
                morphToSquare(btnScanNearby,false,"Scan Nearby Device",R.color.color_disabled);
                simulateProgress(btnConnectBle);
                mainActivity.connectToBluetooth(processEnteredName(edtMacAdress.getText().toString()));
                isConnectBtnPressed = true;
                //onMorphButton1Clicked(btnConnectBle);
            }
        });

        //btnScanQr = (ImageView)rootView.findViewById(R.id.img_start_scan);
        btnScanQr = (IndeterminateProgressButton) rootView.findViewById(R.id.btn_scan_qr);
        btnScanQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isScanQrBtnPressed = true;
                morphToSquare(btnConnectBle,false,"Connect",R.color.color_disabled);
                morphToSquare(btnScanNearby,false,"Scan Nearby Device",R.color.color_disabled);
                mainActivity.startScanningQRcode();
            }
        });

        btnScanNearby = (IndeterminateProgressButton) rootView.findViewById(R.id.btn_scan_nearby);
        btnScanNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                morphToSquare(btnConnectBle,false,"Connect",R.color.color_disabled);
                morphToSquare(btnScanQr,false,"Scan QR",R.color.color_disabled);
                simulateProgress(btnScanNearby);
                mainActivity.scanForNearbyDevice();
            }
        });

        morphToSquare(btnConnectBle, true,"Connect",R.color.colorAccent);
        morphToSquare(btnScanQr, true,"Scan QR",R.color.colorAccent);
        morphToSquare(btnScanNearby, true,"Scan Nearby Device",R.color.colorAccent);
        return rootView;
    }

    private String processEnteredName(String strName){
        if(strName.contains("-")){
            strName = strName.replace("-","");
        }

        if(strName.contains(" ")){
            strName = strName.replace(" ","");
        }

        if(strName.contains("o") || strName.contains("O")){
            strName = strName.replace("o","0");
            strName = strName.replace("O","0");
        }
        strName = strName.toUpperCase();
        return strName;
    }

    public void enableBtnConnectBle(boolean isEnable){
        Log.i("FragmentScanning","btnConnectBle: " + isEnable);
        if(isEnable){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                btnConnectBle.setEnabled(true);
            }
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                btnConnectBle.setEnabled(false);
            }
        }
    }

    private void simulateProgress(final IndeterminateProgressButton button){
        int progressColor1 = color(R.color.holo_blue_bright);
        int progressColor2 = color(R.color.holo_green_light);
        int progressColor3 = color(R.color.holo_orange_light);
        int progressColor4 = color(R.color.holo_red_light);
        int color = color(R.color.mb_gray);
        int progressCornerRadius = dimen(R.dimen.mb_corner_radius_4);
        int width = dimen(R.dimen.mb_width_200);
        int height = dimen(R.dimen.mb_height_8);
        int duration = integer(R.integer.mb_animation);

        button.blockTouch(); // prevent user from clicking while button is in progress
        button.morphToProgress(color, progressCornerRadius, width, height, duration, progressColor1, progressColor2,
                progressColor3, progressColor4);
    }


    private void simulateProgress1(final IndeterminateProgressButton button) {
        int progressColor1 = color(R.color.holo_blue_bright);
        int progressColor2 = color(R.color.holo_green_light);
        int progressColor3 = color(R.color.holo_orange_light);
        int progressColor4 = color(R.color.holo_red_light);
        int color = color(R.color.mb_gray);
        int progressCornerRadius = dimen(R.dimen.mb_corner_radius_4);
        int width = dimen(R.dimen.mb_width_200);
        int height = dimen(R.dimen.mb_height_8);
        int duration = integer(R.integer.mb_animation);

        button.blockTouch(); // prevent user from clicking while button is in progress
        button.morphToProgress(color, progressCornerRadius, width, height, duration, progressColor1, progressColor2,
                progressColor3, progressColor4);

        mainActivity.connectToBluetooth(edtMacAdress.getText().toString());
    }

    public void simulateProgress2() {
        int progressColor1 = color(R.color.holo_blue_bright);
        int progressColor2 = color(R.color.holo_green_light);
        int progressColor3 = color(R.color.holo_orange_light);
        int progressColor4 = color(R.color.holo_red_light);
        int color = color(R.color.mb_gray);
        int progressCornerRadius = dimen(R.dimen.mb_corner_radius_4);
        int width = dimen(R.dimen.mb_width_200);
        int height = dimen(R.dimen.mb_height_8);
        int duration = integer(R.integer.mb_animation);

        btnScanQr.blockTouch(); // prevent user from clicking while button is in progress
        btnScanQr.morphToProgress(color, progressCornerRadius, width, height, duration, progressColor1, progressColor2,
                progressColor3, progressColor4);

        mainActivity.connectToBluetooth(edtMacAdress.getText().toString());
    }

    public void displayBluetoothName(String name){
        edtMacAdress.setText(name);
    }

    public void morphToError(final IndeterminateProgressButton btnMorph,String message) {

        MorphingButton.Params square = MorphingButton.Params.create()
                //.duration(100)
                .cornerRadius(dimen(R.dimen.mb_corner_radius_4))
                .width(dimen(R.dimen.mb_width_200))
                .height(dimen(R.dimen.mb_height_56))
                .color(color(R.color.colorError))
                .colorPressed(color(R.color.mb_blue_dark))
                .text(message);
        btnMorph.morph(square);
        btnMorph.unblockTouch();
    }

    private void morphToSquare(final IndeterminateProgressButton btnMorph, boolean enable, String buttonText, int myColor) {
        MorphingButton.Params square = MorphingButton.Params.create()
                //.duration(duration)
                .cornerRadius(dimen(R.dimen.mb_corner_radius_4))
                .width(dimen(R.dimen.mb_width_200))
                .height(dimen(R.dimen.mb_height_56))
                //.color(color(R.color.colorAccent))
                .color(color(myColor))
                .colorPressed(color(R.color.mb_blue_dark))
                .text(buttonText);
        btnMorph.morph(square);
        if(enable){
            btnMorph.unblockTouch();
        }else {
            btnMorph.blockTouch();
        }
    }

    public void deviceNotFound() {
        if(isConnectBtnPressed){
            isConnectBtnPressed = false;
            morphToError(btnConnectBle,"Device Not Found");
            morphToSquare(btnScanNearby,true,"Scan Nearby",R.color.colorAccent);
            morphToSquare(btnScanQr,true,"Scan QR",R.color.colorAccent);
        }else if(isScanQrBtnPressed){
            isScanQrBtnPressed = false;
            morphToError(btnScanQr,"Device Not Found");
            morphToSquare(btnScanNearby,true,"Scan Nearby",R.color.colorAccent);
            morphToSquare(btnConnectBle,true,"Connect",R.color.colorAccent);
        }else {
            morphToError(btnScanNearby,"Device Not Found");
            morphToSquare(btnScanQr,true,"Scan QR",R.color.colorAccent);
            morphToSquare(btnConnectBle,true,"Connect",R.color.colorAccent);
        }
    }

    public void refreshButton(){
        morphToSquare(btnConnectBle,true,"Connect",R.color.colorAccent);
        morphToSquare(btnScanQr,true,"Scan QR",R.color.colorAccent);
        morphToSquare(btnScanNearby,true,"Scan Nearby Device",R.color.colorAccent);

        btnConnectBle.unblockTouch();
        btnScanQr.unblockTouch();
        btnScanNearby.unblockTouch();
    }

    public int dimen(@DimenRes int resId) {
        return (int) getResources().getDimension(resId);
    }

    public int color(@ColorRes int resId) {
        return getResources().getColor(resId);
    }

    public int integer(@IntegerRes int resId) {
        return getResources().getInteger(resId);
    }


}

