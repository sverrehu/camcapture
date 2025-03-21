#include <stdlib.h>

#include "no_shhsoft_camcapture_JniAdapter.h"
#include "cam.h"

#if 0
static void throwOutOfMemoryError(JNIEnv *env) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"), "");
}
#endif

static void throwError(JNIEnv *env, char *message) {
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"), message);
}

static jclass findClass(JNIEnv *env, char *className) {
    jclass c = (*env)->FindClass(env, className);
    if (c == NULL) {
        return NULL;
    }
    return c;
}

static jmethodID findConstructor(JNIEnv *env, jclass c, char *constructorParams) {
    jmethodID constructor = (*env)->GetMethodID(env, c, "<init>", constructorParams);
    if (constructor == NULL) {
        return NULL;
    }
    return constructor;
}

static CamDevice *getCamDevice(JNIEnv *env, jobject obj) {
    jfieldID fieldId = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, obj), "nativeAddress", "J");
    return (CamDevice *) (*env)->GetLongField(env, obj, fieldId);
}

/* Implementation *******************************************************/

JNIEXPORT void JNICALL Java_no_shhsoft_camcapture_JniAdapter_init(JNIEnv *env, jobject obj) {
    if (sizeof(CamDevice *) > sizeof(jlong)) {
        throwError(env, "Native pointer will not fit in a Java long. The library cannot be used on this platform.");
    }
}

JNIEXPORT jobject JNICALL Java_no_shhsoft_camcapture_JniAdapter_getDefaultDevice(JNIEnv *env, jobject obj) {
    CamDevice *camDevice = getDefaultCamDevice();
    if (camDevice == NULL) {
        throwError(env, "No camera found");
        return NULL;
    }
    jclass dimensionClass = (*env)->FindClass(env, "no/shhsoft/camcapture/CamCaptureDimension");
    if (dimensionClass == NULL) {
        return NULL;
    }
    jobjectArray dimensionArray = (*env)->NewObjectArray(env, camDevice->numModes, dimensionClass, 0);
    if (dimensionArray == NULL) {
        return NULL;
    }
    for (int q = 0; q < camDevice->numModes; q++) {
        jmethodID constructor = findConstructor(env, dimensionClass, "(JII)V");
        if (constructor == NULL) {
            return NULL;
        }
        CamMode *mode = camDevice->modes[q];
        jobject dimension = (*env)->NewObject(env, dimensionClass, constructor, (jlong) mode, mode->width, mode->height);
        if (dimension == NULL) {
            return NULL;
        }
        (*env)->SetObjectArrayElement(env, dimensionArray, q, dimension);
    }

    jclass c = findClass(env, "no/shhsoft/camcapture/CamCaptureDevice");
    if (c == NULL) {
        return NULL;
    }
    jmethodID constructor = findConstructor(env, c, "(JLjava/lang/String;[Lno/shhsoft/camcapture/CamCaptureDimension;I)V");
    if (constructor == NULL) {
        return NULL;
    }
    jobject camCaptureDevice = (*env)->NewObject(env, c, constructor, (jlong) camDevice, (*env)->NewStringUTF(env, camDevice->name), dimensionArray, camDevice->activeModeIndex);
    return camCaptureDevice;
}

JNIEXPORT void JNICALL Java_no_shhsoft_camcapture_JniAdapter_free(JNIEnv *env, jobject obj, jobject camCaptureDevice) {
    CamDevice *camDevice = getCamDevice(env, camCaptureDevice);
    freeCamDevice(camDevice);
}

JNIEXPORT void JNICALL Java_no_shhsoft_camcapture_JniAdapter_setResolutionIndex(JNIEnv *env, jobject obj, jobject camCaptureDevice, jint index) {
    CamDevice *camDevice = getCamDevice(env, camCaptureDevice);
    camDevice->activeModeIndex = index;
    if (updateCamDeviceFormat(camDevice) != 0) {
        throwError(env, "Unable to update device format");
    }
}

JNIEXPORT jint JNICALL Java_no_shhsoft_camcapture_JniAdapter_getResolutionIndex(JNIEnv *env, jobject obj, jobject camCaptureDevice) {
    CamDevice *camDevice = getCamDevice(env, camCaptureDevice);
    return (jint) camDevice->activeModeIndex;
}

JNIEXPORT void JNICALL Java_no_shhsoft_camcapture_JniAdapter_open(JNIEnv *env, jobject obj, jobject camCaptureDevice) {
    CamDevice *camDevice = getCamDevice(env, camCaptureDevice);
    int result = openCamDevice(camDevice);
    if (result != 0) {
        throwError(env, "Unable to open device");
    }
    jfieldID fieldId = (*env)->GetFieldID(env, (*env)->GetObjectClass(env, camCaptureDevice), "transferBuffer", "Ljava/nio/ByteBuffer;");
    jobject transferBuffer = (*env)->GetObjectField(env, camCaptureDevice, fieldId);
    if (transferBuffer == NULL) {
        throwError(env, "Unable to get direct access to transferBuffer");
        return;
    }
    camDevice->transferBuffer = (*env)->GetDirectBufferAddress(env, transferBuffer);
}

JNIEXPORT void JNICALL Java_no_shhsoft_camcapture_JniAdapter_close(JNIEnv *env, jobject obj, jobject camCaptureDevice) {
    CamDevice *camDevice = getCamDevice(env, camCaptureDevice);
    closeCamDevice(camDevice);
}
