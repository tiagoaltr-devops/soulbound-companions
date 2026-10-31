package br.com.soulbound.companion;

public enum BehaviorMode {
    FOLLOW_PASSIVE,
    STAY;

    public static BehaviorMode fromSavedName(String name) {
        return "STAY".equals(name) ? STAY : FOLLOW_PASSIVE;
    }
}
