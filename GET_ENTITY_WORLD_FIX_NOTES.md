# getEntityWorld API fix

Minecraft/Yarn 1.21.11 exposes ServerPlayerEntity#getEntityWorld(), not getWorld() or getServerWorld().
Replaced the remaining world access calls in ServerCustomSoundController and CrossBuilder.
