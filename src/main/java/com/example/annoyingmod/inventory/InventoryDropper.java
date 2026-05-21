package com.example.annoyingmod.inventory;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class InventoryDropper {
    private InventoryDropper() {}

    public static boolean isEligibleForDrop(ServerPlayerEntity player) {
        // Keep this deliberately minimal for Minecraft/Fabric 1.21.11 compatibility.
        // Avoid ServerPlayerEntity/PlayerEntity state helpers such as isAlive(), isCreative(),
        // and isSpectator() because in this environment they may resolve to removed/renamed
        // runtime methods and crash the scheduled server tick with NoSuchMethodError.
        return player != null;
    }

    public static boolean dropOneRandomItem(ServerPlayerEntity player, Random rng, String reason) {
        if (!isEligibleForDrop(player)) {
            System.out.println("[AnnoyingMod] drop skipped (" + reason + "): null player");
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        List<Integer> occupiedSlots = findOccupiedSlots(inventory);

        if (occupiedSlots.isEmpty()) {
            System.out.println("[AnnoyingMod] drop skipped (" + reason + "): inventory empty");
            return false;
        }

        int slot = occupiedSlots.get(rng.nextInt(occupiedSlots.size()));
        ItemStack removed = inventory.removeStack(slot, 1);

        if (removed.isEmpty()) {
            System.out.println("[AnnoyingMod] drop skipped (" + reason + "): removeStack returned empty for slot=" + slot);
            return false;
        }

        boolean spawned = player.dropItem(removed, false, false) != null;

        inventory.markDirty();
        inventory.updateItems();
        player.playerScreenHandler.sendContentUpdates();
        player.currentScreenHandler.sendContentUpdates();

        System.out.println("[AnnoyingMod] drop fired (" + reason + "): player=" + player.getName().getString()
                + ", slot=" + slot
                + ", item=" + removed.getRegistryEntry().getIdAsString()
                + ", count=" + removed.getCount()
                + ", spawnedEntity=" + spawned);

        return true;
    }

    private static List<Integer> findOccupiedSlots(PlayerInventory inventory) {
        List<Integer> occupiedSlots = new ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty()) {
                occupiedSlots.add(slot);
            }
        }
        return occupiedSlots;
    }
}