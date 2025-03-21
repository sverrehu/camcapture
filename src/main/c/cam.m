#include <stdlib.h>
#include <string.h>
#include <AVFoundation.h>

#include "cam.h"

#define FourCC2Str(code) (char[5]){(code >> 24) & 0xFF, (code >> 16) & 0xFF, (code >> 8) & 0xFF, code & 0xFF, 0}

@interface CaptureDelegate : NSObject<AVCaptureVideoDataOutputSampleBufferDelegate>
- (id) initWithCallback: (void (*)(CMSampleBufferRef, CamDevice *)) callback camDevice: (CamDevice *) theCamDevice;
- (void) captureOutput: (AVCaptureOutput*) captureOutput didOutputSampleBuffer: (CMSampleBufferRef) sampleBuffer fromConnection: (AVCaptureConnection*) connection;
@end

static void formatChanged(CamDevice *camDevice, AVCaptureDeviceFormat *newFormat) {
    for (int q = 0; q < camDevice->numModes; q++) {
        if ([((AVCaptureDeviceFormat *) camDevice->modes[q]->nativeFormat) isEqual:newFormat]) {
            camDevice->activeModeIndex = q;
            break;
        }
    }
}

static void sampleBufferCallback(CMSampleBufferRef sampleBuffer, CamDevice *camDevice) {
    AVCaptureDevice *device = camDevice->nativeDevice;
    AVCaptureDeviceFormat *activeFormat = [device activeFormat];
    CMFormatDescriptionRef activeFormatDescription = [activeFormat formatDescription];
    CMVideoDimensions activeDimensions = CMVideoFormatDescriptionGetDimensions(activeFormatDescription);
    CamMode *camMode = camDevice->modes[camDevice->activeModeIndex];
    if (activeDimensions.width != camMode->width || activeDimensions.height != camMode->height) {
        formatChanged(camDevice, activeFormat);
        updateCamDeviceFormat(camDevice);
        return;
    }
    CMFormatDescriptionRef desc = CMSampleBufferGetFormatDescription(sampleBuffer);
    CMVideoDimensions dims = CMVideoFormatDescriptionGetDimensions(desc);
    CVImageBufferRef cameraFrame = CMSampleBufferGetImageBuffer(sampleBuffer);
#if 0
    CMPixelFormatType pixelFormat = CMFormatDescriptionGetMediaSubType(desc);
    CMPixelFormatType activePixelFormat = CMFormatDescriptionGetMediaSubType(activeFormatDescription);
    printf("cam %dx%d, format: %s (camMode format: %s); out %dx%d, format: %s\n",
           activeDimensions.width, activeDimensions.height, FourCC2Str(activePixelFormat), FourCC2Str(camMode->pixelFormat),
           dims.width, dims.height, FourCC2Str(pixelFormat));
    fflush(stdout);
#endif
    CVPixelBufferLockBaseAddress(cameraFrame, kCVPixelBufferLock_ReadOnly);
    memcpy(camDevice->transferBuffer, CVPixelBufferGetBaseAddress(cameraFrame), 4 * dims.width * dims.height);
    CVPixelBufferUnlockBaseAddress(cameraFrame, kCVPixelBufferLock_ReadOnly);
}

static CamMode *convertMode(AVCaptureDeviceFormat *format) {
    CamMode *camMode = malloc(sizeof(CamMode));
    if (camMode == NULL) {
        return NULL;
    }
    camMode->nativeFormat = format;
    CMFormatDescriptionRef formatDescription = [format formatDescription];
    CMVideoDimensions dimensions = CMVideoFormatDescriptionGetDimensions(formatDescription);
    CMPixelFormatType pixelFormat = CMFormatDescriptionGetMediaSubType(formatDescription);
    camMode->width = dimensions.width;
    camMode->height = dimensions.height;
    camMode->pixelFormat = pixelFormat;
    return camMode;
}

static CamDevice *convertDevice(AVCaptureDevice *device) {
    CamDevice *camDevice = malloc(sizeof(CamDevice));
    if (camDevice == NULL) {
        return NULL;
    }
    camDevice->nativeDevice = device;
    strncpy(camDevice->name, [[device localizedName] cStringUsingEncoding:NSUTF8StringEncoding], sizeof(camDevice->name));
    camDevice->name[sizeof(camDevice->name) - 1] = '\0';
    AVCaptureDeviceFormat *activeFormat = [device activeFormat];
    camDevice->activeModeIndex = -1;
    camDevice->numModes = 0;
    for (AVCaptureDeviceFormat *format in [device formats]) {
        CamMode *mode = convertMode(format);
        if (mode == NULL) {
            freeCamDevice(camDevice);
            return NULL;
        }
        if ([format isEqual:activeFormat]) {
            camDevice->activeModeIndex = camDevice->numModes;
        }
        camDevice->modes[camDevice->numModes++] = mode;
        if (camDevice->numModes == MAX_MODES) {
            break;
        }
    }
    camDevice->session = NULL;
    camDevice->input = NULL;
    camDevice->output = NULL;
    camDevice->transferBuffer = NULL;
    return camDevice;
}

CamDevice *getDefaultCamDevice() {
    AVCaptureDevice* device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
    if (device == NULL) {
        return NULL;
    }
    return convertDevice(device);
}

void freeCamDevice(CamDevice *camDevice) {
    for (int q = 0; q < camDevice->numModes; q++) {
        free(camDevice->modes[q]);
    }
    free(camDevice);
}

