package com.kegel.booker.watch;

import android.content.Context;
import android.util.Log;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;
import com.kegel.booker.book.Helpers;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigTransmitter {
    private ConnectIQ connectIQ;
    private Context context;
    private IQDevice device;

    public ConfigTransmitter(Context context) {
        this.context = context;
        connectIQ = ConnectIQ.getInstance(context, ConnectIQ.IQConnectType.WIRELESS);
        connectIQ.initialize(context, true, new ConnectIQ.ConnectIQListener() {
            @Override
            public void onSdkReady() {
                Log.d("watch", "SDK Ready");
                IQDevice dev = WatchHelpers.getConnectedDevices(connectIQ).stream().findFirst().orElse(null);
                if (dev != null) {
                    Log.d("watch", "Found a watch!");
                    try {
                        getApplication(dev);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("watch", "Could not get teh application");
                    }
                }
            }

            @Override
            public void onInitializeError(ConnectIQ.IQSdkErrorStatus iqSdkErrorStatus) {
                Log.e("watch", "Failed it initialize Garmin SDK");
            }

            @Override
            public void onSdkShutDown() {

            }
        });
    }

    private void getApplication(IQDevice dev) throws InvalidStateException, ServiceUnavailableException {
        this.device = dev;
        connectIQ.getApplicationInfo("17b573921b504c09aae18443b5f9774d", dev, new ConnectIQ.IQApplicationInfoListener() {
            @Override
            public void onApplicationInfoReceived(IQApp iqApp) {
                try {
                    register(iqApp);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("watch", "Failed to register application listener");
                }
            }

            @Override
            public void onApplicationNotInstalled(String s) {
                Log.e("watch", "Application is not installed on watch");
            }
        });
    }

    private void register(IQApp app) throws InvalidStateException {
        connectIQ.registerForAppEvents(device, app, new ConnectIQ.IQApplicationEventListener() {
            @Override
            public void onMessageReceived(IQDevice iqDevice, IQApp iqApp, List<Object> msgList, ConnectIQ.IQMessageStatus iqMessageStatus) {
                Log.d("watch", "Got a message from the watch");
                if (msgList != null && msgList.size() > 0) {
                    parseMessage(app, (Map<String, String>) msgList.get(0));
                }
            }
        });
    }

    private void parseMessage(IQApp app, Map<String, String> commandMap) {
        if (commandMap.containsKey("operation")) {
            String op = commandMap.get("operation");
            if (op.equals("config")) { //requested a config, lets send it
                try {
                    connectIQ.sendMessage(device, app, buildConfigMesssge(), new ConnectIQ.IQSendMessageListener() {
                        @Override
                        public void onMessageStatus(IQDevice iqDevice, IQApp iqApp, ConnectIQ.IQMessageStatus iqMessageStatus) {
                            if (iqMessageStatus != ConnectIQ.IQMessageStatus.SUCCESS) {
                                Log.e("watch", "Failed to send configuration");
                            }
                            else {
                                Log.d("watch", "Configuration sent!");
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("watch", "Failed to send configuration");
                }
            }
        }

    }

    private Map<String, String> buildConfigMesssge() throws MalformedURLException, URISyntaxException {
        Map<String, String> config = new HashMap<>();
        config.put("url", Helpers.getURL(context));
        return config;
    }


}
