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
    private static CallbackContext callback = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();
        ctx = activity.getApplicationContext();
        accessoryControl = new USBAccessoryControl(ctx);
        receiver = new IoxBroadcastReceiver(accessoryControl);

        IntentFilter filter = new IntentFilter(USBAccessoryControl.ACTION_USB_PERMISSION);
        new ShowToastUtil(ctx, activity);

        activity.registerReceiver(receiver, filter);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("setCallback".equals(action)) {
            callback = callbackContext;
            return true;
        }

        return false;
    }

    public static void sendToJS(String data) {
        if (callback != null) {
            try {
                JSONObject parameter = new JSONObject();
                parameter.put("myData", data);
                PluginResult result = new PluginResult(PluginResult.Status.OK, parameter);
                result.setKeepCallback(true);
                callback.sendPluginResult(result);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        accessoryControl.open();
    }
}
