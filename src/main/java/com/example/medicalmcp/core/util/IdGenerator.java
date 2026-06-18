package com.example.medicalmcp.core.util;

import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Enumeration;

public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ID_LENGTH = 24;

    private static final byte[] MACHINE_ID = generateMachineId();
    private static final byte[] PROCESS_ID = generateProcessId();
    private static final Object COUNTER_LOCK = new Object();
    private static int counter = RANDOM.nextInt(0xFFFFFF);

    private IdGenerator() {}

    public static String generateId() {
        byte[] idBytes = new byte[12];
        ByteBuffer buffer = ByteBuffer.wrap(idBytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        int timestamp = (int) (System.currentTimeMillis() / 1000);
        buffer.putInt(timestamp);

        buffer.put(MACHINE_ID, 0, 3);
        buffer.put(PROCESS_ID, 0, 2);

        synchronized (COUNTER_LOCK) {
            counter = (counter + 1) & 0xFFFFFF;
            buffer.put((byte) ((counter >>> 16) & 0xFF));
            buffer.put((byte) ((counter >>> 8) & 0xFF));
            buffer.put((byte) (counter & 0xFF));
        }

        return bytesToHex(idBytes);
    }

    public static boolean isValidId(String id) {
        if (id == null || id.length() != ID_LENGTH) {
            return false;
        }
        return id.matches("^[0-9a-fA-F]{24}$");
    }

    private static byte[] generateMachineId() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length >= 6) {
                    byte[] machineId = new byte[3];
                    System.arraycopy(mac, 0, machineId, 0, 3);
                    return machineId;
                }
            }
        } catch (Exception e) {
            // fall through to random
        }
        byte[] machineId = new byte[3];
        RANDOM.nextBytes(machineId);
        return machineId;
    }

    private static byte[] generateProcessId() {
        byte[] processId = new byte[2];
        try {
            long pid = ProcessHandle.current().pid();
            if (pid > 0 && pid <= 0xFFFF) {
                processId[0] = (byte) ((pid >>> 8) & 0xFF);
                processId[1] = (byte) (pid & 0xFF);
                return processId;
            }
        } catch (Exception e) {
            // fall through to random
        }
        RANDOM.nextBytes(processId);
        return processId;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
