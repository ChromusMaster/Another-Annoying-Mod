# Annoying Cross OAK_LOG Axis Crash Fix

Crash fixed:
- Cause: AbstractBlock.Settings.copy(Blocks.OAK_LOG)
- Problem: OAK_LOG has axis-dependent behavior/properties.
- AnnoyingCrossBlock does not define the axis property.
- Result: Minecraft crashed at startup with:
  Cannot get property axis as it does not exist in Block{[unregistered]}

Fix:
- Changed block settings base to Blocks.OAK_PLANKS.
- OAK_PLANKS has wood-like behavior without the axis property.
- Collision remains full cube.
- Outline remains cross-shaped.
- Block remains world-only and drops no item.
