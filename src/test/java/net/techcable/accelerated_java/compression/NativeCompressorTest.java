package net.techcable.accelerated_java.compression;

import java.io.File;

import net.techcable.accelerated_java.InvalidDataException;
import net.techcable.accelerated_java.jni.Native;

import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NativeCompressorTest extends AbstractCompressorTest {

    @BeforeClass
    public static void loadNative() {
        File nativesDirectory = new File("natives");
        if (!nativesDirectory.exists()) {
            System.err.println("Natives directory doesn't exist");
            throw new AssumptionViolatedException("Natives directory doesn't exist");
        }
        try {
            Native.LIBRARY.load(nativesDirectory);
        } catch (Throwable t) {
            System.err.println("(Non-Fatal) Unable to load native library");
            t.printStackTrace();
        }
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
