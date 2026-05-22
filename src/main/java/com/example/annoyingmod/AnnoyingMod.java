package com.example.annoyingmod;

import com.example.annoyingmod.config.ModConfig;
import com.example.annoyingmod.events.Scheduler;
import com.example.annoyingmod.inventory.InventoryDropper;
import com.example.annoyingmod.world.CrossBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class AnnoyingMod implements ModInitializer {
    public static final String MOD_ID = "annoyingmod";

    private final Scheduler scheduler = new Scheduler();

    @Override
    public void onInitialize() {
        System.out.println("[AnnoyingMod] initializing server/common side");
        ModConfig.load();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ModConfig.load();
            scheduler.resetSchedule();
            log("server started; schedule reset");
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                scheduler.onServerTick(server);
            } catch (Throwable t) {
                logError("server tick error", t);
            }
        });

        registerServerCommands();
    }

    public static void log(String message) {
        if (ModConfig.get().debugLogging) {
            System.out.println("[AnnoyingMod] " + message);
        }
    }

    public static void logAlways(String message) {
        System.out.println("[AnnoyingMod] " + message);
    }

    public static void logWarning(String message) {
        System.out.println("[AnnoyingMod] WARNING " + message);
    }

    public static void logError(String message, Throwable t) {
        System.err.println("[AnnoyingMod] ERROR " + message + ": " + t.getClass().getName() + ": " + t.getMessage());
        if (ModConfig.get().debugLogging) {
            t.printStackTrace();
        }
    }

    private static void feedback(ServerCommandSource source, String text) {
        source.sendFeedback(() -> Text.literal(text), false);
    }

    private void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("annoyingmod")
                        .then(CommandManager.literal("reload")
                                .executes(ctx -> {
                                    try {
                                        ModConfig.load();
                                        scheduler.resetSchedule();
                                        feedback(ctx.getSource(), "OK");
                                        return 1;
                                    } catch (Throwable t) {
                                        logError("reload command failed", t);
                                        feedback(ctx.getSource(), "Error");
                                        return 0;
                                    }
                                }))
                        .then(CommandManager.literal("status")
                                .executes(ctx -> {
                                    try {
                                        feedback(ctx.getSource(), scheduler.status());
                                        return 1;
                                    } catch (Throwable t) {
                                        logError("status command failed", t);
                                        feedback(ctx.getSource(), "Error");
                                        return 0;
                                    }
                                }))
                        .then(CommandManager.literal("debug")
                                .executes(ctx -> {
                                    try {
                                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                        boolean dropOk = InventoryDropper.dropOneRandomItem(player, Scheduler.RNG, "debug");
                                        boolean crossOk = CrossBuilder.buildTwoBlocksAhead(player, Scheduler.RNG);
                                        logAlways("debug command executed: drop=" + dropOk + ", cross=" + crossOk);
                                        feedback(ctx.getSource(), (dropOk || crossOk) ? "OK" : "Warning");
                                        return (dropOk || crossOk) ? 1 : 0;
                                    } catch (Throwable t) {
                                        logError("debug command failed", t);
                                        feedback(ctx.getSource(), "Error");
                                        return 0;
                                    }
                                }))
                        .then(CommandManager.literal("test")
                                .then(CommandManager.literal("drop")
                                        .executes(ctx -> {
                                            try {
                                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                boolean ok = InventoryDropper.dropOneRandomItem(player, Scheduler.RNG, "manual");
                                                if (!ok) logWarning("manual drop test returned false");
                                                feedback(ctx.getSource(), ok ? "OK" : "Warning");
                                                return ok ? 1 : 0;
                                            } catch (Throwable t) {
                                                logError("manual drop test failed", t);
                                                feedback(ctx.getSource(), "Error");
                                                return 0;
                                            }
                                        }))
                                .then(CommandManager.literal("cross")
                                        .executes(ctx -> {
                                            try {
                                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                boolean ok = CrossBuilder.buildTwoBlocksAhead(player, Scheduler.RNG);
                                                if (!ok) logWarning("manual cross test returned false");
                                                feedback(ctx.getSource(), ok ? "OK" : "Warning");
                                                return ok ? 1 : 0;
                                            } catch (Throwable t) {
                                                logError("manual cross test failed", t);
                                                feedback(ctx.getSource(), "Error");
                                                return 0;
                                            }
                                        }))
                                .then(CommandManager.literal("sound")
                                        .executes(ctx -> {
                                            logWarning("server-side sound test requested; sound is client-side. Use /annoyingmodclient test sound");
                                            feedback(ctx.getSource(), "Warning");
                                            return 0;
                                        }))
                        )
                )
        );
    }
}
