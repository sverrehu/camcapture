# camcapture - Mac Camera to Java BufferedImage

Keywords: Java, JNI, Mac, MacOS, WebCam, Camera, native, Image, BufferedImage

Very simple, not production-ready, code to capture camera input from a Mac and make it available to Java.
Just enough for my needs.
Lots of TODOs and gotchas, but made publicly available in case it helps anyone.

I was using stuff by [@frankpapenmeier](https://github.com/frankpapenmeier) for a long time.
Very good forks, but not updated for almost 10 years.
It crashes in the old, embedded version of the [`libyuv`](https://chromium.googlesource.com/libyuv/libyuv/) library when another app opens the camera.

So I decided to roll my own library, which does not depend on `libyuv`.
And here you go.

I stole, and implemented, a couple of interfaces from the [Sarxos webcam-capture](https://github.com/sarxos/webcam-capture) project (from which @frankpapenmeier forked) just to make this code drop-in-replaceable with my old code.

License: MIT
