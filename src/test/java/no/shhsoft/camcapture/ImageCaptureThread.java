package no.shhsoft.camcapture;

import no.shhsoft.thread.DaemonThread;
import no.shhsoft.utils.UncheckedInterruptedException;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class ImageCaptureThread
extends DaemonThread {

    private static final Logger LOG = Logger.getLogger(ImageCaptureThread.class.getName());
    private static final long MS_BETWEEN_VIDEO_IMAGE_UPDATES = 200L;
    private final ImageFrame imageFrame;
    private CamCaptureDevice webcam;

    static {
        ImageIO.setUseCache(false);
        //Webcam.setHandleTermSignal(true);
    }

    ImageCaptureThread(final ImageFrame imageFrame) {
        this.imageFrame = imageFrame;
        setVmShouldWaitForThisThread(false);
        setName(ImageCaptureThread.class.getName() + " thread");
    }

    @Override
    protected void beforeStart() {
        webcam = (CamCaptureDevice) CamCaptureDriver.getInstance().getDevices().get(0);
        if (webcam == null) {
            LOG.info("No default web camera. Won't capture images.");
            return;
        }
        LOG.info("Webcam is " + webcam.getName());
        final Dimension[] viewSizes = webcam.getResolutions();
        System.out.println("Available camera view sizes:");
        for (final Dimension viewSize : viewSizes) {
            System.out.println("  " + viewSize.getWidth() + "x" + viewSize.getHeight());
        }
        final Dimension prioritizedResolution = findLowestResolution(viewSizes);
        System.out.println("Using " + prioritizedResolution.getWidth() + "x" + prioritizedResolution.getHeight());
        webcam.setResolution(prioritizedResolution);
        webcam.open();
    }

    private Dimension findHighestResolution(final Dimension[] dimensions) {
        Dimension highest = null;
        for (final Dimension dimension : dimensions) {
            if (dimension.getWidth() <= dimension.getHeight()) {
                continue;
            }
            if (highest != null && dimension.getHeight() <= highest.getHeight()) {
                continue;
            }
            final double highestResolution = highest != null ? highest.getWidth() * highest.getHeight() : 0.0;
            final double resolution = dimension.getWidth() * dimension.getHeight();
            if (resolution > highestResolution) {
                highest = dimension;
            }
        }
        return highest;
    }

    private Dimension findLowestResolution(final Dimension[] dimensions) {
        Dimension lowest = null;
        for (final Dimension dimension : dimensions) {
            if (dimension.getWidth() <= dimension.getHeight()) {
                continue;
            }
            if (lowest != null && dimension.getHeight() >= lowest.getHeight()) {
                continue;
            }
            final double lowestResolution = lowest != null ? lowest.getWidth() * lowest.getHeight() : Double.MAX_VALUE;
            final double resolution = dimension.getWidth() * dimension.getHeight();
            if (resolution < lowestResolution) {
                lowest = dimension;
            }
        }
        return lowest;
    }

    @Override
    protected void afterStop() {
        if (webcam != null) {
            webcam.close();
            webcam = null;
        }
    }

    @Override
    public void run() {
        if (webcam == null) {
            return;
        }
        while (!shouldStop()) {
            final long before = System.currentTimeMillis();
            final BufferedImage image = webcam.getImage();
            imageFrame.setImage(image);
            final long timeToSleep = MS_BETWEEN_VIDEO_IMAGE_UPDATES - (System.currentTimeMillis() - before);
            if (timeToSleep > 0L) {
                try {
                    Thread.sleep(timeToSleep);
                } catch (final InterruptedException e) {
                    throw new UncheckedInterruptedException(e);
                }
            }
        }
    }

    public CamCaptureDevice getWebcam() {
        return webcam;
    }

}
