package no.shhsoft.camcapture;

import no.shhsoft.jni.utils.JniLoader;

import java.util.Map;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
final class JniAdapter {

    private static JniAdapter instance;

    private JniAdapter() {
        JniLoader.loadNativeLibrary(Map.of(
            JniLoader.Os.MAC, "native-darwin.dylib"
        ));
        init();
    }

    public static JniAdapter getInstance() {
        if (instance == null) {
            instance = new JniAdapter();
        }
        return instance;
    }

    public native void init();

    public native CamCaptureDevice getDefaultDevice();

    public native void free(CamCaptureDevice camCaptureDevice);

    public native void setResolutionIndex(CamCaptureDevice camCaptureDevice, int index);

    public native int getResolutionIndex(CamCaptureDevice camCaptureDevice);

    public native void open(CamCaptureDevice camCaptureDevice);

    public native void close(CamCaptureDevice camCaptureDevice);

}
