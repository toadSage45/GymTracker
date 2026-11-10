# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class com.example.gymtracker.data.db.** { *; }
