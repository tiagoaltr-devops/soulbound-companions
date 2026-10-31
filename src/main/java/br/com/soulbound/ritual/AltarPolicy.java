package br.com.soulbound.ritual;

/** Pure server policies shared by trials, forge confirmations and automated checks. */
public final class AltarPolicy {
    private AltarPolicy() {}
    public static boolean trialCoolingDown(long end,long now,int configuredTicks){return configuredTicks>0 && end>now;}
    public record ForgeResult(String state,int attempts){}
    public static ForgeResult resolveForge(boolean first,int previousAttempts,double failureRoll,double criticalRoll,double failureChance,double criticalChance){
        if(first)return new ForgeResult("ACTIVE",previousAttempts);
        return new ForgeResult(failureRoll>=failureChance?"ACTIVE":criticalRoll<criticalChance?"DESTROYED":"INACTIVE",previousAttempts+1);
    }
    @FunctionalInterface public interface Layout { boolean matches(int x,int z,int component); }
    public static int rotation(Layout layout){
        int[][] offsets={{0,-2,0},{-2,0,0},{2,0,0},{0,2,1},{0,-1,2},{-1,0,2},{1,0,2},{0,1,2}};
        for(int r=0;r<4;r++){boolean ok=true;for(var v:offsets){var p=rotate(v[0],v[1],r);if(!layout.matches(p[0],p[1],v[2])){ok=false;break;}}if(ok)return r;}
        return -1;
    }
    public static int symbols(int completions) { return Math.min(7, 3 + Math.max(0, completions)); }
    public static int guardians(int completions) { return Math.min(12, 2 + Math.max(0, completions)); }
    public static int bondTicks(int completions) { return 100 + Math.min(10, Math.max(0, completions)) * 40; }
    public static double failure(int previousAttempts) {
        return switch (previousAttempts) { case 0 -> .01; case 1 -> .20; case 2 -> .40; case 3 -> .60; default -> .80; };
    }
    public static int xp(String kind) {
        return switch (kind) { case "awakening" -> 30; case "ascension" -> 45; case "divine" -> 60; case "restoration" -> 25; default -> throw new IllegalArgumentException(kind); };
    }
    public static boolean accepts(String kind, String stage, String status, boolean unlocked) {
        return switch (kind) {
            case "awakening" -> status.equals("ACTIVE") && stage.equals("BABY");
            case "ascension" -> status.equals("ACTIVE") && stage.equals("JUNIOR");
            case "divine" -> status.equals("ACTIVE") && stage.equals("PLENO") && !unlocked;
            case "restoration" -> status.equals("DEAD");
            default -> false;
        };
    }
    public static int[] rotate(int x, int z, int rotation) {
        return switch (Math.floorMod(rotation, 4)) { case 0 -> new int[]{x,z}; case 1 -> new int[]{-z,x}; case 2 -> new int[]{-x,-z}; default -> new int[]{z,-x}; };
    }
}
