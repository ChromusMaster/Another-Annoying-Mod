package com.example.annoyingmod.block;

import com.example.annoyingmod.AnnoyingMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block ANNOYING_CROSS = register("annoying_cross");

    private ModBlocks() {}

    private static Block register(String name) {
        Identifier id = Identifier.of(AnnoyingMod.MOD_ID, name);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);

        Block block = new AnnoyingCrossBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
                .registryKey(blockKey)
                .nonOpaque()
                .sounds(BlockSoundGroup.WOOD)
                .strength(0.8F));

        Registry.register(Registries.BLOCK, id, block);

        return block;
    }

    public static void initialize() {
        // static init hook
    }
}
