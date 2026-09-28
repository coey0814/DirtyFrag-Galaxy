# DirtyFrag Galaxy — keep JNI + exploit entry points intact.
-keep class dirtyfrag.galaxy.exploit.** { *; }
-keepclasseswithmembernames class * { native <methods>; }
