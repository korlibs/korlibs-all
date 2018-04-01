package jpcsp;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Memory {
    public ByteBuffer buffer = ByteBuffer.allocate(0x800000).order(ByteOrder.LITTLE_ENDIAN);

    static private Memory _instance;
    static public Memory getInstance() {
        if (_instance == null) {
            _instance = new Memory();
        }
        return _instance;
    }

    public int read8(int addr) {
        return buffer.get(addr) & 0xFF;
    }

    public int read16(int addr) {
        return buffer.getChar(addr) & 0xFFFF;
    }

    public void write8(int addr, int v) {
        buffer.put(addr, (byte)v);
    }

    public void write16(int addr, int v) {
        buffer.putShort(addr, (short) v);
    }

    public void writeBytes(int addr, @NotNull byte[] bytes) {
        for (int n = 0; n < bytes.length; n++) write8(n, bytes[n]);
    }

    @NotNull public byte[] readBytes(int addr, int count) {
        byte[] out = new byte[count];
        for (int n = 0; n < count; n++) out[n] = (byte) read8(addr + n);
        return out;
    }
}
