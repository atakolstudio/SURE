# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager

# Keep IR code model classes (reflection-free, but keep names for debugging)
-keep class com.atakolstudio.sure.data.ir.** { *; }
