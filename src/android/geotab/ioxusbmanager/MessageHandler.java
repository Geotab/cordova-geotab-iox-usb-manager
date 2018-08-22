package android.geotab.ioxusbmanager;

import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import com.google.gson.Gson;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class MessageHandler {

    private static final String TAG = MessageHandler.class.getSimpleName();
    public final Lock mLock = new ReentrantLock();
    public final Condition mEvent = mLock.newCondition();

    public static byte [] mabMessage;
    private USBAccessoryControl accessoryCtrl;

    public MessageHandler(USBAccessoryControl accessorControl) {
        accessoryCtrl = accessorControl;
    }

    public void write(byte[] abData) {
        if (!USBAccessoryControl.mfConnectionOpen)
            return;

        try {
            // Lock the output stream for the write operation
            synchronized (USBAccessoryControl.mOutputStream) {
                USBAccessoryControl.mOutputStream.write(abData);
            }
        } catch (IOException e) {
            Log.w(TAG, "Exception writing to output stream", e);
            accessoryCtrl.close();
        }
    }

    public void CheckMessage(byte[] abData) {
        if (isDataValid(abData)) {
            byte bType = abData[1];

            switch (bType) {
                case IoxUSBStateManager.MESSAGE_HANDSHAKE:
                    mLock.lock();
                    try {
                        IoxUSBStateManager.mfHandshakeReceived = true;
                        mEvent.signal();
                    } finally {
                        mLock.unlock();
                    }
                    break;

                case IoxUSBStateManager.MESSAGE_ACK:
                    mLock.lock();
                    try {
                        IoxUSBStateManager.mfAckReceived = true;
                        mEvent.signal();
                    } finally {
                        mLock.unlock();
                    }
                    break;

                case IoxUSBStateManager.MESSAGE_GO_DEVICE_DATA:
                    ExtractHOSData(abData);
                    Gson gson = new Gson();
                    String json = gson.toJson(USBAccessoryControl.hosData);
                    IoxUSBManager.sendToJS(json);

                    Log.i(TAG, json);

                    byte[] abAck = new byte[] {};
                    mabMessage = BuildMessage(IoxUSBStateManager.TP_HOS_ACK, abAck);
                    USBAccessoryControl.messageHandler.write(mabMessage);
                    break;
            }
        }
    }

    public void ExtractHOSData(byte[] abData) {
        synchronized (USBAccessoryControl.hosData) {
            ByteBuffer abConvert;

            byte[] abDateTime = new byte[4];
            System.arraycopy(abData, 3, abDateTime, 0, abDateTime.length);
            abConvert = ByteBuffer.wrap(abDateTime).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int iDateTime = abConvert.getInt();
            Calendar gmtCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            gmtCalendar.clear();
            gmtCalendar.set(2002, Calendar.JANUARY, 1); // (Units given in seconds since Jan 1, 2002)
            gmtCalendar.add(Calendar.SECOND, iDateTime);
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
            USBAccessoryControl.hosData.sDateTime = dataFormat.format(gmtCalendar.getTime());

            byte[] abLatitude = new byte[4];
            System.arraycopy(abData, 7, abLatitude, 0, abLatitude.length);
            abConvert = ByteBuffer.wrap(abLatitude).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int iLatitude = abConvert.getInt();
            USBAccessoryControl.hosData.Latitude = (float) iLatitude / 10000000; // (Units given in 10^-7)

            byte[] abLogitude = new byte[4];
            System.arraycopy(abData, 11, abLogitude, 0, abLogitude.length);
            abConvert = ByteBuffer.wrap(abLogitude).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int iLogitude = abConvert.getInt();
            USBAccessoryControl.hosData.Logitude = (float) iLogitude / 10000000; // (Units given in 10^-7)

            USBAccessoryControl.hosData.iRoadSpeed = abData[15];

            byte[] abPRM = new byte[2];
            System.arraycopy(abData, 16, abPRM, 0, abPRM.length);
            abConvert = ByteBuffer.wrap(abPRM).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            USBAccessoryControl.hosData.iRPM = abConvert.getShort();
            USBAccessoryControl.hosData.iRPM /= 4; // Convert to RPM (Units given in 0.25)

            byte[] abOdometer = new byte[4];
            System.arraycopy(abData, 18, abOdometer, 0, abOdometer.length);
            abConvert = ByteBuffer.wrap(abOdometer).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            USBAccessoryControl.hosData.iOdometer = abConvert.getInt(); // (Units given in 0.1/km)

            byte bStatus = abData[22];
            USBAccessoryControl.hosData.sStatus = "";

            if ((bStatus & (1 << 0)) != 0)
                USBAccessoryControl.hosData.sStatus += "GPS Latched | ";
            else
                USBAccessoryControl.hosData.sStatus += "GPS Invalid | ";

            if ((bStatus & (1 << 1)) != 0)
                USBAccessoryControl.hosData.sStatus += "IGN on | ";
            else
                USBAccessoryControl.hosData.sStatus += "IGN off | ";

            if ((bStatus & (1 << 2)) != 0)
                USBAccessoryControl.hosData.sStatus += "Engine Data | ";
            else
                USBAccessoryControl.hosData.sStatus += "No Engine Data | ";

            if ((bStatus & (1 << 3)) != 0)
                USBAccessoryControl.hosData.sStatus += "Date/Time Valid | ";
            else
                USBAccessoryControl.hosData.sStatus += "Date/Time Invalid | ";

            if ((bStatus & (1 << 4)) != 0)
                USBAccessoryControl.hosData.sStatus += "Speed From Engine | ";
            else
                USBAccessoryControl.hosData.sStatus += "Speed From GPS | ";

            if ((bStatus & (1 << 5)) != 0)
                USBAccessoryControl.hosData.sStatus += "Distance From Engine | ";
            else
                USBAccessoryControl.hosData.sStatus += "Distance From GPS | ";

            byte[] abTripOdometer = new byte[4];
            System.arraycopy(abData, 23, abTripOdometer, 0, abTripOdometer.length);
            abConvert = ByteBuffer.wrap(abTripOdometer).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            USBAccessoryControl.hosData.iTripOdometer = abConvert.getInt(); // (Units given in 0.1/km)

            byte[] abEngineHours = new byte[4];
            System.arraycopy(abData, 27, abEngineHours, 0, abEngineHours.length);
            abConvert = ByteBuffer.wrap(abEngineHours).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            USBAccessoryControl.hosData.iEngineHours = abConvert.getInt(); // Already in units of 0.1h

            byte[] abTripDuration = new byte[4];
            System.arraycopy(abData, 31, abTripDuration, 0, abTripDuration.length);
            abConvert = ByteBuffer.wrap(abTripDuration).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            USBAccessoryControl.hosData.iTripDuration = abConvert.getInt(); // Units of seconds

            byte[] abVehicleId = new byte[4];
            System.arraycopy(abData, 35, abVehicleId, 0, abVehicleId.length);
            abConvert = ByteBuffer.wrap(abVehicleId).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            USBAccessoryControl.hosData.iVehicleId = abConvert.getInt();

            byte[] abDriverId = new byte[4];
            System.arraycopy(abData, 39, abDriverId, 0, abDriverId.length);
            abConvert = ByteBuffer.wrap(abDriverId).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            USBAccessoryControl.hosData.iDriverId = abConvert.getInt();
        }
    }

    public static byte[] BuildMessage(byte bType, byte[] abData) {
        byte[] abMessage = new byte[abData.length + 6];

        abMessage[0] = 0x02;
        abMessage[1] = bType;
        abMessage[2] = (byte) abData.length;

        System.arraycopy(abData, 0, abMessage, 3, abData.length);

        int iLengthUpToChecksum = abData.length + 3;
        byte abCalcChecksum[] = CalcChecksum(abMessage, iLengthUpToChecksum);
        System.arraycopy(abCalcChecksum, 0, abMessage, iLengthUpToChecksum, 2);

        abMessage[abMessage.length - 1] = 0x03;

        return abMessage;
    }

    private boolean isDataValid(byte[] abData) {
        if (abData == null || abData.length < 6) {
            return false;
        }

        // Check structure
        byte bSTX = abData[0];
        byte bLength = abData[2];
        byte bETX = abData[abData.length - 1];

        if (bSTX != 0x02 || bETX != 0x03) {
            return false ;
        }

        // Check checksum
        byte[] abChecksum = new byte[] { abData[abData.length - 3], abData[abData.length - 2] };
        byte[] abCalcChecksum = CalcChecksum(abData, bLength + 3);

        if (!Arrays.equals(abChecksum, abCalcChecksum)) {
            return false;
        }

        return true;
    }

    // Calculate the Fletcher's checksum over the given bytes
    private static byte[] CalcChecksum(byte[] abData, int iLength) {
        byte[] abChecksum = new byte[] { 0x00, 0x00 };

        for (int i = 0; i < iLength; i++) {
            abChecksum[0] += abData[i];
            abChecksum[1] += abChecksum[0];
        }

        return abChecksum;
    }
}