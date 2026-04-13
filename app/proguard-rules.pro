# RhythmTrainer ProGuard rules

# Keep JNI native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep audio engine binding class (called from native code)
-keep class com.rhythmtrainer.audio.AudioEngineBinding { *; }

# Room - keep entities
-keep class com.rhythmtrainer.data.local.db.entity.** { *; }
