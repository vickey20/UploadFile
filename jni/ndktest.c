#include <string.h>
#include <jni.h>

jstring Java_com_example_uploadfile_UploadNew_invokeNativeFunction(JNIEnv* env, jobject javaThis) {
  return (*env)->NewStringUTF(env, "Hello from native code!");
}
