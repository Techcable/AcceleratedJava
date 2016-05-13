package net.techcable.accelerated_java.compression;

import lombok.*;

import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.base.VerifyException;

import io.netty.buffer.ByteBuf;

import net.techcable.accelerated_java.InvalidDataException;
import net.techcable.accelerated_java.jni.Native;
import net.techcable.accelerated_java.jni.ZLibNative;

import static com.google.common.base.Preconditions.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class ZLibNativeCompressor implements Compressor {
    private final int level;
    private final ZLibNative.NativeZlibStream nativeStream;

    private static final int BUFFER_SIZE = 4096;

    @Override
    public int getLevel() {
        synchronized (nativeStream) {
            getState().requireStateNot(State.DECOMPRESSING, "Compressing doesn't have a level!");
            return level;
        }
    }

    @Override
    public State getState() {
        return nativeStream.getState().getCompressorState();
    }

    @Override
    public CompressionType getType() {
        return CompressionType.ZLIB;
    }

    @Override
    public void close() {
        synchronized (nativeStream) {
            getState().requireStateNot(State.CLOSED, "Stream is already closed");
            nativeStream.close();
        }
    }

    @Override
    public Status decompress(ByteBuf in, ByteBuf out) throws InvalidDataException {
        checkNotNull(in, "Null input buffer");
        checkNotNull(in, "Null output buffer");
        in.retain();
        out.retain();
        try {
            synchronized (this) {
                getState().requireState(State.DECOMPRESSING);
                boolean needsMoreOutput;
                final boolean inHasNativeAddress = Native.hasNativeAddress(in);
                final boolean outHasNativeAddress = Native.hasNativeAddress(in);
                final ByteBuf inNativeBuf = inHasNativeAddress ? null : out.alloc().directBuffer(BUFFER_SIZE);
                if (inNativeBuf != null && !Native.hasNativeAddress(inNativeBuf))
                    throw new VerifyException("Direct buffer " + inNativeBuf.getClass().getName() + " doesn't have native address!");
                final ByteBuf outNativeBuf = outHasNativeAddress ? null : in.alloc().directBuffer(BUFFER_SIZE);
                if (outNativeBuf != null && !Native.hasNativeAddress(outNativeBuf))
                    throw new VerifyException("Direct buffer " + outNativeBuf.getClass().getName() + " doesn't have native address!");
                ZLibNative.Status nativeStatus;
                try {
                    do {
                        final ByteBuf nativeIn;
                        if (inHasNativeAddress) {
                            nativeIn = in;
                        } else {
                            nativeIn = inNativeBuf;
                            int length = Math.min(in.readableBytes(), BUFFER_SIZE);
                            nativeIn.readerIndex(0);
                            in.getBytes(in.readerIndex(), nativeIn, 0, length);
                            nativeIn.writerIndex(length);
                            assert nativeIn.writerIndex() == length;
                        }
                        int oldReaderIndex = nativeIn.readerIndex();
                        int writtenBytes;
                        do {
                            int minWritableBytes = Math.min(BUFFER_SIZE, out.maxWritableBytes());
                            out.ensureWritable(minWritableBytes);
                            final ByteBuf nativeOut;
                            final int nativeLength;
                            if (outHasNativeAddress) {
                                nativeOut = out;
                                nativeLength = out.writableBytes();
                            } else {
                                nativeOut = outNativeBuf;
                                nativeOut.capacity(minWritableBytes);
                                nativeOut.writerIndex(0);
                                nativeLength = minWritableBytes;
                            }
                            int oldWriterIndex = nativeOut.writerIndex();
                            nativeStatus = nativeStream.decompress(nativeIn, nativeOut);
                            writtenBytes = nativeOut.writerIndex() - oldWriterIndex;
                            if (nativeOut != out) {
                                nativeOut.readerIndex(0);
                                out.writeBytes(nativeOut);
                            }
                            needsMoreOutput = writtenBytes == nativeLength;
                        } while (writtenBytes > 0 && needsMoreOutput && out.maxWritableBytes() > 0);
                        int readBytes = nativeIn.readerIndex() - oldReaderIndex;
                        if (nativeIn != in) in.readerIndex(in.readerIndex() + readBytes);
                    }
                    while (nativeStatus != ZLibNative.Status.FINISHED && in.isReadable());
                    if (needsMoreOutput) {
                        return Status.INSUFFICIENT_OUTPUT;
                    } else if (nativeStatus == ZLibNative.Status.FINISHED) {
                        assert !in.isReadable();
                        assert getState() == State.FINISHED;
                        return Status.FINISHED;
                    } else {
                        return Status.OK;
                    }
                } finally {
                    if (inNativeBuf != null) inNativeBuf.release();
                    if (outNativeBuf != null) outNativeBuf.release();
                }
            }
        } finally {
            in.release();
            out.release();
        }
    }

    @Override
    public Status compress(ByteBuf in, ByteBuf out, boolean finish) {
        checkNotNull(in, "Null input buffer");
        checkNotNull(in, "Null output buffer");
        in.retain();
        out.retain();
        try {
            synchronized (this) {
                getState().requireState(State.COMPRESSING);
                boolean needsMoreOutput;
                final boolean inHasNativeAddress = Native.hasNativeAddress(in);
                final boolean outHasNativeAddress = Native.hasNativeAddress(in);
                final ByteBuf inNativeBuf = inHasNativeAddress ? null : out.alloc().directBuffer(BUFFER_SIZE);
                if (inNativeBuf != null && !Native.hasNativeAddress(inNativeBuf)) throw new VerifyException("Direct buffer " + inNativeBuf.getClass().getName() + " doesn't have native address!");
                final ByteBuf outNativeBuf = outHasNativeAddress ? null : in.alloc().directBuffer(BUFFER_SIZE);
                if (outNativeBuf != null && !Native.hasNativeAddress(outNativeBuf)) throw new VerifyException("Direct buffer " + outNativeBuf.getClass().getName() + " doesn't have native address!");
                ZLibNative.Status nativeStatus;
                try {
                    do {
                        final ByteBuf nativeIn;
                        if (inHasNativeAddress) {
                            nativeIn = in;
                        } else {
                            nativeIn = inNativeBuf;
                            int length = Math.min(in.readableBytes(), BUFFER_SIZE);
                            nativeIn.readerIndex(0);
                            in.getBytes(in.readerIndex(), nativeIn, 0, length);
                            nativeIn.writerIndex(length);
                            assert nativeIn.writerIndex() == length;
                        }
                        int oldReaderIndex = nativeIn.readerIndex();
                        int writtenBytes;
                        do {
                            int minWritableBytes = Math.min(BUFFER_SIZE, out.maxWritableBytes());
                            out.ensureWritable(minWritableBytes);
                            final ByteBuf nativeOut;
                            final int nativeLength;
                            if (outHasNativeAddress) {
                                nativeOut = out;
                                nativeLength = out.writableBytes();
                            } else {
                                nativeOut = outNativeBuf;
                                nativeOut.clear();
                                nativeOut.capacity(minWritableBytes);
                                nativeLength = minWritableBytes;
                            }
                            int oldWriterIndex = nativeOut.writerIndex();
                            nativeStatus = nativeStream.compress(nativeIn, nativeOut, finish);
                            writtenBytes = nativeOut.writerIndex() - oldWriterIndex;
                            if (nativeOut != out) {
                                nativeOut.readerIndex(0);
                                out.writeBytes(nativeOut);
                            }
                            needsMoreOutput = writtenBytes == nativeLength;
                        } while (writtenBytes > 0 && needsMoreOutput && out.maxWritableBytes() > 0);
                        int readBytes = nativeIn.readerIndex() - oldReaderIndex;
                        if (nativeIn != in) in.readerIndex(in.readerIndex() + readBytes);
                    }
                    while (nativeStatus != ZLibNative.Status.FINISHED && in.isReadable());
                    if (needsMoreOutput) {
                        return Status.INSUFFICIENT_OUTPUT;
                    } else if (nativeStatus == ZLibNative.Status.FINISHED) {
                        assert !in.isReadable();
                        assert getState() == State.FINISHED;
                        return Status.FINISHED;
                    } else {
                        return Status.OK;
                    }
                } finally {
                    if (inNativeBuf != null) inNativeBuf.release();
                    if (outNativeBuf != null) outNativeBuf.release();
                }
            }
        } finally {
            in.release();
            out.release();
        }
    }

    public static final Factory FACTORY = new Factory();

    private static class Factory implements CompressorFactory {

        @Override
        public Compressor createDecompressor() {
            return new ZLibNativeCompressor(-1, ZLibNative.createDecompressingStream());
        }

        @Override
        public Compressor createCompressor(int level) {
            return new ZLibNativeCompressor(level, ZLibNative.createCompressingStream(level));
        }

        @Override
        public CompressionType getType() {
            return CompressionType.ZLIB;
        }
    }
}
