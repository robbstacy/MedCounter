# kotlinx.serialization — keep serializers for the model classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.purovida.core.**$$serializer { *; }
-keepclassmembers class com.purovida.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.purovida.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
