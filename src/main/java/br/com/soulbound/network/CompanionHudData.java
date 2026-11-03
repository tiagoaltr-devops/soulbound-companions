package br.com.soulbound.network;

import java.util.UUID;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.progression.ProgressionState;

/** Ephemeral presentation snapshot, never a second identity or an authorization source. */
public record CompanionHudData(boolean present,UUID owner,UUID bond,String name,String form,CompanionStatus status,
        int level,long xp,long required,boolean healthKnown,float health,float maxHealth,float shield,float maxShield,int godSeconds,int godCooldownSeconds) {
    public CompanionHudData(boolean present,UUID owner,UUID bond,String name,String form,CompanionStatus status,int level,long xp,long required,boolean known,float health,float maximum){this(present,owner,bond,name,form,status,level,xp,required,known,health,maximum,0,0,0,0);}
    public CompanionHudData god(SoulboundRecord r,long now){boolean active=r.activeForm().equals("GOD") && r.companionStatus()==CompanionStatus.ACTIVE && now<r.forms().god().end();return new CompanionHudData(present,owner,bond,name,form,status,level,xp,required,healthKnown,health,maxHealth,active?r.forms().god().shield():0,active?r.forms().god().maximum():0,active?(int)Math.max(0,(r.forms().god().end()-now+19)/20):0,(int)Math.min(900,Math.max(0,(r.godCooldownEnd()-now+19)/20)));}
    private static final UUID ZERO=new UUID(0,0);
    public static final CompanionHudData EMPTY=new CompanionHudData(false,ZERO,ZERO,"","BABY",CompanionStatus.MISSING,1,0,0,false,0,0);
    public static CompanionHudData project(SoulboundRecord r,String name,boolean reliable,float current,float maximum){
        if(r==null)return EMPTY;
        boolean known=r.companionStatus()==CompanionStatus.ACTIVE && reliable && Float.isFinite(current) && Float.isFinite(maximum) && maximum>0;
        return new CompanionHudData(true,r.ownerUuid(),r.bondUuid(),name.length()>128?name.substring(0,128):name,r.activeForm(),r.companionStatus(),
            r.progression().level(),r.progression().xp(),r.progression().level()==30?0:ProgressionState.required(r.progression().level()),known,
            known?Math.max(0,Math.min(current,maximum)):0,known?maximum:0);
    }
    public static float fraction(double current,double maximum){return !Double.isFinite(current) || !Double.isFinite(maximum) || maximum<=0?0:(float)Math.max(0,Math.min(1,current/maximum));}
    public float healthFraction(){return healthKnown?fraction(health,maxHealth):0;}
    public float xpFraction(){return level>=30?1:fraction(xp,required);}
}
