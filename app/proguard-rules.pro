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
# ── Gson / JSON serialization ─────────────────────────────────────────────────
# Gson uses reflection to read/write field names. ProGuard renames them by
# default in release builds, causing "non-null is null" crashes on import.
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Keep all data classes that Gson serializes/deserializes
-keep class com.example.expensetracker.TodoItem { *; }
-keep class com.example.expensetracker.ui.backup.BackupData { *; }
-keep class com.example.expensetracker.tag.TagEntity { *; }
