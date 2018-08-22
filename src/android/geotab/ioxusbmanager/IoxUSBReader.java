package android.geotab.ioxusbmanager;

import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class IoxUSBReader implements Runnable {

    private AtomicBoolean fRunning = new AtomicBoolean(true);
    private Activity mActivity;
    private Context mContext;
    private IoxUSBStateManager stateManager;
    private static final String TAG = IoxUSBReader.class.getSimpleName();
    private final Lock mLock = new ReentrantLock();
    private final Condition mReceiverEnded = mLock.newCondition();

    public IoxUSBReader(IoxUSBStateManager stateManager) {
        this.stateManager = stateManager;
    }

    public void run()
    {
        int iNumberOfBytesRead = 0;
        byte[] abBuffer = new byte[512];	// max is [16384]

        try
        {
            while (fRunning.get())
            {
                // Note: Read blocks until one byte has been read, the end of the source stream is detected or an exception is thrown
                iNumberOfBytesRead = USBAccessoryControl.mInputStream.read(abBuffer);

                if (fRunning.get() && (iNumberOfBytesRead > 0))
                {
                    byte[] abMessage = new byte[iNumberOfBytesRead];
                    System.arraycopy(abBuffer, 0, abMessage, 0, abMessage.length);

                    USBAccessoryControl.messageHandler.CheckMessage(abMessage);
                }
            }
        }
        catch (Exception e)
        {
            Log.w(TAG, "Exception reading input stream", e);
            close();
        }

        mLock.lock();
        try
        {
            mReceiverEnded.signal();
        }
        finally
        {
            mLock.unlock();
        }

        Log.i(TAG, "Receiver thread ended");
    }

    // Shutdown the receiver and third party threads
    public void close()
    {
        fRunning.set(false);
        this.stateManager.close();
    }
}