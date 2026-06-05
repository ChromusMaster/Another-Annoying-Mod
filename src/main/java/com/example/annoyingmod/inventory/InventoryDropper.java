package com.example.annoyingmod.inventory;

import com.example.annoyingmod.AnnoyingMod;
import com.example.annoyingmod.config.ModConfig;
import com.example.annoyingmod.sound.ServerCustomSoundController;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class InventoryDropper {
    private InventoryDropper() {}

    private enum SlotArea {
        HOTBAR,
        MAIN,
        ARMOR,
        OFFHAND,
        UNKNOWN
    }

    private record SlotRef(SlotArea area, int slot, ItemStack stack) {}

    public static boolean dropOneRandomItem(ServerPlayerEntity player, Random rng, String reason) {
        if (player == null) {
            AnnoyingMod.logWarning("drop skipped (" + reason + "): player is null");
            return false;
        }

        PlayerInventory inventory = player.getInventory();
        List<SlotRef> droppableSlots = collectDroppableSlots(inventory);

        if (droppableSlots.isEmpty()) {
            AnnoyingMod.logWarning("drop skipped (" + reason + "): no droppable items after protection rules");
            return false;
        }

        SlotRef selected = droppableSlots.get(rng.nextInt(droppableSlots.size()));
        ItemStack removed = inventory.removeStack(selected.slot, 1);

        if (removed.isEmpty()) {
            AnnoyingMod.logWarning("drop skipped (" + reason + "): removeStack returned empty for slot=" + selected.slot);
            return false;
        }

        player.dropItem(removed, false, false);
        ServerCustomSoundController.GLOBAL.onValuableItemDropped(player, removed);

        inventory.markDirty();
        inventory.updateItems();
        player.playerScreenHandler.sendContentUpdates();
        player.currentScreenHandler.sendContentUpdates();

        AnnoyingMod.log("drop fired (" + reason + "): player=" + player.getName().getString()
                + ", area=" + selected.area
                + ", slot=" + selected.slot
                + ", item=" + removed.getRegistryEntry().getIdAsString()
                + ", count=" + removed.getCount());

        return true;
    }

    private static List<SlotRef> collectDroppableSlots(PlayerInventory inventory) {
        List<SlotRef> slots = new ArrayList<>();
        ModConfig cfg = ModConfig.get();

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (stack.isEmpty()) {
                continue;
            }

            SlotArea area = classifySlot(slot);

            if (area == SlotArea.HOTBAR && !cfg.dropHotbar) continue;
            if (area == SlotArea.MAIN && !cfg.dropMainInventory) continue;
            if (area == SlotArea.ARMOR && !cfg.dropArmor) continue;
            if (area == SlotArea.OFFHAND && !cfg.dropOffhand) continue;
            if (area == SlotArea.UNKNOWN) continue;
            if (!passesItemRules(stack, cfg)) continue;

            slots.add(new SlotRef(area, slot, stack));
        }

        return slots;
    }

    /**
     * PlayerInventory indexes in current Yarn/Fabric builds:
     * 0-8   = hotbar
     * 9-35  = main inventory
     * 36-39 = armor
     * 40    = offhand
     *
     * We intentionally avoid PlayerInventory.main/armor/offHand because in the
     * tested 1.21.11 runtime those fields are private and cause IllegalAccessError.
     */
    private static SlotArea classifySlot(int slot) {
        if (slot >= 0 && slot <= 8) return SlotArea.HOTBAR;
        if (slot >= 9 && slot <= 35) return SlotArea.MAIN;
        if (slot >= 36 && slot <= 39) return SlotArea.ARMOR;
        if (slot == 40) return SlotArea.OFFHAND;
        return SlotArea.UNKNOWN;
    }

    private static boolean passesItemRules(ItemStack stack, ModConfig cfg) {
        String id = stack.getRegistryEntry().getIdAsString().toLowerCase();

        if (isAlwaysProtected(id)) {
            return false;
        }

        if (cfg.dropProtectedItems.contains(id)) {
            return false;
        }

        if (!cfg.dropTools && isTool(id)) {
            return false;
        }

        if (!cfg.dropWeapons && isWeapon(id)) {
            return false;
        }

        if (!cfg.dropArmor && isArmor(id)) {
            return false;
        }

        return true;
    }

    private static boolean isAlwaysProtected(String id) {
        return id.contains("netherite")
                || id.contains("ancient_debris")
                || id.equals("minecraft:elytra")
                || id.equals("minecraft:totem_of_undying");
    }

    private static boolean isTool(String id) {
        return id.endsWith("_pickaxe")
                || id.endsWith("_axe")
                || id.endsWith("_shovel")
                || id.endsWith("_hoe")
                || id.equals("minecraft:shears")
                || id.equals("minecraft:flint_and_steel")
                || id.equals("minecraft:fishing_rod")
                || id.equals("minecraft:brush")
                || id.equals("minecraft:carrot_on_a_stick")
                || id.equals("minecraft:warped_fungus_on_a_stick");
    }

    private static boolean isWeapon(String id) {
        return id.endsWith("_sword")
                || id.equals("minecraft:bow")
                || id.equals("minecraft:crossbow")
                || id.equals("minecraft:trident")
                || id.equals("minecraft:mace")
                || id.equals("minecraft:shield");
    }

    private static boolean isArmor(String id) {
        return id.endsWith("_helmet")
                || id.endsWith("_chestplate")
                || id.endsWith("_leggings")
                || id.endsWith("_boots")
                || id.equals("minecraft:elytra")
                || id.equals("minecraft:turtle_helmet");
    }
}
