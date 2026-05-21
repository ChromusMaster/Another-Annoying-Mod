package com.example.annoyingmod.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class CrossBuilder {
    private static final BlockState CROSS_BLOCK = Blocks.ACACIA_FENCE.getDefaultState();
    private static final int ROOF_SCAN_BLOCKS = 48;

    private CrossBuilder() {}

    public static boolean buildTwoBlocksAhead(ServerPlayerEntity player) {
        if (player == null) return false;

        ServerWorld world = player.getServerWorld();
        BlockPos playerFeet = player.getBlockPos();

        BlockPos roofAbovePlayer = findAnyBlockAbove(world, playerFeet);
        if (roofAbovePlayer != null) {
            System.out.println("[AnnoyingMod] cross skipped: ceiling detected above player at " + format(roofAbovePlayer));
            return false;
        }

        Direction facing = player.getHorizontalFacing();
        BlockPos base = playerFeet.offset(facing, 2);

        if (!hasSolidGround(world, base) && hasSolidGround(world, base.down())) {
            base = base.down();
        } else if (!hasSolidGround(world, base) && hasSolidGround(world, base.up())) {
            base = base.up();
        }

        BlockPos roofAboveTarget = findAnyBlockAbove(world, base);
        if (roofAboveTarget != null) {
            System.out.println("[AnnoyingMod] cross skipped: ceiling detected above target at " + format(roofAboveTarget));
            return false;
        }

        if (!world.isInBuildLimit(base) || !world.isInBuildLimit(base.up(2))) {
            System.out.println("[AnnoyingMod] cross skipped: outside build limit");
            return false;
        }

        if (!hasSolidGround(world, base)) {
            System.out.println("[AnnoyingMod] cross skipped: no solid ground two blocks ahead");
            return false;
        }

        if (!areaClear(world, base, facing)) {
            System.out.println("[AnnoyingMod] cross skipped: target area blocked at " + format(base));
            return false;
        }

        place(world, base, facing);
        System.out.println("[AnnoyingMod] cross fired: player=" + player.getName().getString()
                + ", base=" + format(base)
                + ", armAxis=" + facing.getName());
        return true;
    }

    private static BlockPos findAnyBlockAbove(ServerWorld world, BlockPos origin) {
        int startY = origin.getY() + 1;
        int maxY = startY + ROOF_SCAN_BLOCKS;
        for (int y = startY; y <= maxY; y++) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());
            if (!world.isInBuildLimit(pos)) break;
            BlockState state = world.getBlockState(pos);
            if (!state.isAir()) return pos;
        }
        return null;
    }

    private static boolean hasSolidGround(ServerWorld world, BlockPos base) {
        BlockPos below = base.down();
        return world.getBlockState(below).isSolidBlock(world, below);
    }

    private static boolean areaClear(ServerWorld world, BlockPos base, Direction facing) {
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        return isReplaceable(world, base)
                && isReplaceable(world, base.up())
                && isReplaceable(world, base.up(2))
                && isReplaceable(world, base.up(2).offset(left))
                && isReplaceable(world, base.up(2).offset(right));
    }

    private static boolean isReplaceable(ServerWorld world, BlockPos pos) {
        return world.isInBuildLimit(pos) && world.getBlockState(pos).isReplaceable();
    }

    private static void place(ServerWorld world, BlockPos base, Direction facing) {
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        world.setBlockState(base, CROSS_BLOCK, 3);
        world.setBlockState(base.up(), CROSS_BLOCK, 3);
        world.setBlockState(base.up(2), CROSS_BLOCK, 3);
        world.setBlockState(base.up(2).offset(left), CROSS_BLOCK, 3);
        world.setBlockState(base.up(2).offset(right), CROSS_BLOCK, 3);
    }

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
