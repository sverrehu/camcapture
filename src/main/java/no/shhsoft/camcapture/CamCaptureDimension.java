package no.shhsoft.camcapture;

import java.awt.Dimension;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class CamCaptureDimension
extends Dimension {

    private final long nativeAddress;

    CamCaptureDimension(final long nativeAddress, final int width, final int height) {
        this.nativeAddress = nativeAddress;
        this.setSize(width, height);
    }

    @Override
    public String toString() {
        return width + "x" + height;
    }

}
