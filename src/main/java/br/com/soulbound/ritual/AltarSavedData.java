package br.com.soulbound.ritual;

import com.google.gson.Gson;
import com.mojang.serialization.Codec;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.storage.LevelResource;

/** Canonical global ledger. Physical items contain only references into this registry. */
public final class AltarSavedData extends SavedData {
    private static final Gson GSON = new Gson();
    public static final Codec<AltarSavedData> CODEC = Codec.STRING.xmap(AltarSavedData::decode, d -> GSON.toJson(d.state));
    public static final SavedDataType<AltarSavedData> TYPE = new SavedDataType<>("soulbound_altar", AltarSavedData::new, CODEC);
    public static final class State {
        public Map<String, Ruin> ruins = new HashMap<>();
        public Map<String, Forge> forges = new HashMap<>();
        public Map<String, Token> tokens = new HashMap<>();
        public Map<String, Progress> progress = new HashMap<>();
        public Map<String, String> slots = new HashMap<>();
        public Map<String, Ritual> rituals = new HashMap<>();
        public Set<String> slainDragons = new HashSet<>();
        public Map<String,Transfer> transfers = new HashMap<>();
    }
    public static final class Ruin {
        public String id, dimension, village;
        public int x,y,z, completions;
        public Set<String> completed = new HashSet<>();
        public Map<String, Long> cooldowns = new HashMap<>();
        public Trial trial;
        public String origin="LEGACY_UNVERIFIED", integrity="UNKNOWN";
        public int generationVersion=1;
        public long lastVerified;
    }
    public static final class Trial {
        public String player, run;
        public int phase, cursor, ticks, spawned;
        public int[] sequence;
        public long started;
        public Set<String> guards = new HashSet<>();
    }
    public static final class Forge {
        public String id, owner, dimension, state = "UNBOUND";
        public int x,y,z, attempts;
        public boolean placed;
        public String core = "";
        public Activation activation;
    }
    public static final class Activation {
        public String player,result,core="";
        public long end;
        public int rotation,attempts;
        public Map<String,String> offerings=new HashMap<>();
    }
    public static final class Token {
        public String id, kind, owner;
        public String bond;
        public boolean recoveryOnly;
        public String xpRewardKey;
        public boolean spent, delivered;
        public String applicationOwner,applicationBond;
        public boolean applicationPending;
        public boolean previousGodUnlocked;
        public long previousGodEnd;
        public int previousResurrections;
    }
    public static final class Progress {
        public boolean godUnlocked;
        public long godEnd;
        public int resurrections;
        public long resonanceCooldownEnd;
    }
    public static final class Ritual {
        public String id, forge, player, kind, bond, core;
        public Map<String,String> offerings = new HashMap<>();
        public long end;
        public int rotation;
    }
    public static final class Transfer {
        public String id,player,target,stack;
        public boolean deposit;
    }
    public State state = new State();
    private static AltarSavedData decode(String json) {
        var d = new AltarSavedData(); d.state = GSON.fromJson(json, State.class);
        if (d.state == null || d.state.ruins == null || d.state.forges == null || d.state.tokens == null || d.state.progress == null
                || d.state.slots==null || d.state.rituals==null || d.state.transfers==null || d.state.slainDragons==null)
            throw new IllegalArgumentException("Invalid altar registry");
        d.state.ruins.forEach((id,r) -> { if (!id.equals(r.id) || r.completed == null || r.cooldowns == null || r.dimension==null || r.completions<0) throw new IllegalArgumentException("Invalid ruin"); });
        d.state.forges.forEach((id,f) -> { if (!id.equals(f.id) || f.attempts<0 || f.core==null || !Set.of("UNBOUND","INACTIVE","ACTIVE","DESTROYED").contains(f.state)) throw new IllegalArgumentException("Invalid forge"); });
        d.state.forges.values().forEach(f->{var a=f.activation;if(a!=null){
            if(a.player==null || a.core==null || a.offerings==null || a.offerings.size()!=3 || a.rotation<0 || a.rotation>3 || a.attempts<0
                    || !Set.of("ACTIVE","INACTIVE","DESTROYED").contains(a.result) || f.owner==null && !"ACTIVE".equals(a.result))throw new IllegalArgumentException("Invalid activation escrow");
            UUID.fromString(a.player);
        }});
        d.state.tokens.forEach((id,t) -> { if (!id.equals(t.id) || t.owner==null || !Set.of("spark","awakening","ascension","divine","restoration","talent_reset").contains(t.kind)) throw new IllegalArgumentException("Invalid token"); });
        d.state.rituals.forEach((id,r)->{if(!id.equals(r.id) || r.offerings==null || !d.state.forges.containsKey(r.forge))throw new IllegalArgumentException("Invalid ritual escrow");});
        d.state.transfers.forEach((id,t)->{if(!id.equals(t.id) || t.player==null || t.target==null || t.stack==null)throw new IllegalArgumentException("Invalid inventory transfer");});
        return d;
    }
    public static AltarSavedData get(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Altar access outside server thread");
        var storage = server.overworld().getDataStorage();
        var data = storage.get(TYPE);
        if (data == null) {
            if (Files.exists(server.getWorldPath(LevelResource.ROOT).resolve("data/soulbound_altar.dat")))
                throw new IllegalStateException("Existing altar registry failed to load; refusing reset");
            data = storage.computeIfAbsent(TYPE);
        }
        return data;
    }
    public void flush(MinecraftServer server) {
        setDirty(); server.overworld().getDataStorage().saveAndJoin();
        try {
            var file = server.getWorldPath(LevelResource.ROOT).resolve("data/soulbound_altar.dat");
            var root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            var saved = CODEC.parse(NbtOps.INSTANCE, root.get("data")).getOrThrow();
            if (!GSON.toJsonTree(state).equals(GSON.toJsonTree(saved.state))) throw new IllegalStateException("Altar disk snapshot mismatch");
        } catch (Exception e) { setDirty(); throw new IllegalStateException("Cannot persist altar ledger",e); }
    }
    public Progress progress(UUID owner) { return state.progress.computeIfAbsent(owner.toString(), k -> new Progress()); }
}
