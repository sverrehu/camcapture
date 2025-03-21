package no.shhsoft.camcapture;

import java.awt.image.BufferedImage;

interface ImageAnalyzer {

    /* May change the image too. */
    void analyze(BufferedImage image);

}
