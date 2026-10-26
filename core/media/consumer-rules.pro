# ProGuard rules for :core:media

# ML Kit Vision & Task
-keep class com.google.mlkit.vision.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# Media3 ExoPlayer & Transformer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ExifInterface
-keep class androidx.exifinterface.media.ExifInterface { *; }

# Coil 3
-keep class coil3.** { *; }
-dontwarn coil3.**

# Internal serialization data classes
-keepclassmembers class com.jian.relive.core.media.MediaAnalysisManager$AnalysisResult { *; }
-keepclassmembers class com.jian.relive.core.media.MediaAnalysisManager$PrefetchState { *; }
