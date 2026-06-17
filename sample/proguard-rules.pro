#----------------------------- base -----------------------------
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-verbose
-dontoptimize
-dontpreverify
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

#----------------------------- default keep section -----------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers public class * extends android.view.View {
 public <init>(android.content.Context);
 public <init>(android.content.Context, android.util.AttributeSet);
 public <init>(android.content.Context, android.util.AttributeSet, int);
 public void set*(***);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context,android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context,android.util.AttributeSet,int);
}
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# keep Parcelable unobfuscated (PrintTextFormat etc.)
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-dontwarn android.support.**

# Components the framework instantiates by name.
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver

# The system recreates fragments by class name after process death — keep them.
-keep class * extends androidx.fragment.app.Fragment { <init>(); }
-keep class com.posio.printersdk.sample.** extends androidx.fragment.app.Fragment

# In-app camera scanner (ZXing) — keep the library + decoder intact.
-keep class com.journeyapps.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.journeyapps.**
-dontwarn com.google.zxing.**

# ============ ignore warnings, otherwise packaging may fail =============
-ignorewarnings
