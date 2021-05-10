package com.kegel.booker.watch;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQDevice;

import java.util.ArrayList;
import java.util.List;

public class WatchHelpers {
    public static List<IQDevice> getConnectedDevices(ConnectIQ transport) {
        try {
            List<IQDevice> paired = transport.getKnownDevices();
            List<IQDevice> connect = new ArrayList<>();
            if (paired != null && paired.size() > 0) {
                // get the status of the devices
                for (IQDevice device : paired) {
                    IQDevice.IQDeviceStatus status = transport.getDeviceStatus(device);
                    if (status == IQDevice.IQDeviceStatus.CONNECTED) {
                        connect.add(device);
                    }
                }
            }
            return connect;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
