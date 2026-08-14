-keepattributes Signature
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep class com.orion.app.cinema.data.** { *; }
-keep class com.orion.app.games.data.** { *; }
-keep class com.orion.app.books.data.** { *; }

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.orion.app.**$$serializer { *; }
-keepclassmembers class com.orion.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.orion.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
