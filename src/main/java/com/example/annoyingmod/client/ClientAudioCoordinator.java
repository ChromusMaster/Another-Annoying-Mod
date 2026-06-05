package com.example.annoyingmod.client;

import com.example.annoyingmod.config.ModConfig;
import net.minecraft.client.MinecraftClient;

import java.util.Random;

public final class ClientAudioCoordinator {
    private static final Random RNG = new Random();

    private long clientTick = 0L;
    private long nextVanillaSoundTick = -1L;

    public void onClientTick(MinecraftClient client) {
        clientTick++;

        if (client.player == null || client.world == null) {
            nextVanillaSoundTick = -1L;
            return;
        }

        ModConfig cfg = ModConfig.get();

        if (!cfg.soundsEnabled) {
            nextVanillaSoundTick = -1L;
            return;
        }

        if (nextVanillaSoundTick < 0L) {
            nextVanillaSoundTick = clientTick + secondsToTicks(cfg.soundIntervalMinSeconds, cfg.soundIntervalMaxSeconds);
            if (cfg.debugLogging) {
                System.out.println("[AnnoyingMod] next vanilla client sound in " + (nextVanillaSoundTick - clientTick) + " ticks");
            }
            return;
        }

        if (clientTick >= nextVanillaSoundTick) {
            boolean ok = ClientSoundPlayer.playRandomNow(RNG);
            if (cfg.debugLogging) {
                System.out.println("[AnnoyingMod] scheduled vanilla client sound: " + (ok ? "ok" : "skipped"));
            }
            nextVanillaSoundTick = clientTick + secondsToTicks(cfg.soundIntervalMinSeconds, cfg.soundIntervalMaxSeconds);
            if (cfg.debugLogging) {
                System.out.println("[AnnoyingMod] next vanilla client sound in " + (nextVanillaSoundTick - clientTick) + " ticks");
            }
        }
    }

    public void reset() {
        clientTick = 0L;
        nextVanillaSoundTick = -1L;
    }

    private static long secondsToTicks(int min, int max) {
        int realMin = Math.max(1, min);
        int realMax = Math.max(realMin, max);
        int chosen = realMin + RNG.nextInt(realMax - realMin + 1);
        return chosen * 20L;
    }
}
