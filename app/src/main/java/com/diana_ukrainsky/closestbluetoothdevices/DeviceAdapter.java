package com.diana_ukrainsky.closestbluetoothdevices;

import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MyViewHolder> {


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView deviceItem_TXT_deviceName, deviceItem_TXT_deviceDistance;
        public MyViewHolder(@NonNull View itemView) {

            super(itemView);
            deviceItem_TXT_deviceName = itemView.findViewById(R.id.deviceItem_TXT_deviceName);
            deviceItem_TXT_deviceDistance = itemView.findViewById(R.id.deviceItem_TXT_deviceDistance);
        }
        public void bind( Device device ){
            deviceItem_TXT_deviceName.setText(device.getName());
            deviceItem_TXT_deviceDistance.setText(""+device.getDistance());
        }
    }

}
