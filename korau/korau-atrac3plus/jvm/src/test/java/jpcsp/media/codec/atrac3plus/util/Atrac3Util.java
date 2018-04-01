package jpcsp.media.codec.atrac3plus.util;

import jpcsp.Memory;
import jpcsp.util.Utilities;
import org.apache.log4j.Logger;

import static jpcsp.util.Utilities.readUnaligned32;

public class Atrac3Util {
    public static final int PSP_CODEC_AT3PLUS = 0x00001000;
    public static final int PSP_CODEC_AT3     = 0x00001001;
    public static final int PSP_CODEC_MP3     = 0x00001002;
    public static final int PSP_CODEC_AAC     = 0x00001003;


    public    static final int AT3_MAGIC      = 0x0270; // "AT3"
    public    static final int AT3_PLUS_MAGIC = 0xFFFE; // "AT3PLUS"
    public    static final int RIFF_MAGIC = 0x46464952; // "RIFF"
    public    static final int WAVE_MAGIC = 0x45564157; // "WAVE"
    public    static final int FMT_CHUNK_MAGIC = 0x20746D66; // "FMT "
    protected static final int FACT_CHUNK_MAGIC = 0x74636166; // "FACT"
    protected static final int SMPL_CHUNK_MAGIC = 0x6C706D73; // "SMPL"
    public    static final int DATA_CHUNK_MAGIC = 0x61746164; // "DATA"

    private static final int ATRAC3_CONTEXT_READ_SIZE_OFFSET = 160;
    private static final int ATRAC3_CONTEXT_REQUIRED_SIZE_OFFSET = 164;
    private static final int ATRAC3_CONTEXT_DECODE_RESULT_OFFSET = 188;

    public static final int PSP_ATRAC_ALLDATA_IS_ON_MEMORY = -1;
    public static final int PSP_ATRAC_NONLOOP_STREAM_DATA_IS_ON_MEMORY = -2;
    public static final int PSP_ATRAC_LOOP_STREAM_DATA_IS_ON_MEMORY = -3;

    protected static final int PSP_ATRAC_STATUS_NONLOOP_STREAM_DATA = 0;
    protected static final int PSP_ATRAC_STATUS_LOOP_STREAM_DATA = 1;

    public static final int ATRAC_HEADER_HASH_LENGTH = 512;

    public static final int atracDecodeDelay = 2300; // Microseconds, based on PSP tests
    static Logger log = Logger.getLogger("Atrac3Util");

    public final static int ERROR_ATRAC_UNKNOWN_FORMAT                          = 0x80630006;
    public final static int ERROR_ATRAC_INVALID_SIZE                            = 0x80630011;

    protected static String getStringFromInt32(int n) {
        char c1 = (char) ((n      ) & 0xFF);
        char c2 = (char) ((n >>  8) & 0xFF);
        char c3 = (char) ((n >> 16) & 0xFF);
        char c4 = (char) ((n >> 24) & 0xFF);

        return String.format("%c%c%c%c", c1, c2, c3, c4);
    }

