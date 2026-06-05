package com.example.annoyingmod.world;

import com.example.annoyingmod.AnnoyingMod;
import com.example.annoyingmod.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class CrossBuilder {
    private static final long CROSS_LIFETIME_MS = 20L * 60L * 1000L;
    private static final int MAX_TRACKED_CROSSES = 512;
    private static final double CLEANUP_PLAYER_DISTANCE_SQUARED = 128.0D * 128.0D;

    private static final List<CrossRecord> TRACKED_CROSSES = new ArrayList<>();

    private CrossBuilder() {}

    private record CrossRecord(RegistryKey<World> worldKey, BlockPos base, long createdAtMs) {}

    public static boolean buildTwoBlocksAhead(ServerPlayerEntity player, Random rng) {
        if (player == null) {
            return false;
        }

        ServerWorld world = player.getEntityWorld();
        BlockPos playerFeet = player.getBlockPos();

        if (hasCeilingAbove(world, playerFeet, "player")) {
            return false;
        }

        Direction facing = player.getHorizontalFacing();
        BlockPos base = playerFeet.offset(facing, 2);

        if (!hasSolidGround(world, base) && hasSolidGround(world, base.down())) {
            base = base.down();
        } else if (!hasSolidGround(world, base) && hasSolidGround(world, base.up())) {
            base = base.up();
        }

        if (!world.isInBuildLimit(base) || !world.isInBuildLimit(base.up(2))) {
            AnnoyingMod.logWarning("cross skipped: outside build limit");
            return false;
        }

        if (hasCeilingAbove(world, base, "target")) {
            return false;
        }

        if (!hasSolidGround(world, base)) {
            AnnoyingMod.logWarning("cross skipped: no solid ground two blocks ahead");
            return false;
        }

        if (!areaClear(world, base, facing)) {
            AnnoyingMod.logWarning("cross skipped: target area blocked at " + format(base));
            return false;
        }

        BlockState crossBlock = ModBlocks.ANNOYING_CROSS.getDefaultState();
        world.setBlockState(base, crossBlock, 3);
        trackCross(world, base);

        AnnoyingMod.log("cross fired: player=" + player.getName().getString()
                + ", base=" + format(base)
                + ", armAxis=" + facing.asString()
                + ", block=annoyingmod:annoying_cross");

        return true;
    }

    public static void cleanupExpired(MinecraftServer server) {
        if (TRACKED_CROSSES.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<CrossRecord> iterator = TRACKED_CROSSES.iterator();
        int removedRecords = 0;
        int removedBlocks = 0;

        while (iterator.hasNext()) {
            CrossRecord record = iterator.next();
            if (now - record.createdAtMs < CROSS_LIFETIME_MS) {
                continue;
            }

            ServerWorld world = server.getWorld(record.worldKey);
            if (world == null) {
                iterator.remove();
                removedRecords++;
                continue;
            }

            if (!hasNearbyPlayer(world, record.base)) {
                continue;
            }

            if (world.isInBuildLimit(record.base) && world.getBlockState(record.base).getBlock() == ModBlocks.ANNOYING_CROSS) {
                world.removeBlock(record.base, false);
                removedBlocks++;
            }

            iterator.remove();
            removedRecords++;
        }

        if (removedRecords > 0) {
            AnnoyingMod.log("cross cleanup: records=" + removedRecords + ", blocks=" + removedBlocks + ", remaining=" + TRACKED_CROSSES.size());
        }
    }

    public static int trackedCrossCount() {
        return TRACKED_CROSSES.size();
    }

    private static boolean hasCeilingAbove(ServerWorld world, BlockPos base, String source) {
        int startY = base.getY() + 1;
        int maxY = startY + 48;

        for (int y = startY; y <= maxY; y++) {
            BlockPos pos = new BlockPos(base.getX(), y, base.getZ());
            if (!world.isInBuildLimit(pos)) {
                break;
            }

            if (!world.getBlockState(pos).isAir()) {
                AnnoyingMod.logWarning("cross skipped: ceiling detected above " + source + " at " + format(pos));
                return true;
            }
        }

        return false;
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

    private static void trackCross(ServerWorld world, BlockPos base) {
        if (TRACKED_CROSSES.size() >= MAX_TRACKED_CROSSES) {
            TRACKED_CROSSES.remove(0);
        }
        TRACKED_CROSSES.add(new CrossRecord(world.getRegistryKey(), base.toImmutable(), System.currentTimeMillis()));
    }

    private static boolean hasNearbyPlayer(ServerWorld world, BlockPos center) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getBlockPos().getSquaredDistance(center) <= CLEANUP_PLAYER_DISTANCE_SQUARED) {
                return true;
            }
        }
        return false;
    }

    private static String format(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
