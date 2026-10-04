# appcontrol R8 / ProGuard release rules
# 1. AttributesL Firebase Kotlin mapper needs @Metadata, crash traces, generics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# 2. kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# Companion objects of @Serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion *;
}

# serializer() on companion objects
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# serializer() on singleton objects
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# 3. Firebase Realtime DatabaseL shared model classes read by reflection
#    Both appclient and appcontrol read the same Firebase schema, so all
#    shared model classes must survive minification in this module too.
-keep class com.xeg911.appcontrol.data.model.** { *; }
-keep class com.xeg911.shared.data.model.** { *; }
-keep class com.xeg911.shared.data.model.usage.** { *; }
-keep class com.xeg911.shared.data.model.event.** { *; }
-keep class com.xeg911.shared.data.model.notification.** { *; }
-keep class com.xeg911.shared.data.model.transfer.** { *; }
-keep class com.xeg911.shared.rules.** { *; }

# 4. Google Auth Library FCM HTTP v1 service account authentication
#    com.google.auth:google-auth-library-oauth2-http has no consumer ProGuard
#    rules. ServiceAccountCredentials.fromStream() uses Jackson internally to
#    parse res/raw/service_account.json via reflection.
-keep class com.google.auth.** { *; }
-keep class com.google.api.client.** { *; }
# Jackson model classes used internally for JSON deserialization
-keepclassmembers class * extends com.google.api.client.json.GenericJson {
    <init>(...);
    <fields>;
    public <methods>;
}
-dontwarn com.google.auth.**
-dontwarn com.google.api.client.**
# Reflective JSON field access inside google-http-java-client
-dontwarn com.fasterxml.jackson.core.**

# 5. Hilt / Dagger our own entry points
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# 6. Enum safety
-keepclassmembers enum com.xeg911.shared.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.xeg911.appcontrol.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 7. Warning suppressions
-dontwarn org.graalvm.sdk.**
-dontwarn com.oracle.svm.**
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**