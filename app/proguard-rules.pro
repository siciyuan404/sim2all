# JavaMail
-keep com.sun.mail.** { *; }
-keep javax.mail.** { *; }
-keep javax.activation.** { *; }
-keep org.apache.harmony.** { *; }
-keep com.sun.mail.**.Pop3Folder { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
-dontwarn javax.activation.**

# 保留反射使用的类
-keep class com.sim2all.smsforward.data.** { *; }
-keep class com.sim2all.smsforward.mail.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
