# LibSu rules
-keep class com.topjohnwu.superuser.** { *; }
-keepclassmembers class * extends com.topjohnwu.superuser.ipc.RootService {
    public <init>(...);
}
