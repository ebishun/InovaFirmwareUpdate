package promosys.com.unarvusoftwareupdate;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FragmentBleScanning extends Fragment {

      private View rootView;
      private Context context;
      private MainActivity2 mainActivity;

      public EditText edtBleName;
      public Button btnConnectBluetooth;
      public ImageView btnStartScanQR;

      public LinearLayout layoutConnecting;

      @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_ble_scanning,container,false);
            context = rootView.getContext();
            mainActivity = (MainActivity2) context;
            edtBleName = (EditText)rootView.findViewById(R.id.edt_ble_name);
            btnConnectBluetooth = (Button)rootView.findViewById(R.id.btn_connect_ble);
            btnConnectBluetooth.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                  btnConnectBluetooth.setBackgroundTintList(getResources().getColorStateList(R.color.color_disabled));
                }
                //mainActivity.SCANNED_MAC_ADDRESS = edtBleName.getText().toString();
                //mainActivity.startScanning();
                mainActivity.connectToBluetooth(edtBleName.getText().toString());
                btnConnectBluetooth.setEnabled(false);
                btnStartScanQR.setEnabled(false);
              }
            });

            btnStartScanQR = (ImageView)rootView.findViewById(R.id.img_start_scan);
            btnStartScanQR.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                mainActivity.startScanningQRcode();
              }
            });


            layoutConnecting = (LinearLayout)rootView.findViewById(R.id.layout_connecting);
            layoutConnecting.setVisibility(View.GONE);

            return rootView;
      }
}
