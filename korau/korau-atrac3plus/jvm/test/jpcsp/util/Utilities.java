package jpcsp.util;

import jpcsp.Memory;

import static java.lang.System.arraycopy;

public class Utilities {
    public static int signExtend(int value, int bits) {
        int shift = Integer.SIZE - bits;
        return (value << shift) >> shift;
    }

    public static void copy(float[] to, float[] from) {
        arraycopy(from, 0, to, 0, to.length);
    }

    public static void copy(int[] to, int[] from) {
        arraycopy(from, 0, to, 0, to.length);
    }

    public static int readUnaligned32(Memory mem, int address) {
        //switch (address & 3) {
        //    case 0:
        //        return mem.read32(address);
        //    case 2:
        //        return mem.read16(address) | (mem.read16(address + 2) << 16);
        //    default:
                return (mem.read8(address + 3) << 24)
                        | (mem.read8(address + 2) << 16)
                        | (mem.read8(address + 1) << 8)
                        | (mem.read8(address));
        //}
    }
}
