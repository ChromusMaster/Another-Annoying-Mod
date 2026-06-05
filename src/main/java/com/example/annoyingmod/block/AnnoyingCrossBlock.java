package com.example.annoyingmod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class AnnoyingCrossBlock extends Block {
    private static final VoxelShape OUTLINE_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(6.0D, 0.0D, 6.0D, 10.0D, 16.0D, 10.0D),
            Block.createCuboidShape(2.0D, 9.0D, 6.0D, 14.0D, 13.0D, 10.0D)
    );

    private static final VoxelShape COLLISION_SHAPE = VoxelShapes.fullCube();

    public AnnoyingCrossBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION_SHAPE;
    }
}