int openCamDevice(CamDevice *camDevice) {
    AVCaptureDevice *device = camDevice->nativeDevice;
    CamMode *camMode = camDevice->modes[camDevice->activeModeIndex];
    AVCaptureDeviceFormat *format = camMode->nativeFormat;
    NSError *err = NULL;
    AVCaptureDeviceInput* input = [AVCaptureDeviceInput deviceInputWithDevice:device error:&err];
    if (input == NULL) {
        return -1;
    }
    [input retain];
    AVCaptureSession* session = [[AVCaptureSession alloc] init];
    if (session == NULL) {
        [input release];
        return -2;
    }
    [session beginConfiguration];
    if (![session canAddInput:input]) {
        [input release];
        [session release];
        return -3;
    }
    [session addInput:input];
    [device lockForConfiguration: &err];
    if (err) {
        [input release];
        [session release];
        return -4;
    }
    [device setActiveFormat: format];
    /* TODO: handle FPS */
    //[device setActiveVideoMinFrameDuration: [fps minFrameDuration]];
    [device unlockForConfiguration];

    AVCaptureVideoDataOutput *output = [[AVCaptureVideoDataOutput alloc] init];
    if (output == NULL) {
        [input release];
        [session release];
        return -5;
    }
    [output retain];
    OSType pixelFormat = kCVPixelFormatType_32BGRA;
    [output setVideoSettings: [NSDictionary dictionaryWithObjectsAndKeys:
                              [NSNumber numberWithInt:pixelFormat] /*[NSNumber numberWithInt:camMode->pixelFormat]*/, kCVPixelBufferPixelFormatTypeKey,
                              [NSNumber numberWithInteger:camMode->width], (id)kCVPixelBufferWidthKey,
                              [NSNumber numberWithInteger:camMode->height], (id)kCVPixelBufferHeightKey,
                              nil]];
    if (![session canAddOutput:output]) {
        [input release];
        [output release];
        [session release];
        return -6;
    }
    [output setAlwaysDiscardsLateVideoFrames:YES];
    dispatch_queue_t queue = dispatch_queue_create("cameraQueue", DISPATCH_QUEUE_SERIAL);
    [output setSampleBufferDelegate:[[CaptureDelegate alloc] initWithCallback:sampleBufferCallback camDevice:camDevice] queue:queue];
    dispatch_release(queue);
    [session addOutput:output];
    [session commitConfiguration];
    [session startRunning];
    camDevice->session = session;
    camDevice->input = input;
    camDevice->output = output;
    return 0;
}

void closeCamDevice(CamDevice *camDevice) {
    AVCaptureSession* session = camDevice->session;
    if (session == NULL) {
        return;
    }
    AVCaptureDeviceInput* input = camDevice->input;
    AVCaptureVideoDataOutput* output = camDevice->output;
    if (output != NULL) {
        [output setSampleBufferDelegate:NULL queue:dispatch_get_main_queue()];
    }
    if ([session isRunning]) {
        [session stopRunning];
    }
    if (input != NULL) {
        [session removeInput:input];
        [input release];
    }
    if (output != NULL) {
      [session removeOutput:output];
      [output release];
    }
    [session release];
    camDevice->session = NULL;
    camDevice->input = NULL;
    camDevice->output = NULL;
}

int updateCamDeviceFormat(CamDevice *camDevice) {
    AVCaptureDevice *device = camDevice->nativeDevice;
    CamMode *camMode = camDevice->modes[camDevice->activeModeIndex];
    NSError *err = NULL;
    AVCaptureSession* session = camDevice->session;
    if (session == NULL) {
        return 0;
    }
    [device lockForConfiguration: &err];
    if (err) {
        return -2;
    }
    [session beginConfiguration];
    [device setActiveFormat: camMode->nativeFormat];
    /* TODO: handle FPS */
    //[device setActiveVideoMinFrameDuration: [fps minFrameDuration]];
    AVCaptureVideoDataOutput* output = camDevice->output;
    if (output != NULL) {
        OSType pixelFormat = kCVPixelFormatType_32BGRA;
        [output setVideoSettings: [NSDictionary dictionaryWithObjectsAndKeys:
                                  [NSNumber numberWithInt:pixelFormat], kCVPixelBufferPixelFormatTypeKey,
                                  [NSNumber numberWithInteger:camMode->width], (id)kCVPixelBufferWidthKey,
                                  [NSNumber numberWithInteger:camMode->height], (id)kCVPixelBufferHeightKey,
                                  nil]];
    }
    [session commitConfiguration];
    [device unlockForConfiguration];
    AVCaptureDeviceFormat *activeFormat = [device activeFormat];
    CMFormatDescriptionRef activeFormatDescription = [activeFormat formatDescription];
    CMVideoDimensions activeDimensions = CMVideoFormatDescriptionGetDimensions(activeFormatDescription);
    CMPixelFormatType activePixelFormat = CMFormatDescriptionGetMediaSubType(activeFormatDescription);
    printf("cam %dx%d, format: %s (camMode format: %s); out %dx%d\n",
           activeDimensions.width, activeDimensions.height, FourCC2Str(activePixelFormat), FourCC2Str(camMode->pixelFormat),
           camMode->width, camMode->height);
    fflush(stdout);
    return 0;
}

@implementation CaptureDelegate {
    void (*callback)(CMSampleBufferRef, CamDevice *);
    CamDevice *camDevice;
}

- (id) initWithCallback: (void (*)(CMSampleBufferRef, CamDevice *)) theCallback camDevice: (CamDevice *) theCamDevice
{
    callback = theCallback;
    camDevice = theCamDevice;
    return self;
}

- (void) captureOutput: (AVCaptureOutput*) captureOutput
 didOutputSampleBuffer: (CMSampleBufferRef) sampleBuffer
        fromConnection: (AVCaptureConnection*) connection
{
    callback(sampleBuffer, camDevice);
}
@end
