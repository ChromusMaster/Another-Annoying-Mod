package com.example.annoyingmod.events;

import com.example.annoyingmod.config.ModConfig;
import com.example.annoyingmod.inventory.InventoryDropper;
import com.example.annoyingmod.world.CrossBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class Scheduler {
    public static final Random RNG = new Random();

    private long serverTick = 0L;
    private long nextChatTick = -1L;
    private long nextCrossTick = -1L;

    private Instant nextDropTime = null;
    private long lastDropCheckTick = 0L;

    public void onServerTick(MinecraftServer server) {
        serverTick++;
        ModConfig cfg = ModConfig.get();

        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            nextDropTime = null;
            return;
        }

        if (nextChatTick < 0L) {
            nextChatTick = serverTick + minutesToTicks(cfg.chatIntervalMinMinutes, cfg.chatIntervalMaxMinutes);
            logNextTicks("chat", nextChatTick - serverTick);
        }

        if (cfg.crossesEnabled && nextCrossTick < 0L) {
            nextCrossTick = serverTick + minutesToTicks(cfg.crossIntervalMinMinutes, cfg.crossIntervalMaxMinutes);
            logNextTicks("cross", nextCrossTick - serverTick);
        } else if (!cfg.crossesEnabled) {
            nextCrossTick = -1L;
        }

        if (cfg.inventoryDropEnabled && nextDropTime == null) {
            nextDropTime = getNextDropTime(cfg);
            logNextDrop();
        } else if (!cfg.inventoryDropEnabled) {
            nextDropTime = null;
        }

        if (serverTick >= nextChatTick) {
            fireChat(server);
            nextChatTick = serverTick + minutesToTicks(cfg.chatIntervalMinMinutes, cfg.chatIntervalMaxMinutes);
            logNextTicks("chat", nextChatTick - serverTick);
        }

        if (cfg.crossesEnabled && serverTick >= nextCrossTick) {
            ServerPlayerEntity player = getPrimaryPlayer(server);
            boolean ok = player != null && CrossBuilder.buildTwoBlocksAhead(player);
            System.out.println("[AnnoyingMod] scheduled cross: " + (ok ? "ok" : "skipped"));
            nextCrossTick = serverTick + minutesToTicks(cfg.crossIntervalMinMinutes, cfg.crossIntervalMaxMinutes);
            logNextTicks("cross", nextCrossTick - serverTick);
        }

        if (cfg.inventoryDropEnabled && serverTick - lastDropCheckTick >= 20L) {
            lastDropCheckTick = serverTick;
            if (nextDropTime != null && !Instant.now().isBefore(nextDropTime)) {
                boolean ok = fireDropNow(server, "scheduled");
                System.out.println("[AnnoyingMod] scheduled drop: " + (ok ? "ok" : "skipped"));
                nextDropTime = getNextDropTime(cfg);
                logNextDrop();
            }
        }
    }

    public void resetSchedule() {
        serverTick = 0L;
        nextChatTick = -1L;
        nextCrossTick = -1L;
        nextDropTime = null;
        lastDropCheckTick = 0L;
        System.out.println("[AnnoyingMod] scheduler reset");
    }

    public String statusText() {
        long nextChat = nextChatTick < 0L ? -1L : nextChatTick - serverTick;
        long nextCross = nextCrossTick < 0L ? -1L : nextCrossTick - serverTick;
        long nextDropSeconds = nextDropTime == null ? -1L : Math.max(0L, nextDropTime.getEpochSecond() - Instant.now().getEpochSecond());

        return "[AnnoyingMod] tick=" + serverTick
                + ", nextChat=" + nextChat + " ticks"
                + ", nextCross=" + nextCross + " ticks"
                + ", nextDrop=" + nextDropSeconds + " seconds"
                + ", dropMode=wall-clock";
    }

    public static boolean fireDropNow(MinecraftServer server, String reason) {
        List<ServerPlayerEntity> eligible = new ArrayList<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (InventoryDropper.isEligibleForDrop(player)) {
                eligible.add(player);
            }
        }

        if (eligible.isEmpty()) {
            System.out.println("[AnnoyingMod] drop skipped (" + reason + "): no eligible players");
            return false;
        }

        Collections.shuffle(eligible, RNG);

        for (ServerPlayerEntity player : eligible) {
            if (InventoryDropper.dropOneRandomItem(player, RNG, reason)) {
                return true;
            }
        }

        System.out.println("[AnnoyingMod] drop skipped (" + reason + "): all eligible players had no droppable inventory slots");
        return false;
    }

    private static ServerPlayerEntity getPrimaryPlayer(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        return players.isEmpty() ? null : players.get(0);
    }

    private static void fireChat(MinecraftServer server) {
        ModConfig cfg = ModConfig.get();
        List<String> messages = cfg.chatMessages;
        if (messages == null || messages.isEmpty()) {
            System.out.println("[AnnoyingMod] chat skipped: empty message list");
            return;
        }
        String msg = messages.get(RNG.nextInt(messages.size()));
        server.getPlayerManager().broadcast(Text.literal(msg), false);
        System.out.println("[AnnoyingMod] chat fired: " + msg);
    }

    private static Instant getNextDropTime(ModConfig cfg) {
        int minSeconds = Math.max(1, cfg.inventoryDropIntervalMinMinutes) * 60;
        int maxSeconds = Math.max(cfg.inventoryDropIntervalMinMinutes, cfg.inventoryDropIntervalMaxMinutes) * 60;
        int chosenSeconds = minSeconds + RNG.nextInt(maxSeconds - minSeconds + 1);
        return Instant.now().plusSeconds(chosenSeconds);
    }

    private void logNextDrop() {
        long seconds = nextDropTime == null ? -1L : Math.max(0L, nextDropTime.getEpochSecond() - Instant.now().getEpochSecond());
        System.out.println("[AnnoyingMod] next drop in " + seconds + " seconds");
    }

    private static long minutesToTicks(int min, int max) {
        return secondsToTicks(Math.max(1, min) * 60, Math.max(1, max) * 60);
    }

    private static long secondsToTicks(int min, int max) {
        int realMin = Math.max(1, min);
        int realMax = Math.max(realMin, max);
        int chosen = realMin + RNG.nextInt(realMax - realMin + 1);
        return chosen * 20L;
    }

    private static void logNextTicks(String event, long ticks) {
        System.out.println("[AnnoyingMod] next " + event + " in " + ticks + " ticks");
    }
}
