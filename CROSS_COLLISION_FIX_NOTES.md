# Annoying Cross Collision Fix

Changes:
- Removed noCollision() from annoyingmod:annoying_cross.
- Collision is now a full cube, so the player bumps into it and can stand on top like a normal log/block.
- Outline selection still follows the visible cross shape.
- BlockItem registration was removed. The cross is world-only and should not be obtainable as an inventory item.
- No loot table is provided, so breaking it removes the block without dropping an item.
