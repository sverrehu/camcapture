package no.shhsoft.camcapture;

import com.github.sarxos.webcam.WebcamDevice;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class CamCaptureDevice
implements WebcamDevice {

    private final long nativeAddress;
    private final String name;
    private final CamCaptureDimension[] dimensions;
    private int activeDimensionIndex;
    private boolean opened;
    private boolean disposed;
    private volatile ByteBuffer transferBuffer;

    public CamCaptureDevice(final long nativeAddress, final String name, final CamCaptureDimension[] dimensions, final int activeDimensionIndex) {
        this.nativeAddress = nativeAddress;
        this.name = name;
        this.dimensions = dimensions;
        this.activeDimensionIndex = activeDimensionIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Dimension[] getResolutions() {
        return dimensions;
    }

    @Override
    public Dimension getResolution() {
        if (activeDimensionIndex < 0) {
            return null;
        }
        return dimensions[activeDimensionIndex];
    }

    @Override
    public void setResolution(final Dimension size) {
        if (!(size instanceof CamCaptureDimension)) {
            throw new RuntimeException("Size must be one of the Dimensions returned by getResolutions (wrong type)");
        }
        final CamCaptureDimension camCaptureDimension = (CamCaptureDimension) size;
        int selectedIndex = -1;
        for (int q = 0; q < dimensions.length; q++) {
            if (camCaptureDimension == dimensions[q]) {
                selectedIndex = q;
                break;
            }
        }
        if (selectedIndex < 0) {
            throw new RuntimeException("Size must be one of the Dimensions returned by getResolutions (not in list)");
        }
        JniAdapter.getInstance().setResolutionIndex(this, selectedIndex);
    }

    @Override
    public BufferedImage getImage() {
        if (!opened) {
            return null;
        }
        final int nativeResolutionIndex = JniAdapter.getInstance().getResolutionIndex(this);
        if (nativeResolutionIndex != activeDimensionIndex) {
            activeDimensionIndex = nativeResolutionIndex;
        }
        final Dimension resolution = getResolution();
        if (resolution == null) {
            throw new RuntimeException("Current resolution is null. This should never happen.");
        }
        final IntBuffer argbImageInt = transferBuffer.asIntBuffer();
        final int[] pixels = new int[resolution.width * resolution.height /*argbImageInt.capacity()*/];
        argbImageInt.get(pixels);
        final BufferedImage image = new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_INT_ARGB);
        final int[] rasterData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(pixels, 0, rasterData, 0, pixels.length);
        return image;
    }

    @Override
    public void open() {
        if (disposed) {
            throw new RuntimeException("Device has been disposed, and cannot be opened");
        }
        if (opened) {
            return;
        }
        if (getResolution() == null) {
            throw new RuntimeException("No resolution has been set");
        }
        if (transferBuffer == null) {
            transferBuffer = ByteBuffer.allocateDirect(4 * getMaxDimensionArea());
            transferBuffer.order(ByteOrder.nativeOrder());
        }
        JniAdapter.getInstance().open(this);
        opened = true;
    }

    private int getMaxDimensionArea() {
        int maxArea = 0;
        for (final Dimension dimension : dimensions) {
            final int area = dimension.width * dimension.height;
            if (area > maxArea) {
                maxArea = area;
            }
        }
        return maxArea;
    }

    @Override
    public void close() {
        if (!opened) {
            return;
        }
        JniAdapter.getInstance().close(this);
        transferBuffer = null;
        opened = false;
    }

    private void reopen() {
        close();
        open();
    }

    @Override
    public void dispose() {
        close();
        JniAdapter.getInstance().free(this);
        disposed = true;
    }

    @Override
    public boolean isOpen() {
        return opened;
    }

}
