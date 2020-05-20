package android.geotab.ioxusbmanager;

import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import org.json.JSONObject;
import org.json.JSONException;
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
                    IoxUSBManager.sendToJS(USBAccessoryControl.hosData);

                    Log.i(TAG, USBAccessoryControl.hosData.toString());

                    byte[] abAck = new byte[] {};
                    mabMessage = BuildMessage(IoxUSBStateManager.TP_HOS_ACK, abAck);
                    USBAccessoryControl.messageHandler.write(mabMessage);
                    break;
            }
        }
    }

    public void ExtractHOSData(byte[] abData) {
        synchronized (USBAccessoryControl.hosData) {
            byte[] dateTimeBytes = new byte[4];
            System.arraycopy(abData, 3, dateTimeBytes, 0, dateTimeBytes.length);
            ByteBuffer abConvertDateTime = ByteBuffer.wrap(dateTimeBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int iDateTime = abConvertDateTime.getInt();
            Calendar gmtCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            gmtCalendar.clear();
            gmtCalendar.set(2002, Calendar.JANUARY, 1); // (Units given in seconds since Jan 1, 2002)
            gmtCalendar.add(Calendar.SECOND, iDateTime);
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);

            byte[] latitudeBytes = new byte[4];
            System.arraycopy(abData, 7, latitudeBytes, 0, latitudeBytes.length);
            ByteBuffer convertLat = ByteBuffer.wrap(latitudeBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int latitude = convertLat.getInt();

            byte[] longitudeBytes = new byte[4];
            System.arraycopy(abData, 11, longitudeBytes, 0, longitudeBytes.length);
            ByteBuffer convertLon = ByteBuffer.wrap(longitudeBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            int longitude = convertLon.getInt();

            byte[] RPMBytes = new byte[2];
            System.arraycopy(abData, 16, RPMBytes, 0, RPMBytes.length);
            ByteBuffer convertRPM = ByteBuffer.wrap(RPMBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            short RPM = convertRPM.getShort();

            byte[] odometerBytes = new byte[4];
            System.arraycopy(abData, 18, odometerBytes, 0, odometerBytes.length);
            ByteBuffer convertOdometer = ByteBuffer.wrap(odometerBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);

            byte bStatus = abData[22];
            String sStatus = "";

            if ((bStatus & (1 << 0)) != 0) {
                sStatus += "GPS Latched | ";
            } else {
                sStatus += "GPS Invalid | ";
            }

            if ((bStatus & (1 << 1)) != 0) {
                sStatus += "IGN on | ";
            } else {
                sStatus += "IGN off | ";
            }

            if ((bStatus & (1 << 2)) != 0) {
                sStatus += "Engine Data | ";
            } else {
                sStatus += "No Engine Data | ";
            }

            if ((bStatus & (1 << 3)) != 0) {
                sStatus += "Date/Time Valid | ";
            } else {
                sStatus += "Date/Time Invalid | ";
            }

            if ((bStatus & (1 << 4)) != 0) {
                sStatus += "Speed From Engine | ";
            } else {
                sStatus += "Speed From GPS | ";
            }

            if ((bStatus & (1 << 5)) != 0) {
                sStatus += "Distance From Engine | ";
            } else {
                sStatus += "Distance From GPS | ";
            }

            byte[] tripOdometerBytes = new byte[4];
            System.arraycopy(abData, 23, tripOdometerBytes, 0, tripOdometerBytes.length);
            ByteBuffer convertTripOdometer = ByteBuffer.wrap(tripOdometerBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);

            byte[] engineHoursBytes = new byte[4];
            System.arraycopy(abData, 27, engineHoursBytes, 0, engineHoursBytes.length);
            ByteBuffer convertEngineHours = ByteBuffer.wrap(engineHoursBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);

            byte[] tripDurationBytes = new byte[4];
            System.arraycopy(abData, 31, tripDurationBytes, 0, tripDurationBytes.length);
            ByteBuffer convertTripDuration = ByteBuffer.wrap(tripDurationBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);

            byte[] vehicleIdBytes = new byte[4];
            System.arraycopy(abData, 35, vehicleIdBytes, 0, vehicleIdBytes.length);
            ByteBuffer convertVehicleId = ByteBuffer.wrap(vehicleIdBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);

            byte[] driverIdBytes = new byte[4];
            System.arraycopy(abData, 39, driverIdBytes, 0, driverIdBytes.length);
            ByteBuffer convertDriverId = ByteBuffer.wrap(driverIdBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            try {
                USBAccessoryControl.hosData.put("dateTime", dataFormat.format(gmtCalendar.getTime()));
                USBAccessoryControl.hosData.put("latitude", (float) latitude / 10000000); // (Units given in 10^-7)
                USBAccessoryControl.hosData.put("longitude", (float) longitude / 10000000); // (Units given in 10^-7)
                USBAccessoryControl.hosData.put("roadSpeed", abData[15]);
                USBAccessoryControl.hosData.put("rpm", RPM / 4); // Convert to RPM (Units given in 0.25)
                USBAccessoryControl.hosData.put("odometer", convertOdometer.getInt()); // (Units given in 0.1/km)
                USBAccessoryControl.hosData.put("status", sStatus);
                USBAccessoryControl.hosData.put("tripOdometer", convertTripOdometer.getInt()); // (Units given in 0.1/km)
                USBAccessoryControl.hosData.put("engineHours", convertEngineHours.getInt()); // Already in units of 0.1h
                USBAccessoryControl.hosData.put("tripDuration", convertTripDuration.getInt()); // Units of seconds
                USBAccessoryControl.hosData.put("vehicleId", convertVehicleId.getInt());
                USBAccessoryControl.hosData.put("driverId", convertDriverId.getInt());
                USBAccessoryControl.hosData.put("rawData", abData);
            } catch(JSONException ex) {
                Log.e(TAG, "Exception when creating hosData message JSON", ex);
            }
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
