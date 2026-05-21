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
            System.out.println("[AnnoyingMod] server started; schedule reset");
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                scheduler.onServerTick(server);
            } catch (Throwable t) {
                System.err.println("[AnnoyingMod] server tick error: " + t.getClass().getName() + ": " + t.getMessage());
                t.printStackTrace();
            }
        });

        registerServerCommands();
    }

    private void registerServerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("annoyingmod")
                        .then(CommandManager.literal("reload").executes(ctx -> {
                            ModConfig.load();
                            scheduler.resetSchedule();
                            ctx.getSource().sendFeedback(() -> Text.literal("[AnnoyingMod] config reloaded"), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("status").executes(ctx -> {
                            ctx.getSource().sendFeedback(() -> Text.literal(scheduler.statusText()), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("test")
                                .then(CommandManager.literal("drop").executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    boolean ok = InventoryDropper.dropOneRandomItem(player, Scheduler.RNG, "manual-target");
                                    ctx.getSource().sendFeedback(() -> Text.literal("[AnnoyingMod] test drop: " + (ok ? "ok" : "failed")), false);
                                    return ok ? 1 : 0;
                                }))
                                .then(CommandManager.literal("drop_auto").executes(ctx -> {
                                    boolean ok = Scheduler.fireDropNow(ctx.getSource().getServer(), "manual-auto");
                                    ctx.getSource().sendFeedback(() -> Text.literal("[AnnoyingMod] test drop_auto: " + (ok ? "ok" : "failed")), false);
                                    return ok ? 1 : 0;
                                }))
                                .then(CommandManager.literal("cross").executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    boolean ok = CrossBuilder.buildTwoBlocksAhead(player);
                                    ctx.getSource().sendFeedback(() -> Text.literal("[AnnoyingMod] test cross: " + (ok ? "ok" : "skipped")), false);
                                    return ok ? 1 : 0;
                                }))
                                .then(CommandManager.literal("sound").executes(ctx -> {
                                    ctx.getSource().sendFeedback(() -> Text.literal("[AnnoyingMod] sound is client-side. Use /annoyingmodclient test sound"), false);
                                    return 1;
                                }))
                        )
                )
        );
    }
}
