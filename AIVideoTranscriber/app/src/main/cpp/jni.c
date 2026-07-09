// Thin JNI bridge between com.aivideotranscriber.whisper.WhisperNative and whisper.cpp
// (https://github.com/ggerganov/whisper.cpp, MIT licensed). Adapted from whisper.cpp's own
// examples/whisper.android.java/app/src/main/jni/whisper/jni.c, extended with a language
// parameter and a progress callback so the UI can show live percentage during transcription.

#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include "whisper.h"

#define UNUSED(x) (void)(x)
#define TAG "AIVideoTranscriber-JNI"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

struct progress_user_data {
    JNIEnv *env;
    jobject listener; // implements com.aivideotranscriber.whisper.WhisperProgressListener
    jmethodID on_progress_mid;
};

static void progress_trampoline(struct whisper_context *ctx, struct whisper_state *state, int progress, void *user_data) {
    UNUSED(ctx);
    UNUSED(state);
    struct progress_user_data *pud = (struct progress_user_data *) user_data;
    if (pud == NULL || pud->listener == NULL) {
        return;
    }
    (*pud->env)->CallVoidMethod(pud->env, pud->listener, pud->on_progress_mid, (jint) progress);
}

JNIEXPORT jlong JNICALL
Java_com_aivideotranscriber_whisper_WhisperNative_initContext(
        JNIEnv *env, jobject thiz, jstring model_path_str) {
    UNUSED(thiz);

    const char *model_path_chars = (*env)->GetStringUTFChars(env, model_path_str, NULL);

    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;

    struct whisper_context *context = whisper_init_from_file_with_params(model_path_chars, cparams);

    (*env)->ReleaseStringUTFChars(env, model_path_str, model_path_chars);
    return (jlong) context;
}

JNIEXPORT void JNICALL
Java_com_aivideotranscriber_whisper_WhisperNative_freeContext(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    UNUSED(env);
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    if (context != NULL) {
        whisper_free(context);
    }
}

JNIEXPORT void JNICALL
Java_com_aivideotranscriber_whisper_WhisperNative_fullTranscribe(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint num_threads,
        jfloatArray audio_data, jstring language_str, jobject listener) {
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    if (context == NULL) {
        LOGW("fullTranscribe called with a null context");
        return;
    }

    jfloat *audio_data_arr = (*env)->GetFloatArrayElements(env, audio_data, NULL);
    const jsize audio_data_length = (*env)->GetArrayLength(env, audio_data);
    const char *language_chars = language_str != NULL
            ? (*env)->GetStringUTFChars(env, language_str, NULL)
            : "auto";

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = language_chars; // "auto", or a whisper language code such as "en", "hi", "gu"
    params.n_threads = num_threads;
    params.offset_ms = 0;
    params.no_context = true;
    params.single_segment = false;

    struct progress_user_data pud;
    pud.env = env;
    pud.listener = listener;
    pud.on_progress_mid = NULL;
    if (listener != NULL) {
        jclass listener_cls = (*env)->GetObjectClass(env, listener);
        pud.on_progress_mid = (*env)->GetMethodID(env, listener_cls, "onProgress", "(I)V");
        (*env)->DeleteLocalRef(env, listener_cls);
    }
    params.progress_callback = progress_trampoline;
    params.progress_callback_user_data = &pud;

    whisper_reset_timings(context);

    LOGI("Starting whisper_full: %d samples, %d threads, language=%s", audio_data_length, num_threads, language_chars);
    if (whisper_full(context, params, audio_data_arr, audio_data_length) != 0) {
        LOGW("whisper_full failed");
    } else {
        whisper_print_timings(context);
    }

    (*env)->ReleaseFloatArrayElements(env, audio_data, audio_data_arr, JNI_ABORT);
    if (language_str != NULL) {
        (*env)->ReleaseStringUTFChars(env, language_str, language_chars);
    }
}

JNIEXPORT jint JNICALL
Java_com_aivideotranscriber_whisper_WhisperNative_getTextSegmentCount(
        JNIEnv *env, jobject thiz, jlong context_ptr) {
    UNUSED(env);
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    return whisper_full_n_segments(context);
}

JNIEXPORT jstring JNICALL
Java_com_aivideotranscriber_whisper_WhisperNative_getTextSegment(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    const char *text = whisper_full_get_segment_text(context, index);
    return (*env)->NewStringUTF(env, text);
}

JNIEXPORT jlong JNICALL
Java_com_aivideotranscriber_whisper_WhisperNative_getTextSegmentT0(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    UNUSED(env);
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    return (jlong) whisper_full_get_segment_t0(context, index);
}

JNIEXPORT jlong JNICALL
Java_com_aivideotranscriber_whisper_WhisperNative_getTextSegmentT1(
        JNIEnv *env, jobject thiz, jlong context_ptr, jint index) {
    UNUSED(env);
    UNUSED(thiz);
    struct whisper_context *context = (struct whisper_context *) context_ptr;
    return (jlong) whisper_full_get_segment_t1(context, index);
}

JNIEXPORT jstring JNICALL
Java_com_aivideotranscriber_whisper_WhisperNative_getSystemInfo(
        JNIEnv *env, jobject thiz) {
    UNUSED(thiz);
    const char *sysinfo = whisper_print_system_info();
    return (*env)->NewStringUTF(env, sysinfo);
}
