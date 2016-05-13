package net.techcable.accelerated_java.compression;

import lombok.*;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.zip.DataFormatException;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.techcable.accelerated_java.InvalidDataException;

public interface Compressor {

    /**
     * Get this stream's compression level
     *
     * @return the stream's compression level
     * @throws IllegalStateException if the stream is only for decompressing
     */
    public int getLevel();

    /**
     * Free this compressor
     * <p>Sets the state to {@link State#CLOSED}</p>
     */
    public void close();

    /**
     * Decompress as much data as possible into the given output buffer
     *
     * @param in  the data to decompress
     * @param out the output buffer
     * @return the status of the decompression
     * @throws IllegalStateException if the stream is only for compressing, or if the stream is closed
     * @throws NullPointerException  if any arguments are null
     * @throws InvalidDataException  if the compressed data is invalid
     */
    public Status decompress(ByteBuf in, ByteBuf out) throws InvalidDataException;


    /**
     * Compress as much data as possible into the given output buffer
     *
     * @param in     the data to compress
     * @param out    the output buffer
     * @return the status of the compression
     * @throws IllegalStateException if the stream is only for decompressing, if the stream is closed, or if the stream is finished
     * @throws NullPointerException  if any arguments are null
     */
    public default Status compress(ByteBuf in, ByteBuf out) {
        return compress(in, out, false);
    }

    /**
     * Compress as much data as possible into the given output buffer
     *
     * @param in     the data to compress
     * @param out    the output buffer
     * @param finish if we should finish the compression
     * @return the status of the compression
     * @throws IllegalStateException if the stream is only for decompressing, if the stream is closed, or if the stream is finished
     * @throws NullPointerException  if any arguments are null
     */
    public Status compress(ByteBuf in, ByteBuf out, boolean finish);

    /**
     * Decompress as much data as possible into the given output buffer
     *
     * @param in  the data to decompress
     * @param out the output buffer
     * @return the status of the decompression
     * @throws IllegalStateException if the stream is only for compressing, or if the stream is closed
     * @throws NullPointerException  if any arguments are null
     * @throws InvalidDataException  if the compressed data is invalid
     */
    public default Status decompress(ByteBuffer in, ByteBuffer out) throws InvalidDataException {
        return decompress(Unpooled.wrappedBuffer(in), Unpooled.wrappedBuffer(out));
    }


    /**
     * Compress as much data as possible into the given output buffer
     *
     * @param in  the data to compress
     * @param out the output buffer
     * @return the status of the compression
     * @throws IllegalStateException if the stream is only for decompressing, or if the stream is closed
     * @throws NullPointerException  if any arguments are null
     */
    public default Status compress(ByteBuffer in, ByteBuffer out) {
        return compress(Unpooled.wrappedBuffer(in), Unpooled.wrappedBuffer(out));
    }

    public State getState();

    public CompressionType getType();

    @RequiredArgsConstructor
    @Getter
    public enum State {
        COMPRESSING("Stream is only for compressing!"),
        DECOMPRESSING("Stream is only for decompressing!"),
        FINISHED("Stream is finished!"),
        CLOSED("Stream is closed!");

        public void requireState(State expectedState) {
            requireState(expectedState, getErrorMsg());
        }

        public void requireState(State expectedState, String msg) {
            Preconditions.checkNotNull(expectedState, "Null state");
            Preconditions.checkNotNull(msg, "Null message");
            if (this != expectedState) {
                throw new IllegalStateException(msg);
            }
        }


        public void requireStateNot(State unexpectedState, String msg) {
            Preconditions.checkNotNull(unexpectedState, "Null state");
            Preconditions.checkNotNull(msg, "Null message");
            if (this == unexpectedState) {
                throw new IllegalArgumentException(msg);
            }
        }

        private final String errorMsg;
    }

    public enum Status {
        OK,
        INSUFFICIENT_OUTPUT,
        FINISHED;
    }
}
