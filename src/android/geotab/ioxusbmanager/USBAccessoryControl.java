package android.geotab.ioxusbmanager;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import android.app.Activity;

public class USBAccessoryControl {
    public static final String ACTION_USB_PERMISSION = "android.geotab.ioxusbmanager.MainActivity.action.USB_PERMISSION";
    private static final String TAG = USBAccessoryControl.class.getSimpleName();
    private static final String ACC_MANUF = "Geotab";
    private static final String ACC_MODEL = "IOX USB";
    private FileOutputStream mOutputStream;
    private FileInputStream mInputStream;
    private boolean mfPermissionRequested, mfConnectionOpen;
    private Context mContext;
    private Activity mActivity;
    private UsbManager mUSBManager;
    private ParcelFileDescriptor mParcelFileDescriptor;

    public static enum OpenStatus {
        CONNECTED, REQUESTING_PERMISSION, UNKNOWN_ACCESSORY, NO_ACCESSORY, NO_PARCEL
    }

    public USBAccessoryControl(Context context, Activity activity)
	{
		mfPermissionRequested = false;
		mfConnectionOpen = false;
		// mThirdParty = null;

        mContext = context;
        mActivity = activity;
		mUSBManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public OpenStatus open()
	{
		if (mfConnectionOpen)
			return USBAccessoryControl.OpenStatus.CONNECTED;

		UsbAccessory[] accList = mUSBManager.getAccessoryList();
		if (accList != null && accList.length > 0) {
			if (mUSBManager.hasPermission(accList[0])) {
                return open(accList[0]);
            }

			if (!mfPermissionRequested) {
				Log.i(TAG, "Requesting USB permission");

				PendingIntent permissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
				mUSBManager.requestPermission(accList[0], permissionIntent);
				mfPermissionRequested = true;

				return USBAccessoryControl.OpenStatus.REQUESTING_PERMISSION;
			}
        }
        return USBAccessoryControl.OpenStatus.NO_ACCESSORY;
    }

    public OpenStatus open(UsbAccessory accessory) {
        if (mfConnectionOpen)
            return USBAccessoryControl.OpenStatus.CONNECTED;

        // Check if the accessory is supported by this app
        if (!ACC_MANUF.equals(accessory.getManufacturer()) || !ACC_MODEL.equals(accessory.getModel())) {
            Log.i(TAG, "Unknown accessory: " + accessory.getManufacturer() + ", " + accessory.getModel());
            return USBAccessoryControl.OpenStatus.UNKNOWN_ACCESSORY;
        }

        // Open read/write streams for the accessory
        mParcelFileDescriptor = mUSBManager.openAccessory(accessory);

        if (mParcelFileDescriptor != null) {
            FileDescriptor fd = mParcelFileDescriptor.getFileDescriptor();
            mOutputStream = new FileOutputStream(fd);
            mInputStream = new FileInputStream(fd);

            mfConnectionOpen = true;

            // mReceiver = new Receiver();
            // new Thread(mReceiver).start(); // Run the receiver as a separate thread

            return USBAccessoryControl.OpenStatus.CONNECTED;
        }

        Log.i(TAG, "Couldn't get any ParcelDescriptor");
        return USBAccessoryControl.OpenStatus.NO_PARCEL;
    }

    public void close() {
        if (!mfConnectionOpen) {
            return;
        }

        mfPermissionRequested = false;
        mfConnectionOpen = false;

        // End the receiver thread
        // mReceiver.close();
        Log.i(TAG, "Receiver Thread closed");

        // Close the data streams
        try {
            mInputStream.close();
            Log.i(TAG, "Input Stream closed");
        } catch (IOException e) {
            Log.w(TAG, "Exception when closing Input Stream", e);
        }

        try {
            mOutputStream.close();
            Log.i(TAG, "Output Stream closed");
        } catch (IOException e) {
            Log.w(TAG, "Exception when closing Output Stream", e);
        }

        try {
            mParcelFileDescriptor.close();
            Log.i(TAG, "File Descriptor closed");
        } catch (IOException e) {
            Log.w(TAG, "Exception when closing File Descriptor", e);
        }
    }
}