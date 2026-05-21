package com.example.annoyingmod;

import com.example.annoyingmod.client.ClientSoundPlayer;
import com.example.annoyingmod.client.ClientSoundScheduler;
import com.example.annoyingmod.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.text.Text;

public final class AnnoyingModClient implements ClientModInitializer {
    private final ClientSoundScheduler soundScheduler = new ClientSoundScheduler();

    @Override
    public void onInitializeClient() {
        System.out.println("[AnnoyingMod] initializing client side");
        ModConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                soundScheduler.onClientTick(client);
            } catch (Throwable t) {
                System.err.println("[AnnoyingMod] client sound tick error: " + t.getClass().getName() + ": " + t.getMessage());
                t.printStackTrace();
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("annoyingmodclient")
                        .then(ClientCommandManager.literal("reload").executes(ctx -> {
                            ModConfig.load();
                            soundScheduler.reset();
                            ctx.getSource().sendFeedback(Text.literal("[AnnoyingMod] client config reloaded"));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("status").executes(ctx -> {
                            ctx.getSource().sendFeedback(Text.literal(soundScheduler.statusText()));
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("test")
                                .then(ClientCommandManager.literal("sound").executes(ctx -> {
                                    boolean ok = ClientSoundPlayer.playRandomNow();
                                    ctx.getSource().sendFeedback(Text.literal("[AnnoyingMod] test client sound: " + (ok ? "ok" : "failed")));
                                    return ok ? 1 : 0;
                                })))
                )
        );
    }
}
