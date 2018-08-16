package android.geotab.ioxusbmanager;

import android.content.BroadcastReceiver;
import android.view.Gravity;
import android.widget.Toast;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;
import android.util.Log;

public class IoxBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = IoxBroadcastReceiver.class.getSimpleName();
    private Activity activity;
    private Context ctx;
    private USBAccessoryControl accessoryControl;

    public IoxBroadcastReceiver(Context context, Activity activ, USBAccessoryControl control) {
        ctx = context;
        activity = activ;
        accessoryControl = control;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Check the reason the receiver was called
        if (USBAccessoryControl.ACTION_USB_PERMISSION.equals(action)) {
            UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                Log.i(TAG, "Permission Granted");

                USBAccessoryControl.OpenStatus status = accessoryControl.open(accessory);
                if (status == USBAccessoryControl.OpenStatus.CONNECTED)
                    showToastFromThread("Connected (onReceive)");
                else
                    showToastFromThread("Error: " + status);
            } else {
                Log.i(TAG, "Permission NOT Granted");
            }
        } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
            showToastFromThread("Detached");
            accessoryControl.close();
        }
    }

    public void showToastFromThread(final String sToast) {
        Log.i(TAG, sToast);

        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast DisplayMessage = Toast.makeText(ctx, sToast, Toast.LENGTH_SHORT);
                DisplayMessage.setGravity(Gravity.CENTER_VERTICAL | Gravity.BOTTOM, 0, 0);
                DisplayMessage.show();
            }
        });
    }
}