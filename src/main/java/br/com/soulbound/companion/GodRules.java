package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;

public final class GodRules {
    public static final int DURATION=1200,COOLDOWN=6000,RITUAL=900,ULTIMATE=2400,PREPARATION=40,CHARGE=200,RUPTURE=300;
    public static final double HEALTH=PlenoRules.HEALTH*2,SPEED=PlenoRules.SPEED*1.2,REACH=PlenoRules.REACH*1.35;
    private GodRules(){}
    public static boolean canUnlock(SoulboundRecord r){return r!=null && r.species().equals("LUPUS") && r.companionStatus()==CompanionStatus.ACTIVE && !r.resurrectionRequired()
        && r.unlockedForms().contains("PLENO") && !r.unlockedForms().contains("GOD") && r.progression().level()>=25;}
    public static boolean canActivate(SoulboundRecord r,long now){return r!=null && r.companionStatus()==CompanionStatus.ACTIVE && !r.resurrectionRequired()
        && r.activeForm().equals("PLENO") && r.unlockedForms().contains("GOD") && now>=r.godCooldownEnd() && now>=r.forms().switchEnd();}
    public static boolean core(SoulboundRecord r,String owner,String bond,boolean spent){return !spent && canUnlock(r) && r.ownerUuid().toString().equals(owner) && r.bondUuid().toString().equals(bond);}
    public static double factor(int action,int rank){return switch(action){case 11->1.5;case 12->2.25*(1+.05*rank);case 13->2*(1+.05*rank);case 14->1.5*(1+.05*rank);case 16->4.5*(1+.06*rank);default->1;};}
}
