#ifndef CAM_H
#define CAM_H

#ifdef __cplusplus
  extern "C" {
#endif

#define MAX_DEVICE_NAME_LENGTH 256
#define MAX_MODES 200

typedef struct CamMode {
    void *nativeFormat;
    int width;
    int height;
    unsigned pixelFormat;
} CamMode;

typedef struct CamDevice {
    void *nativeDevice;
    char name[MAX_DEVICE_NAME_LENGTH];
    CamMode *modes[MAX_MODES];
    int numModes;
    int activeModeIndex;
    void *session;
    void *input;
    void *output;
    void *transferBuffer;
} CamDevice;

CamDevice *getDefaultCamDevice();
void freeCamDevice(CamDevice *);
int openCamDevice(CamDevice *);
void closeCamDevice(CamDevice *);
int updateCamDeviceFormat(CamDevice *);

#ifdef __cplusplus
  }
#endif

#endif
