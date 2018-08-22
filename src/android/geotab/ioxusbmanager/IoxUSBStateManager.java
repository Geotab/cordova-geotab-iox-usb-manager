package android.geotab.ioxusbmanager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import android.util.Log;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class IoxUSBStateManager implements Runnable {

    private static final String TAG = IoxUSBStateManager.class.getSimpleName();

    public static boolean mfAckReceived = false;
    public static boolean mfHandshakeReceived = false;
    public static boolean mfMessageToSend = false;
    public final Lock mLock = new ReentrantLock();
    public final Condition mEvent = mLock.newCondition();

    public static final byte MESSAGE_HANDSHAKE = 1;
    public static final byte MESSAGE_ACK = 2;
    public static final byte MESSAGE_GO_DEVICE_DATA = 0x21;
    public static final byte TP_HOS_ACK = (byte) 0x84;
    private static final byte MESSAGE_CONFIRMATION = (byte) 0x81;
    private static final byte[] HOS_ENHANCED_ID_WITH_ACK = new byte[] { 0x2D, 0x10, 0x00, 0x00 };
    private static final byte MESSAGE_SYNC = 0x55;

    private enum State {
        SEND_SYNC, WAIT_FOR_HANDSHAKE, SEND_CONFIRMATION, PRE_IDLE, IDLE, WAIT_FOR_ACK
    }

    private State eState = State.SEND_SYNC;
    private AtomicBoolean isStateRunning = new AtomicBoolean(true);

    public void run() {
       ShowToastUtil.showToastFromThread(TAG, "IoxUSBStateManager started");

        while (isStateRunning.get()) {
            mLock.lock(); // The lock is needed for await and atomic access to flags/buffers

            try {
                switch (eState) {
                    case SEND_SYNC: {
                        byte[] abMessage = new byte[] { MESSAGE_SYNC };
                        USBAccessoryControl.messageHandler.write(abMessage);
                        eState = State.WAIT_FOR_HANDSHAKE;
                        break;
                    }
                    case WAIT_FOR_HANDSHAKE: {
                        // Waits for the handshake message or resends sync every 1s
                        mEvent.await(1000, TimeUnit.MILLISECONDS);

                        if (IoxUSBStateManager.mfHandshakeReceived) {
                            eState = State.SEND_CONFIRMATION;
                        } else {
                            eState = State.SEND_SYNC;
                        }
                        break;
                    }
                    case SEND_CONFIRMATION: {
                        byte[] abMessage = MessageHandler.BuildMessage(MESSAGE_CONFIRMATION, HOS_ENHANCED_ID_WITH_ACK);
                        USBAccessoryControl.messageHandler.write(abMessage);
                        ShowToastUtil.showToastFromThread(TAG, "HOS Connected");
                        eState = State.PRE_IDLE;
                        break;
                    }
                    case PRE_IDLE: {
                        IoxUSBStateManager.mfHandshakeReceived = false;
                        IoxUSBStateManager.mfAckReceived = false;
                        IoxUSBStateManager.mfMessageToSend = false;
                        eState = State.IDLE;
                        break;
                    }
                    case IDLE: {
                        // Sleep and wait for a handshake or a message to send
                        mEvent.await();

                        if (IoxUSBStateManager.mfHandshakeReceived) {
                            eState = State.SEND_CONFIRMATION;
                        } else if (IoxUSBStateManager.mfMessageToSend) {
                            USBAccessoryControl.messageHandler.write(MessageHandler.mabMessage);
                            eState = State.WAIT_FOR_ACK;
                        }
                        break;
                    }
                    case WAIT_FOR_ACK: {
                        // Wait for the ack or reset after 5s
                        mEvent.await(5000, TimeUnit.MILLISECONDS);

                        if (IoxUSBStateManager.mfAckReceived) {
                            eState = State.PRE_IDLE;
                        } else {
                            eState = State.SEND_SYNC;
                        }
                        break;
                    }
                    default: {
                        eState = State.SEND_SYNC;
                        break;
                    }
                }

            } catch (InterruptedException e) {
                Log.w(TAG, "Exception during await", e);
            } finally {
                mLock.unlock();
            }
        }
    }

    // Stop the thread
    public void close() {
        Log.i(TAG, "Shutting down third party SM");

        mLock.lock();
        try {
            isStateRunning.set(false);
            IoxUSBStateManager.mfHandshakeReceived = false;
            IoxUSBStateManager.mfAckReceived = false;
            IoxUSBStateManager.mfMessageToSend = false;
            mEvent.signal();
        } finally {
            mLock.unlock();
        }
    }
}