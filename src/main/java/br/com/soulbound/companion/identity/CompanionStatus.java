package br.com.soulbound.companion.identity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum CompanionStatus {
    READY, SUMMONING, ACTIVE, DEAD, MISSING;

    public static final Codec<CompanionStatus> CODEC = Codec.STRING.comapFlatMap(name -> {
        try { return DataResult.success(valueOf(name)); }
        catch (IllegalArgumentException exception) { return DataResult.error(() -> "Unknown companion status: " + name); }
    }, CompanionStatus::name);
}
