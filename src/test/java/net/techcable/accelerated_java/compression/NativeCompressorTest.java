package net.techcable.accelerated_java.compression;

import java.io.File;

import net.techcable.accelerated_java.InvalidDataException;
import net.techcable.accelerated_java.jni.Native;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NativeCompressorTest extends AbstractCompressorTest {

    @BeforeClass
    public static void loadNative() {
        File nativesDirectory = new File("natives");
        Assume.assumeTrue(nativesDirectory.exists());
        Native.LIBRARY.tryLoad(nativesDirectory);
        Assume.assumeTrue(Native.LIBRARY.isLoaded());
    }

    @Test
    public void testRandomHeapBufferCompression() throws InvalidDataException {
        super.testRandomHeapBufferCompression(CompressorFactory.getInstance());
    }

    @Test
    public void testRandomDirectBufferCompression() throws InvalidDataException {
        super.testRandomDirectBufferCompression(CompressorFactory.getInstance());
    }

    @Test
    public void testRepeatingHeapBufferCompression() throws InvalidDataException {
        super.testRepeatingHeapBufferCompression(CompressorFactory.getInstance());
    }

    @Test
    public void testRepeatingDirectBufferCompression() throws InvalidDataException {
        super.testRepeatingDirectBufferCompression(CompressorFactory.getInstance());
    }

    @Test
    public void testInsufficientOutputDirectBuffer() {
        super.testInsufficientOutputDirectBuffer(CompressorFactory.getInstance());
    }

    @Test
    public void testInsufficientOutputHeapBuffer() {
        super.testInsufficientOutputHeapBuffer(CompressorFactory.getInstance());
    }

    @Test
    public void testRandomCompressionEquals() {
        super.testCompressionEqual(CompressorFactory.getInstance(), true);
    }

    @Test
    public void testRepeatingCompressionEquals() {
        super.testCompressionEqual(CompressorFactory.getInstance(), false);
    }
}
