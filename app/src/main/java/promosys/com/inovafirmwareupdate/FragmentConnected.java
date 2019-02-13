package promosys.com.inovafirmwareupdate;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class FragmentConnected extends Fragment {

    private View rootView;
    private MainActivity2 mainActivity;
    private Context context;
    public ProgressBar progressUpload;
    public TextView txtProgressUpload,txtElapsed,txtCurrentFirmware,txtUpdateVersion,txtDeviceUid;
    private Button btnUploadBin;
    private CardView cardProgress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_connected,container,false);
        context = rootView.getContext();
        mainActivity = (MainActivity2) context;

        cardProgress = (CardView)rootView.findViewById(R.id.card_progress);

        progressUpload = (ProgressBar)rootView.findViewById(R.id.progress_upload);
        txtProgressUpload = (TextView)rootView.findViewById(R.id.txt_upload_progress);
        txtElapsed = (TextView)rootView.findViewById(R.id.txt_elapsed);
        txtCurrentFirmware = (TextView)rootView.findViewById(R.id.txt_current_firmware);
        txtUpdateVersion = (TextView)rootView.findViewById(R.id.txt_firmware_version);
        txtDeviceUid = (TextView)rootView.findViewById(R.id.txt_device_uid);

        txtProgressUpload.setText("0%");
        progressUpload.setProgress(0);


        btnUploadBin = (Button)rootView.findViewById(R.id.btn_upload_bin);
        btnUploadBin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.requestToUploadBin();
                //mainActivity.startUploadingBin();
                setButtonStatus(false);

            }
        });

        return rootView;
    }

    public void setButtonStatus(boolean isEnabled){
        if(isEnabled){
            btnUploadBin.setEnabled(true);
            btnUploadBin.setBackgroundTintList(getResources().getColorStateList(R.color.color_enabled));

        }else {
            btnUploadBin.setEnabled(false);
            btnUploadBin.setBackgroundTintList(getResources().getColorStateList(R.color.color_disabled));
        }
    }

    public void hideButton(boolean isHide){
        if(isHide){
            btnUploadBin.setVisibility(View.GONE);
            cardProgress.setVisibility(View.VISIBLE);
            progressUpload.setProgressDrawable(getActivity().getResources().getDrawable(R.drawable.progressbarstyleone));
            progressUpload.setProgress(0);
            txtProgressUpload.setText("0%");
        }else {
            btnUploadBin.setVisibility(View.VISIBLE);
            cardProgress.setVisibility(View.GONE);
        }
    }

    public void setElapsedTime(String strElapsedTime){
        txtElapsed.setText("Time Elapsed: "+strElapsedTime);
    }

    public void setCurrentFirmware(String firmware,String updatedFirmware) {
        txtCurrentFirmware.setText(firmware);
        txtUpdateVersion.setText(updatedFirmware);
    }
}
