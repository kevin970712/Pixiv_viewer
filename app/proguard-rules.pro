
-keepclassmembers public class com.android.pixivviewer.network.** {
    <init>(...);
    <fields>;
}

# 2. 保留 @SerializedName 註解 (作為輔助)
# -------------------------------------------------------------------
# 確保被 @SerializedName 標註的欄位名在混淆後保持不變。
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}


# 3. 處理 AndroidX 和 Jetpack Compose (保持不變)
# -------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep public class * implements androidx.compose.runtime.Composer
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}


# 4. 處理第三方函式庫 (保持不變，主要靠 -dontwarn)
# -------------------------------------------------------------------
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn coil.**
-dontwarn me.saket.telephoto.**
-dontwarn kotlinx.coroutines.**


# 5. 保留 Parcelable 介面 (保持不變)
# -------------------------------------------------------------------
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response
 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
# ===================================================================
#                           規則結束
# ===================================================================