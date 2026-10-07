# Keep app components and Android entry points
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.accessibilityservice.AccessibilityService

# Keep Ez-touch custom classes
-keep class com.assistivetouch.custom.** { *; }