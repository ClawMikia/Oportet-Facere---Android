# ReqSync ProGuard Rules

# ── Kotlin ──────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# ── Room ────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ── Navigation Component ────────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }

# ── ViewModel / LiveData ────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { *; }

# ── ViewBinding ─────────────────────────────────────────────────────────────
-keep class com.reqsync.app.databinding.** { *; }

# ── App entities & repos ────────────────────────────────────────────────────
-keep class com.reqsync.app.data.** { *; }
-keep class com.reqsync.app.utils.** { *; }

# ── Gson / serialization ────────────────────────────────────────────────────
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── WorkManager ─────────────────────────────────────────────────────────────
-keep class androidx.work.** { *; }

# ── Lottie ──────────────────────────────────────────────────────────────────
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }
