package net.techcable.accelerated_java.compression;

import lombok.*;

import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import io.netty.buffer.ByteBuf;

import net.techcable.accelerated_java.InvalidDataException;

import static com.google.common.base.Preconditions.*;

class ZLibJDKCompressor implements Compressor {
    private final Deflater deflater;
    private final Inflater inflater;
    private final int level;
    @Getter
    private Compressor.State state;

    private static final int BUFFER_SIZE = 2048;

    @NonNull
    private synchronized Inflater getInflater() {
        state.requireState(State.DECOMPRESSING);
        return inflater;
    }

    @NonNull
    private synchronized Deflater getDeflater() {
        state.requireState(State.COMPRESSING);
        return deflater;
    }

    private ZLibJDKCompressor(int level) {
        this.deflater = new Deflater(level);
        this.inflater = null;
        this.level = -1;
        this.state = State.COMPRESSING;
    }

    private ZLibJDKCompressor() {
        this.deflater = null;
        this.inflater = new Inflater();
        this.level = -1;
        this.state = State.DECOMPRESSING;
    }

    @Override
    public synchronized int getLevel() {
        getState().requireStateNot(State.DECOMPRESSING, "Decompressing doesn't have a level!");
        return level;
    }

    @Override
    public synchronized void close() {
        switch (getState()) {
            case DECOMPRESSING:
                getInflater().end();
                break;
            case COMPRESSING:
                getDeflater().end();
                break;
            case CLOSED:
                throw new IllegalStateException("Already closed");
        }
    }

    @Override
    public Status decompress(ByteBuf in, ByteBuf out) throws InvalidDataException {
        checkNotNull(in, "Null input buffer");
        checkNotNull(in, "Null output buffer");
        in.retain();
        out.retain();
        try {
            checkNotNull(in, "Null input buffer");
            checkNotNull(in, "Null output buffer");
            in.retain();
            out.retain();
            try {
                synchronized (this) {
                    getState().requireState(State.DECOMPRESSING);
                    boolean needsMoreOutput;
                    byte[] outBuf = out.hasArray() ? null : new byte[BUFFER_SIZE];
                    byte[] inBuf = in.hasArray() ? null : new byte[BUFFER_SIZE];
                    do {
                        long oldTotalIn = getInflater().getTotalIn();
                        final byte[] inArray;
                        final int inArrayOffset, inArrayLength;
                        if (in.hasArray()) {
                            inArray = in.array();
                            inArrayOffset = in.arrayOffset() + in.readerIndex();
                            inArrayLength = in.readableBytes();
                        } else {
                            inArray = inBuf;
                            inArrayOffset = 0;
                            inArrayLength = Math.min(in.readableBytes(), BUFFER_SIZE);
                            in.getBytes(in.readerIndex(), inArray, inArrayOffset, inArrayLength);
                        }
                        getInflater().setInput(inArray, inArrayOffset, inArrayLength);
                        int writtenBytes;
                        do {
                            out.ensureWritable(Math.min(BUFFER_SIZE, out.maxWritableBytes()));
                            final byte[] outArray;
                            final int outArrayOffset, outArrayLength;
                            if (out.hasArray()) {
                                outArray = out.array();
                                outArrayOffset = out.arrayOffset() + out.writerIndex();
                                outArrayLength = out.writableBytes();
                            } else {
                                outArray = outBuf;
                                outArrayOffset = 0;
                                outArrayLength = Math.min(out.writableBytes(), BUFFER_SIZE);
                            }
                            writtenBytes = getInflater().inflate(outArray, outArrayOffset, outArrayLength);
                            if (!out.hasArray()) {
                                out.writeBytes(outArray, outArrayOffset, writtenBytes);
                            } else {
                                out.writerIndex(out.writerIndex() + writtenBytes);
                            }
                            needsMoreOutput = writtenBytes == outArrayLength;
                        } while (writtenBytes > 0 && needsMoreOutput && out.maxWritableBytes() > 0);
                        int readBytes = (int) (getInflater().getTotalIn() - oldTotalIn);
                        in.readerIndex(in.readerIndex() + readBytes);
                    } while (!getInflater().finished() && in.isReadable());
                    if (needsMoreOutput) {
                        return Status.INSUFFICIENT_OUTPUT;
                    } else if (getInflater().finished()) {
                        assert !in.isReadable();
                        state = State.FINISHED;
                        return Status.FINISHED;
                    } else {
                        return Status.OK;
                    }
                }
            } finally {
                in.release();
                out.release();
            }
        } catch (DataFormatException e) {
            throw new InvalidDataException(e.getMessage(), e);
        } finally {
            in.release();
            out.release();
        }
    }

