package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;

/** Small form-specific policies; all damage still passes through the shared hostile filter. */
public final class PlenoRules {
    public static final int ULTIMATE=1800,PREPARATION=30,CHARGE=240,QUAKE=360;
    public static final double REACH=2*1.15,HEALTH=24*1.40,SPEED=.28*.95;
    private PlenoRules() {}
    public static boolean canUnlock(SoulboundRecord r){return r!=null && r.archetypeId().equals("soulbound:lupus") && r.companionStatus()==CompanionStatus.ACTIVE && !r.resurrectionRequired()
        && r.unlockedForms().contains("JUNIOR") && !r.unlockedForms().contains("PLENO") && r.evolutionStage().equals("JUNIOR") && r.progression().level()>=15;}
    public static double factor(int action,int rank){return switch(action){case 12->1.5*(1+.04*rank);case 13->1.3*(1+.05*rank);case 14->.8*(1+.05*rank);case 16->2.5*(1+.06*rank);default->1;};}
    public static boolean chargeDistance(double distance){return distance>=4 && distance<=10;}
}
