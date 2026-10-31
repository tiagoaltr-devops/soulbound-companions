package br.com.soulbound.companion.recall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bounded, server-thread-only queue. Resource cleanup belongs to the caller, never an implicit eviction. */
public final class RecallQueue<T> {
    public static final int TIMEOUT_TICKS = 200;
    public static final int MAX_PENDING = 8;
    private final Map<UUID, Entry<T>> entries = new HashMap<>();

    public record Entry<T>(T value, long deadline) {
        public boolean expired(long now) { return now >= deadline; }
    }

    public boolean offer(UUID bond, T value, long now) {
        if (entries.containsKey(bond) || entries.size() >= MAX_PENDING) return false;
        entries.put(bond, new Entry<>(value, now + TIMEOUT_TICKS));
        return true;
    }

    public boolean contains(UUID bond) { return entries.containsKey(bond); }
    public Entry<T> remove(UUID bond) { return entries.remove(bond); }
    public List<Map.Entry<UUID, Entry<T>>> snapshot() { return List.copyOf(entries.entrySet()); }
}
