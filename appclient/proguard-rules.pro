# appclient R8 / ProGuard release rules
# 1. Attributes: keep for Firebase Kotlin mapper, crash traces, and generics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# 2. kotlinx.serialization (official rules from JetBrains)
#    https://github.com/Kotlin/kotlinx.serialization#android
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# Keep Companion objects of @Serializable classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion *;
}

# Keep serializer() on companion objects
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializer() on singleton objects
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# TransferRequest is a sealed class with discriminated-union serialization.
# Subclasses must not be removed, their @SerialName discriminators are key.
-keep class com.xeg911.appclient.transfer.model.TransferRequest { *; }
-keep class com.xeg911.appclient.transfer.model.TransferRequest$** { *; }

# 3. Firebase Realtime Database shared model classes read by reflection
#    The `shared` module is a plain Kotlin JVM library (java-library plugin)
#    so it has no consumer ProGuard rules of its own.
#    Firebase RTDB's Kotlin mapper uses @Metadata + constructor reflection.
-keep class com.xeg911.shared.data.model.** { *; }
-keep class com.xeg911.shared.data.model.usage.** { *; }
-keep class com.xeg911.shared.data.model.event.** { *; }
-keep class com.xeg911.shared.data.model.notification.** { *; }
-keep class com.xeg911.shared.data.model.transfer.** { *; }
-keep class com.xeg911.shared.rules.** { *; }

# 4. Hilt / Dagger our own entry points
#    Hilt ships consumer rules for its generated components, we only protect
#    our annotated classes that Hilt code-generates wiring for.
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
# @EntryPoint interface used by ServiceWatchdogWorker / ServiceRestartWorker
-keep @dagger.hilt.EntryPoint interface * { *; }
# Hilt @Inject constructors must not be stripped
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# 5. WorkManager workers NOT using @HiltWorker (use EntryPointAccessors)
#    WorkManager's consumer rules keep ListenableWorker subclasses by default,
#    but explicit keep ensures these two survive any aggressive R8 pass.
-keep class com.xeg911.appclient.monitoring.watchdog.ServiceWatchdogWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.xeg911.appclient.monitoring.watchdog.ServiceRestartWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# 6. WebView JavaScript interface (AppClientBridge)
#    Methods annotated with @JavascriptInterface are called by name from JS;
#    renaming them would break every page that calls AppClient.xyz().
-keepnames class com.xeg911.appclient.ui.webview.AppClientBridge
-keepclassmembers class com.xeg911.appclient.ui.webview.AppClientBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# 7. Enum safety
#    Firebase RTDB stores enum names as strings; kotlinx.serialization reads
#    enum entries by name. R8 must not rename or remove enum constants.
-keepclassmembers enum com.xeg911.shared.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers enum com.xeg911.appclient.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 8. Warning suppressions
# OkHttp3 v5 optional GraalVM native-image support
-dontwarn org.graalvm.sdk.**
-dontwarn com.oracle.svm.**
# Checker Framework annotations are compile-time only
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**