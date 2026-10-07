# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ---- kotlinx.serialization (modelos enviados/recibidos de Supabase) ----
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep,includedescriptorclasses class com.example.pricesapp.data.**$$serializer { *; }
-keepclassmembers class com.example.pricesapp.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.pricesapp.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Ktor / supabase-kt: clases opcionales que no existen en Android ----
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

# Mantener nombres de línea en los stack traces de Logcat
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ---- ML Kit barcode (bundled) ----
# libbarhopper_v3.so usa estas clases por nombre vía JNI y protobuf lite lee sus
# campos por reflexión. R8 en modo completo las eliminaba y el escáner se caía
# al abrir la cámara en el APK de release.
-keep class com.google.android.libraries.barhopper.** { *; }
-keep class com.google.photos.vision.barhopper.** { *; }
-keep class com.google.barhopper.deeplearning.** { *; }
# Los mensajes protobuf internos de ML Kit (mlkit_common, mlkit_vision_*) también
# se leen por reflexión: se conserva ML Kit completo (~1 MB) por seguridad.
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.mlkit.** { *; }
