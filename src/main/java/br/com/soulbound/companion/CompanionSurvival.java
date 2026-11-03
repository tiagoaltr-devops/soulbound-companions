package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;

public final class CompanionSurvival {
    private CompanionSurvival(){}
    public static boolean defensive(LupusCompanionEntity c){
        if(c.level().isClientSide() || c.getOwnerUuid()==null || !c.isAlive())return false;
        var r=SoulboundSavedData.get(c.level().getServer()).find(c.getOwnerUuid());
        return r!=null && SurvivalRules.defensive(r.survival().defensive(),c.getHealth(),c.getMaxHealth());
    }
    public static void tick(LupusCompanionEntity c){
        if(c.level().isClientSide() || !c.isAlive() || !SoulboundIdentity.isCanonical(c))return;
        var data=SoulboundSavedData.get(c.level().getServer());var r=data.find(c.getOwnerUuid());boolean defense=defensive(c);
        if(defense!=r.survival().defensive()){
            data.put(r.withSurvival(r.survival().defense(defense)));
            if(defense){c.plenoCombat().cancelAll();c.setTarget(null);}
        }
        if(c.tickCount%10==0 && (defense || c.getTarget()==null) && c.getActionEnd()<=c.level().getGameTime())CompanionFood.automatic(c);
    }
    public static void markCombat(LupusCompanionEntity c){
        if(c.level().isClientSide() || c.getOwnerUuid()==null || !SoulboundIdentity.isCanonical(c))return;
        var data=SoulboundSavedData.get(c.level().getServer());var r=data.find(c.getOwnerUuid());
        data.put(r.withSurvival(r.survival().combat(c.level().getServer().overworld().getGameTime()+160)));
    }
    public static void regenerate(LupusCompanionEntity c){
        if(!c.isAlive() || !SoulboundIdentity.isCanonical(c) || c.getTarget()!=null || CompanionFormService.busy(c) || c.getHealth()>=c.getMaxHealth())return;
        var s=c.level().getServer();long now=s.overworld().getGameTime();var data=SoulboundSavedData.get(s);var r=data.find(c.getOwnerUuid());
        if(now<r.survival().combatUntil() || now<r.survival().regenEnd())return;
        c.heal(SurvivalRules.regeneration(r.activeForm(),Math.round(c.talents().regeneration()*10)));
        r=data.find(c.getOwnerUuid());data.put(r.withSurvival(r.survival().regenerated(now+40)));
    }
}
