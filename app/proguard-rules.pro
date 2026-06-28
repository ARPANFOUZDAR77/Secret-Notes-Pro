# Project-specific ProGuard/R8 obfuscation rules

# Keep lines and source files obfuscated but keep trace elements for crash tracking if needed
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Repackage all obfuscated classes into a single flat package to hide the original structure
-repackageclasses 'com.example.security.internal'
-allowaccessmodification

# Keep Room database components and entities intact to prevent database schema issues
-keep class com.example.data.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}
-keepclassmembers class * {
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.Entity *;
}

# Keep serialization-related classes if necessary
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep security classes and prevent certain methods from being completely stripped
-keep class com.example.security.** { *; }

