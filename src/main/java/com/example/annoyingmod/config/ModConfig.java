package com.example.annoyingmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("annoyingmod.json");

    private static final Set<String> ALLOWED_CROSS_BLOCKS = Set.of(
            "minecraft:acacia_fence",
            "minecraft:birch_fence",
            "minecraft:oak_fence",
            "minecraft:dark_oak_fence"
    );

    private static final Set<String> DEFAULT_PROTECTED_ITEMS = Set.of(
            "minecraft:ancient_debris",
            "minecraft:netherite_scrap",
            "minecraft:netherite_ingot",
            "minecraft:netherite_block",
            "minecraft:netherite_upgrade_smithing_template",
            "minecraft:netherite_sword",
            "minecraft:netherite_pickaxe",
            "minecraft:netherite_axe",
            "minecraft:netherite_shovel",
            "minecraft:netherite_hoe",
            "minecraft:netherite_helmet",
            "minecraft:netherite_chestplate",
            "minecraft:netherite_leggings",
            "minecraft:netherite_boots",
            "minecraft:elytra",
            "minecraft:totem_of_undying"
    );

    public boolean messagesEnabled = true;
    public int chatIntervalMinMinutes = 10;
    public int chatIntervalMaxMinutes = 15;

    public boolean soundsEnabled = true;
    public int soundIntervalMinSeconds = 300;
    public int soundIntervalMaxSeconds = 600;

    public boolean crossesEnabled = true;
    public int crossIntervalMinMinutes = 20;
    public int crossIntervalMaxMinutes = 30;
    public int crossSearchRadius = 16; // kept only for backward JSON compatibility. Current placement ignores radius.
    public List<String> crossBlocks = new ArrayList<>(Arrays.asList(
            "minecraft:acacia_fence",
            "minecraft:birch_fence",
            "minecraft:oak_fence",
            "minecraft:dark_oak_fence"
    ));

    public boolean inventoryDropEnabled = true;
    public int inventoryDropIntervalMinMinutes = 40;
    public int inventoryDropIntervalMaxMinutes = 60;

    public List<String> dropProtectedItems = new ArrayList<>(Arrays.asList(
            "minecraft:ancient_debris",
            "minecraft:netherite_scrap",
            "minecraft:netherite_ingot",
            "minecraft:netherite_block",
            "minecraft:netherite_upgrade_smithing_template",
            "minecraft:netherite_sword",
            "minecraft:netherite_pickaxe",
            "minecraft:netherite_axe",
            "minecraft:netherite_shovel",
            "minecraft:netherite_hoe",
            "minecraft:netherite_helmet",
            "minecraft:netherite_chestplate",
            "minecraft:netherite_leggings",
            "minecraft:netherite_boots",
            "minecraft:elytra",
            "minecraft:totem_of_undying"
    ));

    public boolean dropArmor = true;
    public boolean dropTools = true;
    public boolean dropWeapons = true;
    public boolean dropHotbar = true;
    public boolean dropMainInventory = true;
    public boolean dropOffhand = true;

    public boolean debugLogging = false;

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
        chatIntervalMinMinutes = Math.max(1, chatIntervalMinMinutes);
        chatIntervalMaxMinutes = Math.max(chatIntervalMinMinutes, chatIntervalMaxMinutes);

        soundIntervalMinSeconds = Math.max(1, soundIntervalMinSeconds);
        soundIntervalMaxSeconds = Math.max(soundIntervalMinSeconds, soundIntervalMaxSeconds);

        crossIntervalMinMinutes = Math.max(1, crossIntervalMinMinutes);
        crossIntervalMaxMinutes = Math.max(crossIntervalMinMinutes, crossIntervalMaxMinutes);
        crossSearchRadius = Math.max(1, Math.min(128, crossSearchRadius));
        sanitizeCrossBlocks();

        inventoryDropIntervalMinMinutes = Math.max(1, inventoryDropIntervalMinMinutes);
        inventoryDropIntervalMaxMinutes = Math.max(inventoryDropIntervalMinMinutes, inventoryDropIntervalMaxMinutes);
        sanitizeProtectedItems();
    }

    private void sanitizeCrossBlocks() {
        if (crossBlocks == null) {
            crossBlocks = new ArrayList<>();
        }

        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String id : crossBlocks) {
            if (id == null) continue;
            String normalized = id.trim().toLowerCase();
            if (ALLOWED_CROSS_BLOCKS.contains(normalized)) {
                sanitized.add(normalized);
            }
        }

        if (sanitized.isEmpty()) {
            sanitized.add("minecraft:acacia_fence");
        }

        crossBlocks = new ArrayList<>(sanitized);
    }

    private void sanitizeProtectedItems() {
        if (dropProtectedItems == null) {
            dropProtectedItems = new ArrayList<>();
        }

        LinkedHashSet<String> sanitized = new LinkedHashSet<>(DEFAULT_PROTECTED_ITEMS);
        for (String id : dropProtectedItems) {
            if (id == null) continue;
            String normalized = id.trim().toLowerCase();
            if (!normalized.isEmpty()) {
                sanitized.add(normalized);
            }
        }
        dropProtectedItems = new ArrayList<>(sanitized);
    }
}
