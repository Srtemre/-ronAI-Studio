# Keep Hilt generated entry points
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keepclassmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel <init>(...); }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room entities & DAOs (used via reflection)
-keep class com.htmltoapk.studio.data.local.entity.** { *; }

# Keep JSoup (uses reflection)
-keep class org.jsoup.** { *; }

# Commons Compress
-keep class org.apache.commons.compress.** { *; }

# Keep our domain models (serialized)
-keep class com.htmltoapk.studio.data.model.** { *; }
-keepclassmembers class com.htmltoapk.studio.data.model.** { *; }
