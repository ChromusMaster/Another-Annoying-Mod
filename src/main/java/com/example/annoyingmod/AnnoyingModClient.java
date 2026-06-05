package com.example.annoyingmod;

import com.example.annoyingmod.client.ClientSoundPlayer;
import com.example.annoyingmod.client.ClientAudioCoordinator;
import com.example.annoyingmod.config.ModConfig;
import com.example.annoyingmod.sound.ModSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

public final class AnnoyingModClient implements ClientModInitializer {
    private final ClientAudioCoordinator soundScheduler = new ClientAudioCoordinator();

    @Override
    public void onInitializeClient() {
        System.out.println("[AnnoyingMod] initializing client side");
        ModConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                soundScheduler.onClientTick(client);
            } catch (Throwable t) {
                AnnoyingMod.logError("client sound tick error", t);
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("annoyingmodclient")
                        .then(ClientCommandManager.literal("reload")
                                .executes(ctx -> {
                                    try {
                                        ModConfig.load();
                                        soundScheduler.reset();
                                        ctx.getSource().sendFeedback(Text.literal("OK"));
                                        return 1;
                                    } catch (Throwable t) {
                                        AnnoyingMod.logError("client reload command failed", t);
                                        ctx.getSource().sendFeedback(Text.literal("Error"));
                                        return 0;
                                    }
                                }))
                        .then(ClientCommandManager.literal("status")
                                .executes(ctx -> {
                                    try {
                                        ModConfig cfg = ModConfig.get();
                                        ctx.getSource().sendFeedback(Text.literal("[AnnoyingMod] vanillaClientSounds=" + cfg.soundsEnabled
                                                + " " + cfg.soundIntervalMinSeconds + "-" + cfg.soundIntervalMaxSeconds + "s"
                                                + ", customSounds=" + cfg.customSoundsEnabled + " " + cfg.customSoundIntervalMinSeconds + "-" + cfg.customSoundIntervalMaxSeconds + "s"
                                                + ", debugLogging=" + cfg.debugLogging));
                                        return 1;
                                    } catch (Throwable t) {
                                        AnnoyingMod.logError("client status command failed", t);
                                        ctx.getSource().sendFeedback(Text.literal("Error"));
                                        return 0;
                                    }
                                }))
                        .then(ClientCommandManager.literal("debug")
                                .executes(ctx -> {
                                    try {
                                        boolean ok = ClientSoundPlayer.playRandomNow();
                                        AnnoyingMod.logAlways("client debug command executed: sound=" + ok);
                                        ctx.getSource().sendFeedback(Text.literal(ok ? "OK" : "Warning"));
                                        return ok ? 1 : 0;
                                    } catch (Throwable t) {
                                        AnnoyingMod.logError("client debug command failed", t);
                                        ctx.getSource().sendFeedback(Text.literal("Error"));
                                        return 0;
                                    }
                                }))
                        .then(ClientCommandManager.literal("test")
                                .then(ClientCommandManager.literal("sound")
                                        .executes(ctx -> {
                                            try {
                                                boolean ok = ClientSoundPlayer.playRandomNow();
                                                if (!ok) AnnoyingMod.logWarning("client sound test returned false");
                                                ctx.getSource().sendFeedback(Text.literal(ok ? "OK" : "Warning"));
                                                return ok ? 1 : 0;
                                            } catch (Throwable t) {
                                                AnnoyingMod.logError("client sound test failed", t);
                                                ctx.getSource().sendFeedback(Text.literal("Error"));
                                                return 0;
                                            }
                                        }))
                                .then(ClientCommandManager.literal("custom_sound")
                                        .executes(ctx -> {
                                            try {
                                                if (net.minecraft.client.MinecraftClient.getInstance().player == null) {
                                                    AnnoyingMod.logWarning("client custom sound test returned false: no client player");
                                                    ctx.getSource().sendFeedback(Text.literal("Warning"));
                                                    return 0;
                                                }
                                                net.minecraft.client.MinecraftClient.getInstance().player.playSound(ModSounds.ITEM_MAGIC_FAIRY_SOUND, 1.0F, 1.0F);
                                                ctx.getSource().sendFeedback(Text.literal("OK"));
                                                return 1;
                                            } catch (Throwable t) {
                                                AnnoyingMod.logError("client custom sound test failed", t);
                                                ctx.getSource().sendFeedback(Text.literal("Error"));
                                                return 0;
                                            }
                                        })))
                )
        );
    }
}
