#include <jni.h>
#include <string>
#include <android/log.h>
#include "oxorany.h"

static char ZENINOP[64] = {0};
static std::string exdate = oxorany("NULL");

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ryzen_MAct_exdate(JNIEnv *env, jclass clazz) {
    if (exdate == "NULL" || exdate.empty()) {
        return env->NewStringUTF("2026-10-04 23:59:59");
    }
    return env->NewStringUTF(exdate.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ryzen_MAct_ZENINOP(JNIEnv *env, jobject activityObject) {
    return env->NewStringUTF(ZENINOP);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ryzen_MAct_apkcrc(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ryzen_LogAct_GetKey(JNIEnv *env, jobject thiz) {
    return env->NewStringUTF(""); // Clean default, managed via Supabase / Vercel server
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ryzen_utils_Downtwo_Version(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ryzen_utils_Downtwo_Link(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF("");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ryzen_BoxApplication_getSdkKey(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(oxorany("BCORE-SDK-ANOY"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_ryzen_LogAct_Check(JNIEnv *env, jclass clazz, jobject mContext, jstring mUserKey) {
    // Legacy third-party auth endpoint (ryzencheat) completely removed.
    // Authentication is fully managed via Supabase / Vercel API.
    return env->NewStringUTF(oxorany("OK"));
}