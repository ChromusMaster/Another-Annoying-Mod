package com.example.annoyingmod.sound;

import com.example.annoyingmod.AnnoyingMod;
import com.example.annoyingmod.config.ModConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.time.Instant;
import java.util.*;

public final class ServerCustomSoundController {
    public static final ServerCustomSoundController GLOBAL = new ServerCustomSoundController();

    private static final Random RNG = new Random();

    private static final long CUSTOM_SOUND_GAP_MS = 12_000L;
    private static final int DEATHS_PER_SOUND = 4;
    private static final int NIGHTS_PER_SOUND = 5;
    private static final int DAYS_PER_MORNING_SOUND = 5;
    private static final int VALUABLE_DROPS_PER_SOUND = 50;
    private static final int NON_HOSTILE_KILLS_PER_SOUND = 30;
    private static final int ENDERMAN_AGGRO_PER_SOUND = 20;
    private static final int MISC_DAY_INTERVAL = 3;
    private static final int MISC_MAX_PER_DAY = 5;

    private long serverTick = 0L;
    private long lastCustomSoundMs = 0L;

    private long lastNightDay = -1L;
    private long lastMorningDay = -1L;
    private long scheduledMorningDay = -1L;
    private Instant morningDueAt = null;

    private long miscActiveDay = -1L;
    private long lastMiscStartDay = -999L;
    private int miscPlayedToday = 0;
    private Instant nextMiscAt = null;
    private final List<SoundEvent> miscBag = new ArrayList<>();

    private final Map<UUID, Boolean> playerWasDead = new HashMap<>();
    private final Map<UUID, Integer> deathCounters = new HashMap<>();
    private final Map<UUID, Integer> valuableDropCounters = new HashMap<>();
    private final Map<UUID, Integer> endermanAggroCounters = new HashMap<>();
    private final Map<UUID, Integer> lastPassiveKillTotal = new HashMap<>();
    private final Map<UUID, Integer> valuableInventoryTotal = new HashMap<>();
    private final Set<UUID> activeEndermenTargetingPlayer = new HashSet<>();

    public void reset() {
        serverTick = 0L;
        lastCustomSoundMs = 0L;
        lastNightDay = -1L;
        lastMorningDay = -1L;
        scheduledMorningDay = -1L;
        morningDueAt = null;
        miscActiveDay = -1L;
        lastMiscStartDay = -999L;
        miscPlayedToday = 0;
        nextMiscAt = null;
        miscBag.clear();
        playerWasDead.clear();
        deathCounters.clear();
        valuableDropCounters.clear();
        endermanAggroCounters.clear();
        lastPassiveKillTotal.clear();
        valuableInventoryTotal.clear();
        activeEndermenTargetingPlayer.clear();
    }

    public void onServerTick(MinecraftServer server) {
        serverTick++;
        ModConfig cfg = ModConfig.get();
        if (!cfg.customSoundsEnabled) {
            return;
        }
        if (server.getPlayerManager().getPlayerList().isEmpty()) {
            return;
        }

        long worldTime = server.getOverworld().getTimeOfDay();
        long day = Math.max(0L, worldTime / 24000L);
        long timeOfDay = Math.floorMod(worldTime, 24000L);

        ServerPlayerEntity player = server.getPlayerManager().getPlayerList().get(0);

        checkDeath(player);
        if (serverTick % 20L == 0L) {
            checkValuableInventoryGain(player);
            checkPassiveKillStats(player);
            checkEndermanAggro(player);
        }

        checkNightStart(player, day, timeOfDay);
        checkMorning(player, day, timeOfDay);
        checkMisc(player, day);
    }

    public boolean shouldBlockSleepAndPlay(ServerPlayerEntity player) {
        if (!ModConfig.get().customSoundsEnabled || player == null) {
            return false;
        }
        long worldTime = player.getEntityWorld().getTimeOfDay();
        long day = Math.max(0L, worldTime / 24000L);
        long timeOfDay = Math.floorMod(worldTime, 24000L);
        if (!isNightSoundDay(day) || lastNightDay == day) {
            return false;
        }
        if (timeOfDay < 12500L || timeOfDay > 23000L) {
            return false;
        }
        boolean ok = playRandom(player, ModSounds.NIGHT_SOUNDS, "night_sleep_block", true);
        if (ok) {
            lastNightDay = day;
        }
        return ok;
    }

    public void onValuableItemDropped(ServerPlayerEntity player, ItemStack stack) {
        if (!ModConfig.get().customSoundsEnabled || player == null || stack == null || stack.isEmpty() || !isValuable(stack)) {
            return;
        }
        UUID uuid = player.getUuid();
        int count = valuableDropCounters.getOrDefault(uuid, 0) + 1;
        valuableDropCounters.put(uuid, count);
        if (count % VALUABLE_DROPS_PER_SOUND == 0) {
            play(player, ModSounds.ITEM_SPONGEBOB_BOOWOMP_SOUND, "valuable_drop", false);
        }
    }

