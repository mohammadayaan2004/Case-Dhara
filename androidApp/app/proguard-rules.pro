# Retrofit + Gson
-keepattributes Signature, *Annotation*
-keep class com.casedhara.data.remote.dto.** { *; }
-keep class com.casedhara.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.* class * { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
