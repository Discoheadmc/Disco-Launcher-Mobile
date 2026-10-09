# Keep kotlinx.serialization generated serializers.
-keepclassmembers class * implements kotlinx.serialization.KSerializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep,includedescriptorclasses class com.discocock.launcher.**$$serializer { *; }

# OkHttp / Conscrypt
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
