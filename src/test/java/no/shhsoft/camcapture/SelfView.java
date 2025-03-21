package no.shhsoft.camcapture;

import no.shhsoft.utils.ThreadUtils;

import java.awt.Dimension;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class SelfView {

    public static void main(final String[] args) {
        final ImageFrame imageFrame = new ImageFrame();
        imageFrame.setVisible(true);
        final ImageCaptureThread imageCaptureThread = new ImageCaptureThread(imageFrame);
        imageCaptureThread.start();
        final CamCaptureDevice webcam = imageCaptureThread.getWebcam();
        final Dimension[] resolutions = webcam.getResolutions();
        for (;;) {
            ThreadUtils.sleep(4000);
            webcam.setResolution(resolutions[resolutions.length - 1]);
            ThreadUtils.sleep(4000);
            webcam.setResolution(resolutions[0]);
        }
    }

}
