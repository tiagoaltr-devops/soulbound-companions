package br.com.soulbound.companion;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.progression.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.network.GrimoirePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Builds display-only data from canonical records and effective native attributes. */
public final class GrimoireSnapshot {
    private GrimoireSnapshot(){}
    private static Component label(String key,Object value){return Component.translatable("grimoire.soulbound."+key,value);}
    private static Component tr(String category,String value){return Component.translatable(category+".soulbound."+value.toLowerCase(Locale.ROOT));}
    private static String number(double value){return String.format(Locale.ROOT,"%.2f",value);}
    private static int seconds(long end,long now){return (int)Math.min(Integer.MAX_VALUE,Math.max(0,(end-now+19)/20));}
    public static GrimoirePayload create(int menu,ServerPlayer p,SoulboundRecord r){
        LupusCompanionEntity c=null;for(var level:p.level().getServer().getAllLevels())if(level.getEntity(r.companionUuid()) instanceof LupusCompanionEntity found && SoulboundIdentity.isCanonical(found)){c=found;break;}
        long now=p.level().getServer().overworld().getGameTime();var overview=new ArrayList<Component>();var abilities=new ArrayList<Component>();var stats=new ArrayList<Component>();var bond=new ArrayList<Component>();
        overview.add(label("name",c==null?Component.translatable("entity.soulbound.lupus_companion"):c.getDisplayName()));
        overview.add(label("species",tr("species",r.species())));overview.add(label("form",tr("stage",r.activeForm())));
        overview.add(Component.translatable("grimoire.soulbound.progress",r.progression().level(),r.progression().xp(),r.progression().level()==30?Component.translatable("hud.soulbound.xp_max"):ProgressionState.required(r.progression().level())));
        overview.add(label("status",tr("status",r.companionStatus().name())));overview.add(label("mode",tr("behavior",r.behaviorMode())));
        overview.add(r.companionStatus()==CompanionStatus.DEAD?Component.translatable("hud.soulbound.dead"):c==null?Component.translatable("grimoire.soulbound.unloaded"):Component.translatable("hud.soulbound.health",number(c.getHealth()),number(c.getMaxHealth())));
        overview.add(label("points",r.progression().available()));
        abilities.add(label("threat",c==null?Component.translatable("grimoire.soulbound.unloaded"):Component.translatable(c.getThreatAlertEnd()>c.level().getGameTime()?"grimoire.soulbound.alert":"grimoire.soulbound.enabled")));
        abilities.add(label("mineral",c==null?Component.translatable("grimoire.soulbound.unloaded"):Component.translatable(c.getMineralDiscoveryEnd()>c.level().getGameTime()?"grimoire.soulbound.discovery":"grimoire.soulbound.enabled")));
        abilities.add(Component.translatable("grimoire.soulbound.exploration_description"));
        if(!r.activeForm().equals("BABY")){
            abilities.add(Component.translatable("grimoire.soulbound.cooldown",Component.translatable("key.soulbound.ultimate"),seconds(r.forms().activeUltimateEnd(),now)));
            abilities.add(Component.translatable("grimoire.soulbound.ultimate_description"));
        }
        if(r.activeForm().equals("PLENO") || r.activeForm().equals("GOD")){
            abilities.add(Component.translatable("grimoire.soulbound.cooldown",Component.translatable("grimoire.soulbound.charge"),seconds(r.activeForm().equals("GOD")?r.forms().god().chargeEnd():r.forms().chargeEnd(),now)));
            abilities.add(Component.translatable("grimoire.soulbound.cooldown",Component.translatable("grimoire.soulbound.quake"),seconds(r.activeForm().equals("GOD")?r.forms().god().ruptureEnd():r.forms().quakeEnd(),now)));
            abilities.add(Component.translatable("grimoire.soulbound.secondary_description"));
        }
        abilities.add(Component.translatable("grimoire.soulbound.cooldown",Component.translatable("menu.soulbound.recall"),r.recall().remainingSeconds(now)));
        if(r.unlockedForms().contains("GOD"))abilities.add(Component.translatable("grimoire.soulbound.cooldown",Component.translatable("stage.soulbound.god"),seconds(r.godCooldownEnd(),now)));
        var profile=TalentEffects.profile(r);
        if(profile.fury()>0){abilities.add(Component.translatable("grimoire.soulbound.cooldown",Component.translatable(TalentDefinitions.find("furia_ancestral").nameKey()),seconds(r.progression().furyCooldownEnd(),now)));abilities.add(Component.translatable(TalentDefinitions.find("furia_ancestral").nameKey()+".description"));}
        stats.add(Component.translatable("grimoire.soulbound.regen",number(SurvivalRules.regeneration(r.activeForm(),Math.round(profile.regeneration()*10)))));
        stats.add(Component.translatable("grimoire.soulbound.bonuses",number(profile.health()),number(profile.damage()),number(profile.reduction()*100)));
        if(c!=null){
            for(var attribute:List.of(Attributes.MAX_HEALTH,Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.ATTACK_DAMAGE,Attributes.ATTACK_SPEED,Attributes.MOVEMENT_SPEED,Attributes.FLYING_SPEED,Attributes.KNOCKBACK_RESISTANCE)){
                var a=c.getAttribute(attribute);if(a!=null){stats.add(Component.translatable("grimoire.soulbound.attribute",Component.translatable(attribute.value().getDescriptionId()),number(a.getBaseValue()),number(a.getValue())));
                    for(var modifier:a.getModifiers())stats.add(Component.translatable("grimoire.soulbound.modifier",modifier.id().toString(),number(modifier.amount()),Component.translatable("grimoire.soulbound.operation."+modifier.operation().name().toLowerCase(Locale.ROOT))));}
            }
        }else stats.add(Component.translatable("grimoire.soulbound.unloaded"));
        bond.add(label("owner",r.ownerLastKnownName()));bond.add(label("owner_uuid",r.ownerUuid().toString()));bond.add(label("bond_uuid",r.bondUuid().toString()));bond.add(label("companion_uuid",r.companionUuid().toString()));
        bond.add(label("archetype",r.archetypeId()));bond.add(label("evolution",number(r.evolutionProgress())));
        bond.add(label("highest_form",tr("stage",r.evolutionStage())));
        var forms=Component.empty();for(String form:List.of("BABY","JUNIOR","PLENO","GOD"))if(r.unlockedForms().contains(form)){if(!forms.getString().isEmpty())forms.append(", ");forms.append(tr("stage",form));}bond.add(label("unlocked",forms));
        var history=br.com.soulbound.ritual.AltarSavedData.get(p.level().getServer()).state.progress.get(r.ownerUuid().toString());
        if(history!=null)bond.add(label("resurrections",history.resurrections));
        if(r.activeForm().equals("GOD"))overview.add(Component.translatable("hud.soulbound.barrier",number(r.forms().god().shield()),number(r.forms().god().maximum())));
        bond.add(Component.translatable("grimoire.soulbound.permanent"));bond.add(Component.translatable("grimoire.soulbound.history_unavailable"));
        return new GrimoirePayload(menu,overview,abilities,stats.stream().limit(64).toList(),bond);
    }
}
