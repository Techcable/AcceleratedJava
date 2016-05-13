package net.techcable.accelerated_java.compression;

import net.techcable.accelerated_java.jni.Native;
import net.techcable.accelerated_java.jni.ZLibNative;

public interface CompressorFactory {
    /**
     * Compression level for fastest compression.
     */
    public static final int BEST_SPEED = 1;

    /**
     * Compression level for best compression.
     */
    public static final int BEST_COMPRESSION = 9;

    /**
     * Default compression level.
     */
    public static final int DEFAULT_COMPRESSION = -1;

    public Compressor createDecompressor();

    public default Compressor createCompressor() {
        return createCompressor(DEFAULT_COMPRESSION);
    }

    public Compressor createCompressor(int level);

    public CompressionType getType();

    public static final CompressorFactory JDK = ZLibJDKCompressor.FACTORY;

    public static CompressorFactory getInstance() {
        if (Native.LIBRARY.isLoaded()) {
            return ZLibNativeCompressor.FACTORY;
        } else {
            return JDK;
        }
    }
}