    public boolean playRandomCustomNow(ServerPlayerEntity player) {
        return playRandom(player, ModSounds.MISC_SOUNDS, "manual_custom", true);
    }

    public boolean playTestCustomNow(ServerPlayerEntity player) {
        return play(player, ModSounds.ITEM_MAGIC_FAIRY_SOUND, "manual_custom_test", true);
    }

    private void checkDeath(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean dead = player.getHealth() <= 0.0F;
        boolean wasDead = playerWasDead.getOrDefault(uuid, false);
        if (dead && !wasDead) {
            int count = deathCounters.getOrDefault(uuid, 0) + 1;
            deathCounters.put(uuid, count);
            if (count % DEATHS_PER_SOUND == 0) {
                playRandom(player, ModSounds.DEATH_SOUNDS, "death", false);
            }
        }
        playerWasDead.put(uuid, dead);
    }

    private void checkNightStart(ServerPlayerEntity player, long day, long timeOfDay) {
        if (!isNightSoundDay(day) || lastNightDay == day) {
            return;
        }
        if (timeOfDay >= 13000L && timeOfDay <= 13200L) {
            if (playRandom(player, ModSounds.NIGHT_SOUNDS, "night_start", false)) {
                lastNightDay = day;
            }
        }
    }

    private void checkMorning(ServerPlayerEntity player, long day, long timeOfDay) {
        if (day == 0L || day % DAYS_PER_MORNING_SOUND != 0L) {
            return;
        }
        if (lastMorningDay == day) {
            return;
        }
        if (scheduledMorningDay != day && timeOfDay <= 400L) {
            scheduledMorningDay = day;
            morningDueAt = Instant.now().plusSeconds(300L + RNG.nextInt(301));
            AnnoyingMod.log("morning custom sound scheduled for day=" + day);
            return;
        }
        if (scheduledMorningDay == day && morningDueAt != null && !Instant.now().isBefore(morningDueAt)) {
            if (playRandom(player, ModSounds.MORNING_SOUNDS, "morning", false)) {
                lastMorningDay = day;
                morningDueAt = null;
            }
        }
    }

    private void checkMisc(ServerPlayerEntity player, long day) {
        if (day - lastMiscStartDay < MISC_DAY_INTERVAL && miscActiveDay != day) {
            return;
        }
        if (miscActiveDay != day) {
            miscActiveDay = day;
            lastMiscStartDay = day;
            miscPlayedToday = 0;
            nextMiscAt = Instant.now().plusSeconds(randomBetween(ModConfig.get().customSoundIntervalMinSeconds, ModConfig.get().customSoundIntervalMaxSeconds));
            miscBag.clear();
            miscBag.addAll(ModSounds.MISC_SOUNDS);
            Collections.shuffle(miscBag, RNG);
        }
        if (miscPlayedToday >= MISC_MAX_PER_DAY || nextMiscAt == null || Instant.now().isBefore(nextMiscAt)) {
            return;
        }
        if (miscBag.isEmpty()) {
            miscBag.addAll(ModSounds.MISC_SOUNDS);
            Collections.shuffle(miscBag, RNG);
        }
        SoundEvent sound = miscBag.remove(0);
        if (play(player, sound, "misc", false)) {
            miscPlayedToday++;
        }
        if (miscPlayedToday < MISC_MAX_PER_DAY) {
            nextMiscAt = Instant.now().plusSeconds(randomBetween(ModConfig.get().customSoundIntervalMinSeconds, ModConfig.get().customSoundIntervalMaxSeconds));
        } else {
            nextMiscAt = null;
        }
    }

    private void checkValuableInventoryGain(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int total = countValuableItems(player);
        int previous = valuableInventoryTotal.getOrDefault(uuid, total);
        valuableInventoryTotal.put(uuid, total);

        if (total <= previous) {
            return;
        }
        String handlerName = player.currentScreenHandler == null ? "" : player.currentScreenHandler.getClass().getName().toLowerCase(Locale.ROOT);
        boolean likelyLootContainer = player.currentScreenHandler != player.playerScreenHandler
                && !handlerName.contains("craft")
                && !handlerName.contains("anvil")
                && !handlerName.contains("enchant")
                && !handlerName.contains("smith")
                && !handlerName.contains("grindstone")
                && !handlerName.contains("stonecutter")
                && !handlerName.contains("furnace");
        if (likelyLootContainer) {
            play(player, ModSounds.ITEM_MAGIC_FAIRY_SOUND, "valuable_found", false);
        }
    }

