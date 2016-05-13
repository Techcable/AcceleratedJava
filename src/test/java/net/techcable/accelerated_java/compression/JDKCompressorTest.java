package net.techcable.accelerated_java.compression;

import net.techcable.accelerated_java.InvalidDataException;

import org.junit.Test;

public class JDKCompressorTest extends AbstractCompressorTest {
    @Test
    public void testRandomHeapBufferCompression() throws InvalidDataException {
        super.testRandomHeapBufferCompression(CompressorFactory.JDK);
    }

    @Test
    public void testRandomDirectBufferCompression() throws InvalidDataException {
        super.testRandomDirectBufferCompression(CompressorFactory.JDK);
    }

    @Test
    public void testRepeatingHeapBufferCompression() throws InvalidDataException {
        super.testRepeatingHeapBufferCompression(CompressorFactory.JDK);
    }

    @Test
    public void testRepeatingDirectBufferCompression() throws InvalidDataException {
        super.testRepeatingDirectBufferCompression(CompressorFactory.JDK);
    }

    @Test
    public void testInsufficientOutputDirectBuffer() {
        super.testInsufficientOutputDirectBuffer(CompressorFactory.JDK);
    }

    @Test
    public void testInsufficientOutputHeapBuffer() {
        super.testInsufficientOutputHeapBuffer(CompressorFactory.JDK);
    }

    @Test
    public void testRandomCompressionEquals() {
        super.testCompressionEqual(CompressorFactory.JDK, true);
    }

    @Test
    public void testRepeatingCompressionEquals() {
        super.testCompressionEqual(CompressorFactory.JDK, false);
    }
}
