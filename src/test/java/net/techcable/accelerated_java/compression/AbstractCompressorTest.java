package net.techcable.accelerated_java.compression;

import java.util.Arrays;
import java.util.Random;
import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.techcable.accelerated_java.InvalidDataException;

import org.junit.Assert;

import static net.techcable.accelerated_java.utils.SimpleFormatter.*;

public abstract class AbstractCompressorTest {
    private static final Random RANDOM = new Random();

    protected void testRandomHeapBufferCompression(CompressorFactory factory) throws InvalidDataException {
        testCompression(factory, Allocator.HEAP, true);
    }

    protected void testRepeatingHeapBufferCompression(CompressorFactory factory) throws InvalidDataException {
        testCompression(factory, Allocator.HEAP, false);
    }

    protected void testRandomDirectBufferCompression(CompressorFactory factory) throws InvalidDataException {
        testCompression(factory, Allocator.DIRECT, true);
    }

    protected void testRepeatingDirectBufferCompression(CompressorFactory factory) throws InvalidDataException {
        testCompression(factory, Allocator.DIRECT, false);
    }

    protected void testInsufficientOutputHeapBuffer(CompressorFactory factory) {
        testInsufficientOutput(factory, Allocator.HEAP);
    }

    protected void testInsufficientOutputDirectBuffer(CompressorFactory factory) {
        testInsufficientOutput(factory, Allocator.DIRECT);
    }

    private void testInsufficientOutput(CompressorFactory factory, Allocator allocator) {
        ByteBuf in = allocator.allocate(2048);
        ByteBuf out = allocator.allocate(15, 15);
        Compressor compressor = factory.createCompressor();
        try {
            fillRandom(in);
            Compressor.Status status = compressor.compress(in, out, true);
            Assert.assertEquals("Somehow managed to compress 2048 random bytes into 15 output!", Compressor.Status.INSUFFICIENT_OUTPUT, status);
        } finally {
            in.release();
            out.release();
            compressor.close();
        }
    }

    protected void testCompressionEqual(CompressorFactory factory, boolean random) {
        Compressor heapCompressor = factory.createCompressor();
        Compressor directCompressor = factory.createCompressor();
        ByteBuf heapIn = Unpooled.buffer(2048);
        ByteBuf heapOut = Unpooled.buffer(2048);
        ByteBuf directIn = Unpooled.directBuffer(2048);
        ByteBuf directOut = Unpooled.directBuffer(2048);
        try {
            if (random) {
                fillRandom(heapIn);
            } else {
                fillRepeating(heapIn);
            }
            heapIn.getBytes(0, directIn);
            Compressor.Status heapStatus = heapCompressor.compress(heapIn, heapOut, true);
            Compressor.Status directStatus = directCompressor.compress(directIn, directOut, true);
            Assert.assertEquals("Heap buffer compression not finished!", Compressor.Status.FINISHED, heapStatus);
            Assert.assertEquals("Direct buffer compression not finished!", Compressor.Status.FINISHED, directStatus);
            Assert.assertEquals(format("Compressed {} bytes in heap but compressed {} for direct.", heapOut.readableBytes(), directOut.readableBytes()), heapOut.readableBytes(), heapOut.readableBytes());
            for (int index = 0; index < heapIn.writerIndex(); index++) {
                int heap = heapOut.getByte(index);
                int direct = directOut.getByte(index);
                Assert.assertEquals(format("Got from heap buffer {} at index {}, but got {} from direct", heap, index, direct), heap, direct);
            }
        } finally {
            heapIn.release();
            heapOut.release();
            directIn.release();
            directOut.release();
            heapCompressor.close();
            directCompressor.close();
        }
    }

    private void testCompression(CompressorFactory factory, Allocator allocator, boolean random) throws InvalidDataException {
        Compressor compressor = factory.createCompressor();
        Compressor decompressor = factory.createDecompressor();
        ByteBuf in = allocator.allocate(2048);
        ByteBuf out = allocator.allocate(2048);
        ByteBuf newData = null;
        try {
            if (random) {
                fillRandom(in);
            } else {
                fillRepeating(in);
            }
            Compressor.Status status = compressor.compress(in, out, true);
            Assert.assertEquals("Compression not finished!", Compressor.Status.FINISHED, status);
            newData = allocator.allocate(2048);
            status = decompressor.decompress(out, newData);
            Assert.assertEquals("Decompression not finished!", Compressor.Status.FINISHED, status);
            in.readerIndex(0);
            Assert.assertEquals(format("Decompressed {} bytes but had {} originally.", newData.readableBytes(), in.readableBytes()), in.readableBytes(), newData.readableBytes());
            for (int index = 0; index < in.writerIndex(); index++) {
                int expected = in.getByte(index);
                int actual = in.getByte(index);
                Assert.assertEquals(format("Got {} at index {}, but expected {}", expected, index, actual), expected, actual);
            }
        } finally {
            in.release();
            out.release();
            if (newData != null) newData.release();
            compressor.close();
            decompressor.close();
        }
    }

    private static void fillRepeating(ByteBuf buf) {
        while (buf.isWritable()) {
            byte val = (byte) RANDOM.nextInt(256);
            int length = Math.min(128, buf.writableBytes());
            for (int i = 0; i < length; i++) {
                buf.writeByte(val);
            }
        }
    }

    private static void fillRandom(ByteBuf buf) {
        while (buf.isWritable()) {
            byte val = (byte) RANDOM.nextInt(256);
            buf.writeByte(val);
        }
    }

    @FunctionalInterface
    private static interface Allocator {
        public static final Allocator HEAP = Unpooled::buffer;
        public static final Allocator DIRECT = Unpooled::directBuffer;

        public ByteBuf allocate(int capacity, int maxCapacity);

        public default ByteBuf allocate(int capacity) {
            return allocate(capacity, Integer.MAX_VALUE);
        }
    }
}
