package com.example.annoyingmod.client;

import com.example.annoyingmod.config.ModConfig;
import net.minecraft.client.MinecraftClient;

import java.util.Random;

public final class ClientSoundScheduler {
    private static final Random RNG = new Random();

    private long clientTick = 0L;
    private long nextSoundTick = -1L;

    public void onClientTick(MinecraftClient client) {
        clientTick++;

        if (client.player == null || client.world == null) {
            nextSoundTick = -1L;
            return;
        }

        ModConfig cfg = ModConfig.get();

        if (!cfg.soundsEnabled) {
            nextSoundTick = -1L;
            return;
        }

        if (nextSoundTick < 0L) {
            nextSoundTick = clientTick + secondsToTicks(cfg.soundIntervalMinSeconds, cfg.soundIntervalMaxSeconds);
            if (cfg.debugLogging) {
                System.out.println("[AnnoyingMod] next client sound in " + (nextSoundTick - clientTick) + " ticks");
            }
            return;
        }

        if (clientTick >= nextSoundTick) {
            boolean ok = ClientSoundPlayer.playRandomNow(RNG);
            if (cfg.debugLogging) {
                System.out.println("[AnnoyingMod] scheduled client sound: " + (ok ? "ok" : "skipped"));
            }
            nextSoundTick = clientTick + secondsToTicks(cfg.soundIntervalMinSeconds, cfg.soundIntervalMaxSeconds);
            if (cfg.debugLogging) {
                System.out.println("[AnnoyingMod] next client sound in " + (nextSoundTick - clientTick) + " ticks");
            }
        }
    }

    public void reset() {
        clientTick = 0L;
        nextSoundTick = -1L;
    }

    private static long secondsToTicks(int min, int max) {
        int realMin = Math.max(1, min);
        int realMax = Math.max(realMin, max);
        int chosen = realMin + RNG.nextInt(realMax - realMin + 1);
        return chosen * 20L;
    }
}
