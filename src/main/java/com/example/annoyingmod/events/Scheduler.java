package com.example.annoyingmod.events;

import com.example.annoyingmod.AnnoyingMod;
import com.example.annoyingmod.config.ModConfig;
import com.example.annoyingmod.inventory.InventoryDropper;
import com.example.annoyingmod.world.CrossBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public final class Scheduler {
    public static final Random RNG = new Random();

    private static final int CHAT_MESSAGE_COUNT = 48;
    private static final long ITEM_CLEANUP_INTERVAL_SECONDS = 20L * 60L;

    private long serverTick = 0L;
    private long nextChatTick = -1L;
    private long nextCrossTick = -1L;
    private long nextCrossCleanupTick = -1L;
    private Instant nextDropTime = null;
    private Instant nextItemCleanupTime = null;

    public void onServerTick(MinecraftServer server) {
        serverTick++;
        ModConfig cfg = ModConfig.get();

        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }

        if (cfg.messagesEnabled && nextChatTick < 0L) {
            nextChatTick = serverTick + minutesToTicks(cfg.chatIntervalMinMinutes, cfg.chatIntervalMaxMinutes);
            AnnoyingMod.log("next chat in " + (nextChatTick - serverTick) + " ticks");
        } else if (!cfg.messagesEnabled) {
            nextChatTick = -1L;
        }

        if (cfg.crossesEnabled && nextCrossTick < 0L) {
            nextCrossTick = serverTick + minutesToTicks(cfg.crossIntervalMinMinutes, cfg.crossIntervalMaxMinutes);
            AnnoyingMod.log("next cross in " + (nextCrossTick - serverTick) + " ticks");
        } else if (!cfg.crossesEnabled) {
            nextCrossTick = -1L;
        }

        if (cfg.inventoryDropEnabled && nextDropTime == null) {
            scheduleNextDrop(cfg);
        } else if (!cfg.inventoryDropEnabled) {
            nextDropTime = null;
        }

        if (nextCrossCleanupTick < 0L) {
            nextCrossCleanupTick = serverTick + secondsToTicks(30, 30);
        }

        if (nextItemCleanupTime == null) {
            nextItemCleanupTime = Instant.now().plusSeconds(ITEM_CLEANUP_INTERVAL_SECONDS);
            AnnoyingMod.log("next item cleanup in " + ITEM_CLEANUP_INTERVAL_SECONDS + " seconds");
        }

        if (cfg.messagesEnabled && serverTick >= nextChatTick) {
            fireChat(server);
            nextChatTick = serverTick + minutesToTicks(cfg.chatIntervalMinMinutes, cfg.chatIntervalMaxMinutes);
            AnnoyingMod.log("next chat in " + (nextChatTick - serverTick) + " ticks");
        }

        if (cfg.crossesEnabled && serverTick >= nextCrossTick) {
            ServerPlayerEntity player = getPrimaryPlayer(server);
            boolean ok = player != null && CrossBuilder.buildTwoBlocksAhead(player, RNG);
            AnnoyingMod.log("scheduled cross: " + (ok ? "ok" : "skipped"));
            nextCrossTick = serverTick + minutesToTicks(cfg.crossIntervalMinMinutes, cfg.crossIntervalMaxMinutes);
            AnnoyingMod.log("next cross in " + (nextCrossTick - serverTick) + " ticks");
        }

        if (cfg.inventoryDropEnabled && nextDropTime != null && !Instant.now().isBefore(nextDropTime)) {
            boolean ok = fireDropNow(server, "scheduled");
            AnnoyingMod.log("scheduled drop: " + (ok ? "ok" : "skipped"));
            scheduleNextDrop(cfg);
        }

        if (serverTick >= nextCrossCleanupTick) {
            CrossBuilder.cleanupExpired(server);
            nextCrossCleanupTick = serverTick + secondsToTicks(30, 30);
        }

        if (nextItemCleanupTime != null && !Instant.now().isBefore(nextItemCleanupTime)) {
            cleanupDroppedItems(server);
            nextItemCleanupTime = Instant.now().plusSeconds(ITEM_CLEANUP_INTERVAL_SECONDS);
            AnnoyingMod.log("next item cleanup in " + ITEM_CLEANUP_INTERVAL_SECONDS + " seconds");
        }
    }

    public void resetSchedule() {
        serverTick = 0L;
        nextChatTick = -1L;
        nextCrossTick = -1L;
        nextCrossCleanupTick = -1L;
        nextDropTime = null;
        nextItemCleanupTime = null;
    }

    public String status() {
        ModConfig cfg = ModConfig.get();
        long nextDropSeconds = nextDropTime == null ? -1L : Math.max(0L, Duration.between(Instant.now(), nextDropTime).toSeconds());
        long nextItemCleanupSeconds = nextItemCleanupTime == null ? -1L : Math.max(0L, Duration.between(Instant.now(), nextItemCleanupTime).toSeconds());
        long crossRecords = CrossBuilder.trackedCrossCount();

        return "[AnnoyingMod] messages=" + cfg.messagesEnabled + " " + cfg.chatIntervalMinMinutes + "-" + cfg.chatIntervalMaxMinutes + "min"
                + ", clientSounds=" + cfg.soundsEnabled + " " + cfg.soundIntervalMinSeconds + "-" + cfg.soundIntervalMaxSeconds + "s"
                + ", crosses=" + cfg.crossesEnabled + " " + cfg.crossIntervalMinMinutes + "-" + cfg.crossIntervalMaxMinutes + "min"
                + ", trackedCrosses=" + crossRecords
                + ", drop=" + cfg.inventoryDropEnabled + " " + cfg.inventoryDropIntervalMinMinutes + "-" + cfg.inventoryDropIntervalMaxMinutes + "min"
                + ", nextDropSeconds=" + nextDropSeconds
                + ", nextItemCleanupSeconds=" + nextItemCleanupSeconds
                + ", debugLogging=" + cfg.debugLogging;
    }

    public boolean fireDropNow(MinecraftServer server, String reason) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            AnnoyingMod.logAlways("WARNING drop skipped (" + reason + "): no players online");
            return false;
        }

        ServerPlayerEntity player = players.get(0);
        return InventoryDropper.dropOneRandomItem(player, RNG, reason);
    }

    private void scheduleNextDrop(ModConfig cfg) {
        int seconds = randomBetween(cfg.inventoryDropIntervalMinMinutes * 60, cfg.inventoryDropIntervalMaxMinutes * 60);
        nextDropTime = Instant.now().plusSeconds(seconds);
        AnnoyingMod.log("next drop in " + seconds + " seconds");
    }

    private static ServerPlayerEntity getPrimaryPlayer(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        return players.isEmpty() ? null : players.get(0);
    }

    private static void fireChat(MinecraftServer server) {
        int index = RNG.nextInt(CHAT_MESSAGE_COUNT) + 1;
        String key = String.format("annoyingmod.chat.%02d", index);
        server.getPlayerManager().broadcast(Text.translatable(key), false);
        AnnoyingMod.log("chat fired: " + key);
    }

    private static void cleanupDroppedItems(MinecraftServer server) {
        int removed = 0;

        try {
            for (ServerWorld world : server.getWorlds()) {
                List<? extends ItemEntity> items = world.getEntitiesByType(TypeFilter.instanceOf(ItemEntity.class), item -> true);

                for (ItemEntity item : items) {
                    item.discard();
                    removed++;
                }
            }

            AnnoyingMod.log("item cleanup executed: removed=" + removed);
        } catch (Throwable t) {
            AnnoyingMod.logError("item cleanup failed", t);
        }
    }

    private static long minutesToTicks(int min, int max) {
        return secondsToTicks(Math.max(1, min) * 60, Math.max(1, max) * 60);
    }

    private static long secondsToTicks(int min, int max) {
        return (long) randomBetween(min, max) * 20L;
    }

    private static int randomBetween(int min, int max) {
        int realMin = Math.max(1, min);
        int realMax = Math.max(realMin, max);
        return realMin + RNG.nextInt(realMax - realMin + 1);
    }
}