    public static int analyzeRiffFile(Memory mem, int addr, int length, AtracFileInfo info) {
        int result = ERROR_ATRAC_UNKNOWN_FORMAT;

        int currentAddr = addr;
        int bufferSize = length;
        info.atracEndSample = -1;
        info.numLoops = 0;
        info.inputFileDataOffset = 0;

        if (bufferSize < 12) {
            log.error(String.format("Atrac buffer too small %d", bufferSize));
            return ERROR_ATRAC_INVALID_SIZE;
        }

        // RIFF file format:
        // Offset 0: 'RIFF'
        // Offset 4: file length - 8
        // Offset 8: 'WAVE'
        int magic = readUnaligned32(mem, currentAddr);
        int WAVEMagic = readUnaligned32(mem, currentAddr + 8);
        if (magic != RIFF_MAGIC || WAVEMagic != WAVE_MAGIC) {
            //log.error(String.format("Not a RIFF/WAVE format! %s", Utilities.getMemoryDump(currentAddr, 16)));
            log.error(String.format("Not a RIFF/WAVE format"));
            return ERROR_ATRAC_UNKNOWN_FORMAT;
        }

        info.inputFileSize = readUnaligned32(mem, currentAddr + 4) + 8;
        info.inputDataSize = info.inputFileSize;
        if (log.isDebugEnabled()) {
            log.debug(String.format("FileSize 0x%X", info.inputFileSize));
        }
        currentAddr += 12;
        bufferSize -= 12;

        boolean foundData = false;
        while (bufferSize >= 8 && !foundData) {
            int chunkMagic = readUnaligned32(mem, currentAddr);
            int chunkSize = readUnaligned32(mem, currentAddr + 4);
            currentAddr += 8;
            bufferSize -= 8;

            switch (chunkMagic) {
                case DATA_CHUNK_MAGIC:
                    foundData = true;
                    // Offset of the data chunk in the input file
                    info.inputFileDataOffset = currentAddr - addr;
                    info.inputDataSize = chunkSize;
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("DATA Chunk: data offset=0x%X, data size=0x%X", info.inputFileDataOffset, info.inputDataSize));
                    }
                    break;
                case FMT_CHUNK_MAGIC: {
                    if (chunkSize >= 16) {
                        int compressionCode = mem.read16(currentAddr);
                        info.atracChannels = mem.read16(currentAddr + 2);
                        info.atracSampleRate = readUnaligned32(mem, currentAddr + 4);
                        info.atracBitrate = readUnaligned32(mem, currentAddr + 8);
                        info.atracBytesPerFrame = mem.read16(currentAddr + 12);
                        int hiBytesPerSample = mem.read16(currentAddr + 14);
                        int extraDataSize = mem.read16(currentAddr + 16);
                        if (extraDataSize == 14) {
                            info.atracCodingMode = mem.read16(currentAddr + 18 + 6);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("WAVE format: magic=0x%08X('%s'), chunkSize=%d, compressionCode=0x%04X, channels=%d, sampleRate=%d, bitrate=%d, bytesPerFrame=0x%X, hiBytesPerSample=%d, codingMode=%d", chunkMagic, getStringFromInt32(chunkMagic), chunkSize, compressionCode, info.atracChannels, info.atracSampleRate, info.atracBitrate, info.atracBytesPerFrame, hiBytesPerSample, info.atracCodingMode));
                            // Display rest of chunk as debug information
                            StringBuilder restChunk = new StringBuilder();
                            for (int i = 16; i < chunkSize; i++) {
                                int b = mem.read8(currentAddr + i);
                                restChunk.append(String.format(" %02X", b));
                            }
                            if (restChunk.length() > 0) {
                                log.debug(String.format("Additional chunk data:%s", restChunk));
                            }
                        }

                        if (compressionCode == AT3_MAGIC) {
                            result = PSP_CODEC_AT3;
                        } else if (compressionCode == AT3_PLUS_MAGIC) {
                            result = PSP_CODEC_AT3PLUS;
                        } else {
                            return ERROR_ATRAC_UNKNOWN_FORMAT;
                        }
                    }
                    break;
                }
                case FACT_CHUNK_MAGIC: {
                    if (chunkSize >= 8) {
                        info.atracEndSample = readUnaligned32(mem, currentAddr);
                        if (info.atracEndSample > 0) {
                            info.atracEndSample -= 1;
                        }
                        if (chunkSize >= 12) {
                            // Is the value at offset 4 ignored?
                            info.atracSampleOffset = readUnaligned32(mem, currentAddr + 8); // The loop samples are offset by this value
                        } else {
                            info.atracSampleOffset = readUnaligned32(mem, currentAddr + 4); // The loop samples are offset by this value
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("FACT Chunk: chunkSize=%d, endSample=0x%X, sampleOffset=0x%X", chunkSize, info.atracEndSample, info.atracSampleOffset));
                        }
                    }
                    break;
                }
                case SMPL_CHUNK_MAGIC: {
                    if (chunkSize >= 36) {
                        int checkNumLoops = readUnaligned32(mem, currentAddr + 28);
                        if (chunkSize >= 36 + checkNumLoops * 24) {
                            info.numLoops = checkNumLoops;
                            info.loops = new LoopInfo[info.numLoops];
                            int loopInfoAddr = currentAddr + 36;
                            for (int i = 0; i < info.numLoops; i++) {
                                LoopInfo loop = new LoopInfo();
                                info.loops[i] = loop;
                                loop.cuePointID = readUnaligned32(mem, loopInfoAddr);
                                loop.type = readUnaligned32(mem, loopInfoAddr + 4);
                                loop.startSample = readUnaligned32(mem, loopInfoAddr + 8) - info.atracSampleOffset;
                                loop.endSample = readUnaligned32(mem, loopInfoAddr + 12) - info.atracSampleOffset;
                                loop.fraction = readUnaligned32(mem, loopInfoAddr + 16);
                                loop.playCount = readUnaligned32(mem, loopInfoAddr + 20);

                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Loop #%d: %s", i, loop.toString()));
                                }
                                loopInfoAddr += 24;
                            }
                            // TODO Second buffer processing disabled because still incomplete
                            //isSecondBufferNeeded = true;
                        }
                    }
                    break;
                }
            }

            if (chunkSize > bufferSize) {
                break;
            }

            currentAddr += chunkSize;
            bufferSize -= chunkSize;
        }

        if (info.loops != null) {
            // If a loop end is past the atrac end, assume the atrac end
            for (LoopInfo loop : info.loops) {
                if (loop.endSample > info.atracEndSample) {
                    loop.endSample = info.atracEndSample;
                }
            }
        }

        return result;
    }


    protected static class LoopInfo {
        protected int cuePointID;
        protected int type;
        protected int startSample;
        protected int endSample;
        protected int fraction;
        protected int playCount;

        @Override
        public String toString() {
            return String.format("LoopInfo[cuePointID %d, type %d, startSample 0x%X, endSample 0x%X, fraction %d, playCount %d]", cuePointID, type, startSample, endSample, fraction, playCount);
        }
    }

    public static class AtracFileInfo {
        public int atracBitrate = 64;
        public int atracChannels = 2;
        public int atracSampleRate = 0xAC44;
        public int atracBytesPerFrame = 0x0230;
        public int atracEndSample;
        public int atracSampleOffset;
        public int atracCodingMode;
        public int inputFileDataOffset;
        public int inputFileSize;
        public int inputDataSize;

        public int loopNum;
        public int numLoops;
        public LoopInfo[] loops;
    }

}
