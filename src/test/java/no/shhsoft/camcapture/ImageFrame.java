package no.shhsoft.camcapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * @author <a href="mailto:shh@thathost.com">Sverre H. Huseby</a>
 */
public final class ImageFrame
extends JFrame {

    private final MyCanvas canvas;

    private static final class MyCanvas
    extends Canvas
    implements ComponentListener {

        private static final long serialVersionUID = 1L;
        private BufferedImage image;
        private Dimension imageDimension;
        private double scale = 1.0;
        private Rectangle rectangle;
        private BufferStrategy bufferStrategy;
        private boolean initDone = false;
        private long scaleVisibleUntil = 0L;

        MyCanvas() {
            addComponentListener(this);
            setSize(new Dimension(640, 480));
        }

        private synchronized void init() {
            if (initDone || !isDisplayable() || getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            createBufferStrategy(2);
            bufferStrategy = getBufferStrategy();
            initDone = true;
        }

        synchronized void setRectangle(final Rectangle rectangle) {
            this.rectangle = rectangle;
            updateScale();
        }

        synchronized void setImage(final BufferedImage image) {
            this.image = image;
            this.imageDimension = image == null ? null : new Dimension(image.getWidth(), image.getHeight());
            if (initDone && rectangle == null) {
                setRectangle(createRectangle(getWidth(), getHeight()));
            }
            updateScale();
        }

        synchronized void flush() {
            if (bufferStrategy == null) {
                return;
            }
            if (rectangle == null || image == null) {
                return;
            }
            do {
                do {
                    final Graphics2D g2 = (Graphics2D) bufferStrategy.getDrawGraphics();
                    g2.setColor(Color.BLACK);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.drawImage(image, rectangle.x, rectangle.y, (int) (scale * image.getWidth()), (int) (scale * image.getHeight()), null);

                    if (System.currentTimeMillis() < scaleVisibleUntil) {
                        final String text = String.format(Locale.US, "%.1f%%", Double.valueOf(scale * 100.0));
                        g2.setColor(Color.WHITE);
                        final Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(text, g2);
                        final int textX = (getWidth() - (int) stringBounds.getWidth()) / 2;
                        final int textY = (getHeight() - (int) stringBounds.getHeight()) / 2;
                        g2.drawString(text, textX, textY + g2.getFontMetrics().getMaxAscent());
                    }

                    g2.dispose();
                } while (bufferStrategy.contentsRestored());
                bufferStrategy.show();
            } while (bufferStrategy.contentsLost());
        }

        @Override
        public void paint(final Graphics g) {
            flush();
        }

        @Override
        public void update(final Graphics g) {
            paint(g);
        }

        @Override
        public void componentHidden(final ComponentEvent e) {
        }

        @Override
        public void componentMoved(final ComponentEvent e) {
        }

        @Override
        public void componentResized(final ComponentEvent e) {
            init();
            final Rectangle rectangle = createRectangle(getWidth(), getHeight());
            setRectangle(rectangle);
            flush();
        }

        @Override
        public void componentShown(final ComponentEvent e) {
            init();
        }

        private synchronized Rectangle createRectangle(final int areaWidth, final int areaHeight) {
            if (imageDimension == null) {
                return null;
            }
            final Dimension dim = resizeAndKeepAspectRatio(imageDimension, new Dimension(areaWidth, areaHeight));
            final Point pos = new Point((areaWidth - dim.width) / 2,
                                        (areaHeight - dim.height) / 2);
            return new Rectangle(pos, dim);
        }

        private static Dimension resizeAndKeepAspectRatio(final Dimension contentSize, final Dimension containerSize) {
            int newWidth = containerSize.width;
            double scale = (double) newWidth / contentSize.width;
            int newHeight = (int) (contentSize.height * scale);
            if (newHeight > containerSize.height) {
                scale = (double) containerSize.height / contentSize.height;
                newHeight = containerSize.height;
                newWidth = (int) (contentSize.width * scale);
            }
            return new Dimension(newWidth, newHeight);
        }

        private synchronized void updateScale() {
            if (image != null && rectangle != null) {
                final double newScale = rectangle.getWidth() / image.getWidth();
                if (newScale != scale) {
                    scale = newScale;
                    scaleVisibleUntil = System.currentTimeMillis() + 3000L;
                }
            }
        }

    }

    private void shutdown() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.exit(0);
            }
        }).start();
    }

    ImageFrame() {
        canvas = new MyCanvas();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(canvas, BorderLayout.CENTER);
        setSize(640, 480);
        pack();
    }

    public void setImage(final BufferedImage image) {
        canvas.setImage(image);
        canvas.flush();
    }

}
