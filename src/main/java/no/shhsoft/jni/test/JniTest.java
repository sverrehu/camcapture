package no.shhsoft.jni.test;

import com.github.sarxos.webcam.WebcamDevice;
import no.shhsoft.camcapture.CamCaptureDriver;

import java.awt.Dimension;
import java.util.List;

public final class JniTest {

    public static void main(final String[] args) {
        System.out.println("Running JNI Test");
        final CamCaptureDriver driver = CamCaptureDriver.getInstance();
        final List<WebcamDevice> devices = driver.getDevices();
        if (devices.isEmpty()) {
            throw new RuntimeException("No devices found");
        }
        final WebcamDevice device = devices.getFirst();
        System.out.println("Device name: " + device.getName());
        System.out.println("Number of dimensions: " + device.getResolutions().length);
        for (final Dimension dimension : device.getResolutions()) {
            System.out.println("  Resolution: " + dimension);
        }
        System.out.println("Active dimension: " + device.getResolution());
        //device.setResolution(device.getResolutions()[device.getResolutions().length - 1]);
        device.open();

        try {
            Thread.sleep(500000);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        device.close();
        device.dispose();
    }

}
