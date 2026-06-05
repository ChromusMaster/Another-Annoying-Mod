# Build upgrade for Minecraft 1.21.11

This package upgrades the project build from Fabric Loom 1.7.4 to Fabric Loom 1.11 and changes the Gradle wrapper to 8.14.

Reason: the custom block registry-key API required by Minecraft 1.21.11 is not available when compiling through the older 1.21.1/Loom 1.7.x setup.

Run:

```bash
./gradlew --version
./gradlew clean build --no-daemon
```

The first run may download Gradle 8.14 and newer dependencies.
