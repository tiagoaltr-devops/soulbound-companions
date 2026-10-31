package br.com.soulbound;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative exploration settings. Durations are measured in game ticks (20/s). */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue THREAT_RADIUS = BUILDER
            .comment("Threat detection radius in blocks.")
            .defineInRange("threatRadius", 12, 4, 32);
    public static final ModConfigSpec.IntValue ALERT_COOLDOWN = BUILDER
            .comment("Minimum ticks between threat alerts (100 = 5 seconds).")
            .defineInRange("alertCooldownTicks", 100, 20, 12000);
    public static final ModConfigSpec.IntValue MINERAL_RADIUS = BUILDER
            .comment("Mineral detection radius in blocks. Only already loaded chunks are inspected.")
            .defineInRange("mineralRadius", 12, 1, 32);
    public static final ModConfigSpec.IntValue MINERAL_INTERVAL = BUILDER
            .comment("Minimum ticks between scan starts. Long scans finish before another starts (200 = 10 seconds).")
            .defineInRange("mineralIntervalTicks", 200, 20, 12000);
    public static final ModConfigSpec.IntValue MINERAL_BLOCKS_PER_TICK = BUILDER
            .comment("Maximum positions examined per tick, per companion, including skipped positions.")
            .defineInRange("mineralBlocksPerTick", 128, 1, 2048);
    public static final ModConfigSpec.IntValue RITUAL_TICKS = BUILDER.defineInRange("ritualTicks",300,20,12000);
    public static final ModConfigSpec.IntValue RITUAL_VISITOR_XP = BUILDER.defineInRange("ritualVisitorLevels",5,0,100);
    public static final ModConfigSpec.IntValue AWAKENING_XP = BUILDER.defineInRange("awakeningLevels",30,0,1000);
    public static final ModConfigSpec.IntValue ASCENSION_XP = BUILDER.defineInRange("ascensionLevels",45,0,1000);
    public static final ModConfigSpec.IntValue DIVINE_XP = BUILDER.defineInRange("divineLevels",60,0,1000);
    public static final ModConfigSpec.IntValue RESTORATION_XP = BUILDER.defineInRange("restorationLevels",25,0,1000);
    public static final ModConfigSpec.IntValue TRIAL_COOLDOWN = BUILDER.comment("Trial retry delay in ticks; 0 disables old and new trial cooldowns for testing.").defineInRange("trialCooldownTicks",0,0,86400000);
    public static final ModConfigSpec.IntValue MAX_SIMULTANEOUS_GUARDIANS = BUILDER.defineInRange("maxSimultaneousGuardians",4,1,12);
    public static final ModConfigSpec.DoubleValue FORGE_FAILURE_1 = BUILDER.defineInRange("forgeFailureAttempt1",.01,0,1);
    public static final ModConfigSpec.DoubleValue FORGE_FAILURE_2 = BUILDER.defineInRange("forgeFailureAttempt2",.20,0,1);
    public static final ModConfigSpec.DoubleValue FORGE_FAILURE_3 = BUILDER.defineInRange("forgeFailureAttempt3",.40,0,1);
    public static final ModConfigSpec.DoubleValue FORGE_FAILURE_4 = BUILDER.defineInRange("forgeFailureAttempt4",.60,0,1);
    public static final ModConfigSpec.DoubleValue FORGE_FAILURE_5 = BUILDER.defineInRange("forgeFailureAttempt5Plus",.80,0,1);
    public static final ModConfigSpec.DoubleValue FORGE_CRITICAL_CHANCE = BUILDER.defineInRange("forgeCriticalGivenFailure",.25,0,1);
    public static final ModConfigSpec.IntValue RESONANCE_RADIUS=BUILDER.defineInRange("resonanceRadius",5000,1000,5000);
    public static final ModConfigSpec.IntValue RESONANCE_COOLDOWN=BUILDER.defineInRange("resonanceCooldownTicks",600,20,12000);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
