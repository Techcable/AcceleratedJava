package net.techcable.accelerated_java.utils;

import lombok.*;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;

import static com.google.common.base.Preconditions.*;

@Getter
public final class NativeLibrary {
    private final String name;
    private final ImmutableSet<String> dependencies;
    private volatile boolean loaded;

    private static final ConcurrentMap<String, NativeLibrary> libraries = new MapMaker().weakValues().makeMap();

    private NativeLibrary(String name, ImmutableSet<String> dependencies) {
        this.name = checkNotNull(name, "Null library name");
        this.dependencies = checkNotNull(dependencies, "Null dependencies");
        checkArgument(isValidLibraryName(name), "Invalid library name '%s'", name);
        dependencies.forEach((dependencyName) -> {
            checkArgument(isValidLibraryName(dependencyName), "Invalid dependency name '%s'", dependencyName);
        });
    }

    @Synchronized
    public void load(File nativesDirectory) {
        checkNotNull(nativesDirectory, "Null natives directory.");
        checkArgument(nativesDirectory.exists(), "Natives directory '%s' doesn't exist", nativesDirectory);
        checkArgument(nativesDirectory.isDirectory(), "Natives directory '%s' isn't a directory", nativesDirectory);
        checkState(!loaded, "Library is already loaded");
        for (String dependency : dependencies) {
            try {
                System.loadLibrary(dependency);
            } catch (UnsatisfiedLinkError e) {
                File f = new File(nativesDirectory, System.mapLibraryName(dependency));
                if (!f.exists()) throw new UnsatisfiedLinkError("Can't find dependency " + dependency + " for library " + getName() + " at " + f);
                System.load(f.getAbsolutePath());
            }
        }
        File f = new File(nativesDirectory, System.mapLibraryName(getName()));
        if (!f.exists()) throw new UnsatisfiedLinkError("Can't find library " + getName() + " at " + f);
        System.load(f.getAbsolutePath());
        loaded = true;
    }

    public static Optional<NativeLibrary> getLibrary(String name) {
        return Optional.ofNullable(libraries.get(name));
    }

    public static NativeLibrary createLibrary(String name, ImmutableSet<String> dependencies) {
        NativeLibrary library = new NativeLibrary(name, dependencies);
        NativeLibrary oldLibrary = libraries.putIfAbsent(name, library);
        if (oldLibrary != null) {
            checkState(oldLibrary.equals(library), "A library %s already exists but with dependencies %s instead of %s", name, oldLibrary.getDependencies(), dependencies);
            return oldLibrary; // Return the existing one
        } else {
            return library;
        }
    }

    private static boolean isValidLibraryName(String name) {
        checkNotNull(name, "Null name");
        int length = name.length();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')) {
                return false;
            }
        }
        return length > 0;
    }

    @Override
    public boolean equals(Object obj) {
        NativeLibrary library;
        return obj == this || obj != null && obj instanceof NativeLibrary && (library = (NativeLibrary) obj).name.equals(this.name) && library.dependencies.equals(this.dependencies);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

}