# SQLCipher is called from native code; keep its Java side intact.
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
