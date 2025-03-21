package no.shhsoft.jni.utils;

import no.shhsoft.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class JniLoader {

    private static boolean loaded = false;

    public enum Os {
        MAC,
        LINUX,
        FREEBSD,
        SOLARIS,
        WINDOWS
    }

    private JniLoader() {
    }

    public static synchronized void loadNativeLibrary(final Map<Os, String> osToLibraryBaseNameMap) {
        if (loaded) {
            return;
        }
        try {
            final Path tempDirectory = Files.createTempDirectory(null);
            tempDirectory.toFile().deleteOnExit();
            final String libraryPath = tempDirectory.toString();
            final Os os = getOs();
            final String baseName = osToLibraryBaseNameMap.get(os);
            if (baseName == null) {
                throw new RuntimeException("Unsupported OS: \"" + os + "\"");
            }
            final byte[] data = IoUtils.readResource("lib/" + baseName);
            final File libraryFile = new File(libraryPath, baseName);
            IoUtils.writeFile(libraryFile.toString(), data);
            libraryFile.deleteOnExit();
            System.load(libraryPath + File.separator + baseName);
            loaded = true;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Os getOs() {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return Os.MAC;
        }
        if (os.contains("win")) {
            return Os.WINDOWS;
        }
        if (os.contains("linux")) {
            return Os.LINUX;
        }
        else if (os.contains("sun")) {
            return Os.SOLARIS;
        }
        else if (os.contains("free")) {
            return Os.FREEBSD;
        }
        throw new RuntimeException("Unable to extract supported OS from \"" + os + "\"");
    }

}
