package net.techcable.accelerated_java.jni;

import lombok.*;

import java.nio.ByteBuffer;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.internal.PlatformDependent;

import net.techcable.accelerated_java.utils.NativeLibrary;

import static com.google.common.base.Preconditions.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Native {

    public static final NativeLibrary LIBRARY = NativeLibrary.createLibrary("acceleratedJava", ImmutableSet.of("z"));

    private static final boolean HAS_UNSAFE = PlatformDependent.hasUnsafe();

    private static native long getNativeAddress0(ByteBuffer nioBuffer);

    /* default */ static long getNativeAddress(ByteBuffer buffer) {
        checkNotNull(buffer, "Null buffer");
        if (!buffer.isDirect())
            throw new IllegalArgumentException(buffer.getClass().getName() + " isn't a direct buffer");
        if (HAS_UNSAFE) {
            return PlatformDependent.directBufferAddress(buffer);
        } else {
            return getNativeAddress0(buffer);
        }
    }

    /* default */ static long getNativeAddress(ByteBuf buf) {
        checkNotNull(buf, "Null buffer");
        if (buf.hasMemoryAddress()) {
            return buf.memoryAddress();
        } else if (buf.nioBufferCount() == 1 && buf.nioBuffer().isDirect()) {
            return getNativeAddress(buf.nioBuffer());
        } else {
            throw new IllegalArgumentException("No native address for buffer " + buf.getClass().getName());
        }
    }

    public static boolean hasNativeAddress(ByteBuf buf) {
        return buf.hasMemoryAddress() || buf.isDirect() && buf.nioBuffer().isDirect();
    }

    public static ByteBuf createNative(ByteBufAllocator allocator, int capacity) {
        return createNative(allocator, capacity, Integer.MAX_VALUE);
    }

    public static ByteBuf createNative(ByteBufAllocator allocator, int capacity, int maxCapacity) {
        ByteBuf direct = allocator.directBuffer(capacity, maxCapacity);
        Verify.verify(hasNativeAddress(direct), "Direct buffer %s doesn't have native address!", direct.getClass().getName());
        return direct;
    }
}
