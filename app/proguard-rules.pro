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

# Preserve line number information for Play Console crash symbolication.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Ensure ViewModel constructors instantiated by ViewModelProvider are preserved.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
