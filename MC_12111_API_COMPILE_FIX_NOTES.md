# Minecraft 1.21.11 API Compile Fix

This package keeps Fabric Loom 1.13.3 and Gradle 8.14.

Applied source fixes:
- ServerPlayerEntity#getServerWorld() -> ((ServerWorld) player.getWorld())
- Direction#getName() -> Direction#asString()
- World#isClient field access -> World#isClient()
- settings.gradle simplified to avoid repository blocking
- fabric_version restored to 0.141.4+1.21.11

Run:
./gradlew clean build --no-daemon --refresh-dependencies
