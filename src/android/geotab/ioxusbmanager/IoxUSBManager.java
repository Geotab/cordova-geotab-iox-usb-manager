package android.geotab.ioxusbmanager;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import android.content.Context;
import android.app.Activity;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;

/**
 * This class echoes a string called from JavaScript.
 */
public class IoxUSBManager extends CordovaPlugin {

    private Activity activity;
    private Context ctx;
    private static final String TAG = IoxUSBManager.class.getSimpleName();
    private USBAccessoryControl accessoryControl;
    private IoxBroadcastReceiver receiver;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        activity = cordova.getActivity();
        ctx = activity.getApplicationContext();
        IntentFilter filter = new IntentFilter(USBAccessoryControl.ACTION_USB_PERMISSION);
        accessoryControl = new USBAccessoryControl(ctx, activity);
        receiver = new IoxBroadcastReceiver(ctx, activity, accessoryControl);

        activity.registerReceiver(receiver, filter);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        accessoryControl.open();
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
