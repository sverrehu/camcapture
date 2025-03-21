package no.shhsoft.camcapture;

import com.github.sarxos.webcam.WebcamDevice;
import com.github.sarxos.webcam.WebcamDriver;

import java.util.List;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class CamCaptureDriver
implements WebcamDriver {

    private static CamCaptureDriver instance;

    private CamCaptureDriver() {
    }

    public static CamCaptureDriver getInstance() {
        if (instance == null) {
            instance = new CamCaptureDriver();
        }
        return instance;
    }

    @Override
    public List<WebcamDevice> getDevices() {
        /* TODO: For now, only a single device is supported, the default device. */
        return List.of(JniAdapter.getInstance().getDefaultDevice());
    }

    @Override
    public boolean isThreadSafe() {
        return false;
    }

}
