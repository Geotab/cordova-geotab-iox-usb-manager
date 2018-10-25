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
import android.app.Activity;

public class USBAccessoryControl {
    public static final String ACTION_USB_PERMISSION = "android.geotab.ioxusbmanager.MainActivity.action.USB_PERMISSION";
    public static FileOutputStream mOutputStream;
    public static FileInputStream mInputStream;
    public static boolean mfConnectionOpen;
    public static MessageHandler messageHandler;
    public static HOSData hosData = new HOSData();

    private static final String TAG = USBAccessoryControl.class.getSimpleName();
    private static final String ACC_MANUF = "Geotab";
    private static final String ACC_MODEL = "IOX USB";

    private boolean mfPermissionRequested;
    private Context mContext;
    private Activity mActivity;
    private UsbManager mUSBManager;
    private ParcelFileDescriptor mParcelFileDescriptor;
    private IoxUSBReader ioxUsbDataReader;
    private IoxUSBStateManager stateManager;

    public static enum OpenStatus {
        CONNECTED, REQUESTING_PERMISSION, UNKNOWN_ACCESSORY, NO_ACCESSORY, NO_PARCEL
    }

    public USBAccessoryControl(Context context)
	{
        USBAccessoryControl.mfConnectionOpen = false;

		mfPermissionRequested = false;
        messageHandler = new MessageHandler(this);
        mContext = context;

        mUSBManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    public OpenStatus open()
	{
		if (USBAccessoryControl.mfConnectionOpen) {
            return USBAccessoryControl.OpenStatus.CONNECTED;
        }

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
        if (USBAccessoryControl.mfConnectionOpen) {
            return USBAccessoryControl.OpenStatus.CONNECTED;
        }

        if (!ACC_MANUF.equals(accessory.getManufacturer()) || !ACC_MODEL.equals(accessory.getModel())) {
            Log.i(TAG, "Unknown accessory: " + accessory.getManufacturer() + ", " + accessory.getModel());
            return USBAccessoryControl.OpenStatus.UNKNOWN_ACCESSORY;
        }

        mParcelFileDescriptor = mUSBManager.openAccessory(accessory);

        if (mParcelFileDescriptor != null) {
            FileDescriptor fd = mParcelFileDescriptor.getFileDescriptor();
            USBAccessoryControl.mOutputStream = new FileOutputStream(fd);
            USBAccessoryControl.mInputStream = new FileInputStream(fd);

            USBAccessoryControl.mfConnectionOpen = true;

            stateManager = new IoxUSBStateManager();
            ioxUsbDataReader = new IoxUSBReader(stateManager);
            new Thread(ioxUsbDataReader, "Data reader thread").start();
            new Thread(stateManager, "State manager of GO device thread").start();

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
        ioxUsbDataReader.close();
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