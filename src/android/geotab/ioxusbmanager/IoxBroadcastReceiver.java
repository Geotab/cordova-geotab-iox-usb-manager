package android.geotab.ioxusbmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;
import android.util.Log;

public class IoxBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = IoxBroadcastReceiver.class.getSimpleName();
    private USBAccessoryControl accessoryControl;

    public IoxBroadcastReceiver(USBAccessoryControl control) {
        accessoryControl = control;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (USBAccessoryControl.ACTION_USB_PERMISSION.equals(action)) {
            UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                Log.i(TAG, "Permission Granted");

                USBAccessoryControl.OpenStatus status = accessoryControl.open(accessory);

                if (status == USBAccessoryControl.OpenStatus.CONNECTED) {
                    Log.i(TAG, "Connected (onReceive)");
                } else {
                    Log.i(TAG, "Error: " + status);
                }
            } else {
                Log.i(TAG, "Permission NOT Granted");
            }
        } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
            Log.i(TAG, "Detached");
            IoxUSBManager.sendToJS("{state: \"detached\"}");
            accessoryControl.close();
        }
    }
}