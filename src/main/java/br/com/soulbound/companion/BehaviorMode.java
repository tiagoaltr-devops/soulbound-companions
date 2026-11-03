package br.com.soulbound.companion;

public enum BehaviorMode {
    FOLLOW_PASSIVE,
    STAY,
    FOLLOW_COMBAT;

    public static BehaviorMode fromSavedName(String name) {
        return "STAY".equals(name) ? STAY : "FOLLOW_COMBAT".equals(name)?FOLLOW_COMBAT:FOLLOW_PASSIVE;
    }
}
