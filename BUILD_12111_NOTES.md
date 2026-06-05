# Minecraft 1.21.11 Build Target

This package updates gradle.properties to compile against Minecraft 1.21.11 / Fabric Loader 0.19.2 / Fabric API 0.141.4+1.21.11.

Reason: custom blocks in the runtime 1.21.11 require registryKey(...) in Block/Item settings. That method does not exist in older 1.21.1 mappings, so the project must be compiled against 1.21.11 when using the custom cross block.
