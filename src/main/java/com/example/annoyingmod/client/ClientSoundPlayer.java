package com.example.annoyingmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.List;
import java.util.Random;

public final class ClientSoundPlayer {
    private static final Random FALLBACK_RNG = new Random();

    private static final List<SoundEvent> SOUNDS = List.of(
            SoundEvents.ENTITY_CREEPER_HURT,
            SoundEvents.ENTITY_ZOMBIE_AMBIENT,
            SoundEvents.ENTITY_SKELETON_AMBIENT,
            SoundEvents.ENTITY_ARROW_SHOOT,
            SoundEvents.BLOCK_CHEST_OPEN,
            SoundEvents.BLOCK_CHEST_CLOSE,
            SoundEvents.BLOCK_DISPENSER_DISPENSE,
            SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundEvents.ENTITY_GENERIC_HURT
    );

    private ClientSoundPlayer() {}

    public static boolean playRandomNow() {
        return playRandomNow(FALLBACK_RNG);
    }

    public static boolean playRandomNow(Random rng) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            System.out.println("[AnnoyingMod] client sound skipped: no client player");
            return false;
        }
        SoundEvent sound = SOUNDS.get(rng.nextInt(SOUNDS.size()));
        float pitch = 0.85F + rng.nextFloat() * 0.30F;
        player.playSound(sound, 1.0F, pitch);
        System.out.println("[AnnoyingMod] client sound fired: " + Registries.SOUND_EVENT.getId(sound) + ", pitch=" + pitch);
        return true;
    }
}
