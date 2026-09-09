# Thin wrapper: build the same native library as Anoy Loader (:app).
# Including app's Android.mk makes $(call my-dir) inside that file resolve to app/jni.
include $(call my-dir)/../../../app/src/main/jni/Android.mk
