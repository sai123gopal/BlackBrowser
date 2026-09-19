# BlackBrowser R8 Whole-Program Optimization Rules

# Multi-pass optimization
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Strip non-critical debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Keep JavaScript Interface methods called from WebView
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebKit classes and models
-keep class androidx.webkit.** { *; }

# Keep data models used in database / records
-keep class com.saigopal.browser.database.Record { *; }
-keep class com.saigopal.browser.data.model.** { *; }

# Coil image loader
-keep class coil.** { *; }

# Google Play Services Ads & UMP
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }

# Preserve native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