    private void checkPassiveKillStats(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        int total = passiveKillTotal(player);
        int previous = lastPassiveKillTotal.getOrDefault(uuid, total);
        lastPassiveKillTotal.put(uuid, total);
        if (total > previous && total > 0 && total % NON_HOSTILE_KILLS_PER_SOUND == 0) {
            playRandom(player, ModSounds.KILLMOB_SOUNDS, "passive_kill", false);
        }
    }

    private void checkEndermanAggro(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(32.0D);
        List<EndermanEntity> endermen = world.getEntitiesByClass(EndermanEntity.class, area, enderman -> enderman.getTarget() == player);
        Set<UUID> now = new HashSet<>();
        for (EndermanEntity enderman : endermen) {
            UUID id = enderman.getUuid();
            now.add(id);
            if (!activeEndermenTargetingPlayer.contains(id)) {
                UUID playerId = player.getUuid();
                int count = endermanAggroCounters.getOrDefault(playerId, 0) + 1;
                endermanAggroCounters.put(playerId, count);
                if (count % ENDERMAN_AGGRO_PER_SOUND == 0) {
                    playRandom(player, ModSounds.ENDERMAN_SOUNDS, "enderman_aggro", false);
                }
            }
        }
        activeEndermenTargetingPlayer.clear();
        activeEndermenTargetingPlayer.addAll(now);
    }

    private boolean playRandom(ServerPlayerEntity player, List<SoundEvent> sounds, String reason, boolean force) {
        if (sounds == null || sounds.isEmpty()) return false;
        return play(player, sounds.get(RNG.nextInt(sounds.size())), reason, force);
    }

    private boolean play(ServerPlayerEntity player, SoundEvent sound, String reason, boolean force) {
        if (player == null || sound == null) return false;
        long now = System.currentTimeMillis();
        if (!force && now - lastCustomSoundMs < CUSTOM_SOUND_GAP_MS) {
            AnnoyingMod.log("custom sound skipped by cooldown: " + reason);
            return false;
        }
        try {
            player.networkHandler.sendPacket(new PlaySoundS2CPacket(
                    Registries.SOUND_EVENT.getEntry(sound),
                    SoundCategory.MASTER,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    1.0F,
                    1.0F,
                    RNG.nextLong()
            ));
            lastCustomSoundMs = now;
            if (ModConfig.get().debugLogging) {
                Identifier id = Registries.SOUND_EVENT.getId(sound);
                AnnoyingMod.log("custom sound fired: reason=" + reason + ", sound=" + id);
            }
            return true;
        } catch (Throwable t) {
            AnnoyingMod.logError("custom sound failed: " + reason, t);
            return false;
        }
    }

    public static boolean isValuable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String id = stack.getRegistryEntry().getIdAsString().toLowerCase(Locale.ROOT);
        return id.contains("diamond")
                || id.contains("netherite")
                || id.contains("redstone")
                || id.contains("golden_apple")
                || stack.hasEnchantments();
    }

    private int countValuableItems(ServerPlayerEntity player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isValuable(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean isNightSoundDay(long day) {
        return day > 0L && day % NIGHTS_PER_SOUND == 0L;
    }

    private static int randomBetween(int min, int max) {
        int realMin = Math.max(1, min);
        int realMax = Math.max(realMin, max);
        return realMin + RNG.nextInt(realMax - realMin + 1);
    }

    private static int passiveKillTotal(ServerPlayerEntity player) {
        return stat(player, EntityType.CHICKEN)
                + stat(player, EntityType.COW)
                + stat(player, EntityType.PIG)
                + stat(player, EntityType.SHEEP)
                + stat(player, EntityType.RABBIT)
                + stat(player, EntityType.HORSE)
                + stat(player, EntityType.DONKEY)
                + stat(player, EntityType.MULE)
                + stat(player, EntityType.LLAMA)
                + stat(player, EntityType.GOAT)
                + stat(player, EntityType.CAMEL)
                + stat(player, EntityType.FOX)
                + stat(player, EntityType.WOLF)
                + stat(player, EntityType.CAT)
                + stat(player, EntityType.PARROT)
                + stat(player, EntityType.TURTLE)
                + stat(player, EntityType.SQUID)
                + stat(player, EntityType.GLOW_SQUID)
                + stat(player, EntityType.BAT)
                + stat(player, EntityType.FROG);
    }

    private static int stat(ServerPlayerEntity player, EntityType<?> type) {
        return player.getStatHandler().getStat(Stats.KILLED.getOrCreateStat(type));
    }
}
