package net.techcable.accelerated_java.jni;

import lombok.*;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.PlatformDependent;

import net.techcable.accelerated_java.InvalidDataException;
import net.techcable.accelerated_java.compression.Compressor;

import static com.google.common.base.Preconditions.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ZLibNative {

    // Internal Magic

    private static native long createStream0(int level, int typeId);

    private static native int inflate0(long ctx, long communicationPtr, long srcPtr, int srcLength, long destPtr, int destLength) throws InvalidDataException;

    private static native int deflate0(long ctx, long communicationPtr, long srcPtr, int srcLength, long destPtr, int destLength, boolean finish);

    private static native void free0(long ctx, int typeId);

    // Public methods

    private static final int DEFLATE_TYPE_ID = 0;
    private static final int INFLATE_TYPE_ID = 1;

    public static NativeZlibStream createDecompressingStream() {
        ByteBuffer communicationBuf = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
        long nativePointer = createStream0(-1, INFLATE_TYPE_ID);
        return new NativeZlibStream(nativePointer, communicationBuf, NativeZlibStream.State.DECOMPRESSING);
    }

    public static NativeZlibStream createCompressingStream(int level) {
        ByteBuffer communicationBuf = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
        long nativePointer = createStream0(level, DEFLATE_TYPE_ID);
        return new NativeZlibStream(nativePointer, communicationBuf, NativeZlibStream.State.COMPRESSING);
    }

    public static final class NativeZlibStream implements Closeable {
        private final long pointer;
        private final ByteBuffer communicationBuf;
        private final long communicationBufPointer;
        private final int typeId;
        @Getter
        private volatile State state;

        private NativeZlibStream(long streamPointer, ByteBuffer communicationBuf, State state) {
            this.pointer = streamPointer;
            this.communicationBuf = checkNotNull(communicationBuf, "Null communication buffer");
            this.communicationBufPointer = Native.getNativeAddress(communicationBuf);
            this.state = checkNotNull(state, "Null state");
            switch (state) {
                case DECOMPRESSING:
                    typeId = INFLATE_TYPE_ID;
                    break;
                case COMPRESSING:
                    typeId = DEFLATE_TYPE_ID;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid starting state: " + state);
            }
        }

        public synchronized Status decompress(ByteBuf in, ByteBuf out) throws InvalidDataException {
            State state = getState();
            state.assertEquals(State.DECOMPRESSING);
            checkNotNull(in, "Null input memory");
            checkNotNull(out, "Null output memory");
            int statusCode = inflate0(
                    this.pointer,
                    communicationBufPointer,
                    Native.getNativeAddress(in) + in.readerIndex(), // Start reading here
                    in.readableBytes(),
                    Native.getNativeAddress(out) + out.writerIndex(), // Start writing here
                    out.writableBytes()
            );
            in.readerIndex(in.readerIndex() + this.getConsumed());
            out.writerIndex(out.writerIndex() + this.getWritten());
            Status status = Status.values()[statusCode];
            if (status == Status.FINISHED) {
                this.state = State.FINISHED;
            }
            return status;
        }

        public synchronized Status compress(ByteBuf in, ByteBuf out, boolean finish) {
            state.assertEquals(State.COMPRESSING);
            checkNotNull(in, "Null input memory");
            checkNotNull(out, "Null output memory");
            int statusCode = deflate0(
                    this.pointer,
                    communicationBufPointer,
                    Native.getNativeAddress(in) + in.readerIndex(), // Start reading here
                    in.readableBytes(),
                    Native.getNativeAddress(out) + out.writerIndex(), // Start writing here
                    out.writableBytes(),
                    finish
            );
            in.readerIndex(in.readerIndex() + this.getConsumed());
            out.writerIndex(out.writerIndex() + this.getWritten());
            Status status = Status.values()[statusCode];
            if (status == Status.FINISHED) {
                this.state = State.FINISHED;
            }
            return status;
        }

        private int getConsumed() {
            return communicationBuf.getInt(0);
        }

        private int getWritten() {
            return communicationBuf.getInt(4);
        }

        public synchronized void close() {
            state.assertNotEquals(State.CLOSED);
            this.state = State.CLOSED;
            PlatformDependent.freeDirectBuffer(communicationBuf);
        }

        @RequiredArgsConstructor
        public enum State {
            COMPRESSING(Compressor.State.COMPRESSING),
            DECOMPRESSING(Compressor.State.DECOMPRESSING),
            CLOSED(Compressor.State.CLOSED),
            FINISHED(Compressor.State.FINISHED);

            private void assertEquals(State expected) {
                checkNotNull(expected, "Null state");
                checkState(this == expected, "Caller is expected to check state is %s", expected);
            }

            private void assertNotEquals(State unexpected) {
                checkNotNull(unexpected, "Null state");
                checkState(this != unexpected, "Caller is expected to check state is not %s", unexpected);
            }

            @Getter
            private final Compressor.State compressorState;
        }
    }

    @RequiredArgsConstructor
    @Getter
    public enum Status {
        OK(Compressor.Status.OK),
        FINISHED(Compressor.Status.FINISHED);

        private final Compressor.Status compressorStatus;
    }
}
