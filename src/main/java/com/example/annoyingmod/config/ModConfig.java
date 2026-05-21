package com.example.annoyingmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("annoyingmod.json");

    public List<String> chatMessages = new ArrayList<>(Arrays.asList(
            "The forest whispers tonight...",
            "Did you hear that?",
            "Something moved in the dark.",
            "You feel watched.",
            "A cold wind passes. Oh, its just Fart.",
            "A group chat is muting you silently.",
            "A mysterious stain tells no lies.",
            "A warm pizza vanishes.",
            "A wild duplicate sock appears.",
            "Did you pay the Wi-Fi bill?",
            "Monday is closer than it seems.",
            "Someone ate the last piece of cake.",
            "Something crunches under the couch.",
            "The GPS says, 'I told you so.'",
            "The TV remote is gone.",
            "The autocorrect changes the mood.",
            "The bathroom scale clears its throat.",
            "The cat remembers what you did.",
            "The fridge is judging your life choices.",
            "The printer senses your fear.",
            "The toaster refuses to pop up.",
            "You are being outsmarted by a pigeon.",
            "You feel mildly inconvenienced.",
            "You hear a spoiler in the distance.",
            "You step on a Lego. In the dark.",
            "Your alarm clock smirks at dawn.",
            "Your leftovers are plotting revenge.",
            "Your phone battery laughs softly.",
            "Your sock has escaped again."
    ));

    public int chatIntervalMinMinutes = 15;
    public int chatIntervalMaxMinutes = 20;

    public boolean soundsEnabled = true;
    public int soundIntervalMinSeconds = 120;
    public int soundIntervalMaxSeconds = 180;

    public boolean crossesEnabled = true;
    public int crossIntervalMinMinutes = 30;
    public int crossIntervalMaxMinutes = 40;
    public int crossSearchRadius = 16;

    public boolean inventoryDropEnabled = true;
    public int inventoryDropIntervalMinMinutes = 1;
    public int inventoryDropIntervalMaxMinutes = 2;

    private static ModConfig INSTANCE;

    private ModConfig() {}

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static synchronized void load() {
        try {
            if (Files.exists(PATH)) {
                String json = Files.readString(PATH);
                ModConfig cfg = GSON.fromJson(json, ModConfig.class);
                INSTANCE = cfg != null ? cfg : new ModConfig();
            } else {
                INSTANCE = new ModConfig();
            }
            INSTANCE.sanitize();
            save();
            System.out.println("[AnnoyingMod] config loaded from " + PATH);
        } catch (Exception e) {
            System.err.println("[AnnoyingMod] failed to load config, using defaults: " + e);
            INSTANCE = new ModConfig();
            INSTANCE.sanitize();
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            System.err.println("[AnnoyingMod] failed to save config: " + e);
        }
    }

    private void sanitize() {
        if (chatMessages == null) chatMessages = new ArrayList<>();
        if (chatMessages.size() > 255) chatMessages = new ArrayList<>(chatMessages.subList(0, 255));
        chatIntervalMinMinutes = Math.max(1, chatIntervalMinMinutes);
        chatIntervalMaxMinutes = Math.max(chatIntervalMinMinutes, chatIntervalMaxMinutes);
        soundIntervalMinSeconds = Math.max(1, soundIntervalMinSeconds);
        soundIntervalMaxSeconds = Math.max(soundIntervalMinSeconds, soundIntervalMaxSeconds);
        crossIntervalMinMinutes = Math.max(1, crossIntervalMinMinutes);
        crossIntervalMaxMinutes = Math.max(crossIntervalMinMinutes, crossIntervalMaxMinutes);
        crossSearchRadius = Math.max(1, Math.min(128, crossSearchRadius));
        inventoryDropIntervalMinMinutes = Math.max(1, inventoryDropIntervalMinMinutes);
        inventoryDropIntervalMaxMinutes = Math.max(inventoryDropIntervalMinMinutes, inventoryDropIntervalMaxMinutes);
    }
}
