package jpcsp.memory;

import jpcsp.Memory;

public class MemoryWriter {
    public static IMemoryWriter getMemoryShortWriter(final Memory mem, final int addr, final int length) {
        return new IMemoryWriter() {
            int caddr = addr;

            @Override public void writeNext(int value) {
                mem.write16(caddr, value);
                caddr += 2;
            }

            @Override public void skip(int n) {
                caddr += n * 2;
            }

            @Override public void flush() {
            }

            @Override public int getCurrentAddress() {
                return caddr;
            }
        };
    }
}
