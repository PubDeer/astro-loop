# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep game classes
-keep class com.astroloop.game.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