    @Override
    public Status compress(ByteBuf in, ByteBuf out, boolean finish) {
        checkNotNull(in, "Null input buffer");
        checkNotNull(in, "Null output buffer");
        boolean haveFinished = false;
        in.retain();
        out.retain();
        try {
            synchronized (this) {
                getState().requireState(State.COMPRESSING);
                boolean needsMoreOutput;
                byte[] outBuf = out.hasArray() ? null : new byte[BUFFER_SIZE];
                byte[] inBuf = in.hasArray() ? null : new byte[BUFFER_SIZE];
                do {
                    long oldTotalIn = getDeflater().getTotalIn();
                    assert !haveFinished;
                    final byte[] inArray;
                    final int inArrayOffset, inArrayLength;
                    if (in.hasArray()) {
                        inArray = in.array();
                        inArrayOffset = in.arrayOffset() + in.readerIndex();
                        inArrayLength = in.readableBytes();
                    } else {
                        inArray = inBuf;
                        inArrayOffset = 0;
                        inArrayLength = Math.min(in.readableBytes(), BUFFER_SIZE);
                        in.getBytes(in.readerIndex(), inArray, inArrayOffset, inArrayLength);
                    }
                    if (finish && inArrayLength == in.readableBytes()) {
                        getDeflater().finish();
                        haveFinished = true;
                    }
                    getDeflater().setInput(inArray, inArrayOffset, inArrayLength);
                    int writtenBytes;
                    do {
                        out.ensureWritable(Math.min(BUFFER_SIZE, out.maxWritableBytes()));
                        final byte[] outArray;
                        final int outArrayOffset, outArrayLength;
                        if (out.hasArray()) {
                            outArray = out.array();
                            outArrayOffset = out.arrayOffset() + out.writerIndex();
                            outArrayLength = out.writableBytes();
                        } else {
                            outArray = outBuf;
                            outArrayOffset = 0;
                            outArrayLength = Math.min(out.writableBytes(), BUFFER_SIZE);
                        }
                        writtenBytes = getDeflater().deflate(outArray, outArrayOffset, outArrayLength, Deflater.SYNC_FLUSH);
                        if (!out.hasArray()) {
                            out.writeBytes(outArray, outArrayOffset, writtenBytes);
                        } else {
                            out.writerIndex(out.writerIndex() + writtenBytes);
                        }
                        needsMoreOutput = writtenBytes == outArrayLength;
                    } while (writtenBytes > 0 && needsMoreOutput && out.maxWritableBytes() > 0);
                    int readBytes = (int) (getDeflater().getTotalIn() - oldTotalIn);
                    in.readerIndex(in.readerIndex() + readBytes);
                } while (!getDeflater().finished() && in.isReadable());
                if (needsMoreOutput) {
                    return Status.INSUFFICIENT_OUTPUT;
                } else if (getDeflater().finished()) {
                    assert !in.isReadable();
                    assert haveFinished;
                    state = State.FINISHED;
                    return Status.FINISHED;
                } else {
                    return Status.OK;
                }
            }
        } finally {
            in.release();
            out.release();
        }
    }

    @Override
    public CompressionType getType() {
        return CompressionType.ZLIB;
    }

    public static final Factory FACTORY = new Factory();

    private static class Factory implements CompressorFactory {

        @Override
        public Compressor createDecompressor() {
            return new ZLibJDKCompressor();
        }

        @Override
        public Compressor createCompressor(int level) {
            return new ZLibJDKCompressor(level);
        }

        @Override
        public CompressionType getType() {
            return CompressionType.ZLIB;
        }
    }
}
