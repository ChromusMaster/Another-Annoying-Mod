package com.example.annoyingmod.client;

import com.example.annoyingmod.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Random;

public final class ClientSoundPlayer {
    private static final Random FALLBACK_RNG = new Random();

    private static final List<SoundEvent> SOUNDS = List.of(
            SoundEvents.ENTITY_COW_DEATH,
            SoundEvents.ENTITY_ZOMBIE_AMBIENT,
            SoundEvents.ENTITY_SKELETON_AMBIENT,
            SoundEvents.ENTITY_DROWNED_AMBIENT,
            SoundEvents.ENTITY_ARROW_SHOOT,
            SoundEvents.BLOCK_CHEST_OPEN,
            SoundEvents.BLOCK_CHEST_CLOSE,
            SoundEvents.BLOCK_DISPENSER_DISPENSE,
            SoundEvents.ENTITY_ENDERMAN_SCREAM,
            SoundEvents.ENTITY_GENERIC_HURT,
            SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
            SoundEvents.ENTITY_GENERIC_BIG_FALL,
            SoundEvents.ENTITY_HORSE_DEATH,
            SoundEvents.ENTITY_PLAYER_DEATH,
            SoundEvents.ENTITY_PLAYER_HURT,
            SoundEvents.ENTITY_PILLAGER_AMBIENT,
            SoundEvents.ENTITY_VILLAGER_HURT,
            SoundEvents.ENTITY_WITCH_AMBIENT,
            SoundEvents.ENTITY_WITCH_CELEBRATE,
            SoundEvents.ITEM_CROSSBOW_HIT
    );

    private ClientSoundPlayer() {}

    public static boolean playRandomNow() {
        return playRandomNow(FALLBACK_RNG);
    }

    public static boolean playRandomNow(Random rng) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null) {
            if (ModConfig.get().debugLogging) {
                System.out.println("[AnnoyingMod] client sound skipped: no client player");
            }
            return false;
        }

        SoundEvent sound = SOUNDS.get(rng.nextInt(SOUNDS.size()));
        float pitch = 0.85F + rng.nextFloat() * 0.30F;

        player.playSound(sound, 1.0F, pitch);

        if (ModConfig.get().debugLogging) {
            Identifier id = Registries.SOUND_EVENT.getId(sound);
            System.out.println("[AnnoyingMod] client sound fired: " + id + ", pitch=" + pitch);
        }
        return true;
    }
}
