package android.geotab.ioxusbmanager;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;
import android.content.Context;
import android.app.Activity;
import android.content.IntentFilter;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

/**
 * This class echoes a string called from JavaScript.
 */
public class IoxUSBManager extends CordovaPlugin {

    private Activity activity;
    private Context ctx;
    private static final String TAG = IoxUSBManager.class.getSimpleName();
    private USBAccessoryControl accessoryControl;
    private IoxBroadcastReceiver receiver;
    private static CallbackContext callback = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();
        ctx = activity.getApplicationContext();
        accessoryControl = new USBAccessoryControl(ctx);
        receiver = new IoxBroadcastReceiver(accessoryControl);

        IntentFilter filter = new IntentFilter(USBAccessoryControl.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        activity.registerReceiver(receiver, filter);

        new ShowToastUtil(ctx, activity);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("setCallback".equals(action)) {
            callback = callbackContext;
            return true;
        }

        return false;
    }

    public static void sendToJS(JSONObject data) {
        if (callback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, data);
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent != null && UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(intent.getAction())) {
            receiver.sendAttachedStatusToJS(true);
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        accessoryControl.open();
    }

    @Override
    public void onStop() {
        super.onStop();

        accessoryControl.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        accessoryControl.close();
    }
}
