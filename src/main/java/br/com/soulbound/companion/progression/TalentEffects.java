package br.com.soulbound.companion.progression;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Cached numeric profile; no attribute modifiers accumulate and models know nothing about talents. */
@EventBusSubscriber(modid="soulbound")
public final class TalentEffects {
    public record Profile(double health,double damage,double basic,double ultimate,double attackSpeed,double reduction,float regeneration,int minerals,int resonance,int aura,int fury,int ultimateCooldown,int dominance,int charge,int quake,int cataclysm,int guardian,int primordial,int celestial,int divine,int apocalypse,int eternal) {}
    public static final Profile NONE=new Profile(1,1,1,1,1,0,0,0,0,0,0,1200,0,0,0,0,0,0,0,0,0,0);
    private TalentEffects() {}
    private static int rank(SoulboundRecord r,String key){return r.progression().rank("soulbound:"+key);}
    public static Profile profile(SoulboundRecord r){
        boolean baby=r.activeForm().equals("BABY"),junior=r.activeForm().equals("JUNIOR"),pleno=r.activeForm().equals("PLENO"),god=r.activeForm().equals("GOD");
        return new Profile(1+.04*rank(r,"vitalidade_ancestral"),junior || pleno || god?1+.03*rank(r,"forca_espiritual"):1,junior?1+.04*rank(r,"golpe_aprimorado"):1,
            junior?1+.08*rank(r,"ruptura_potencializada"):1,junior?1+.03*rank(r,"instinto_predador"):1,.03*(rank(r,"pele_runica")+(god?rank(r,"essencia_eterna"):0)),.1f*rank(r,"regeneracao_espiritual"),
            baby?rank(r,"olhar_ancestral"):0,baby?rank(r,"ressonancia_aprimorada"):0,baby?rank(r,"aura_protetora"):0,junior?rank(r,"furia_ancestral"):0,
            Math.max(600,(int)Math.round(1200*(1-.04*(junior?rank(r,"sintonia_de_combate"):0)))),
            pleno?rank(r,"dominio_ancestral"):0,pleno?rank(r,"investida_implacavel"):0,pleno?rank(r,"impacto_runico"):0,pleno?rank(r,"cataclismo_aprimorado"):0,pleno?rank(r,"guardiao_inabalavel"):0,god?rank(r,"poder_primordial"):0,god?rank(r,"investida_suprema"):0,god?rank(r,"ruptura_absoluta"):0,god?rank(r,"apocalipse_potencializado"):0,god?rank(r,"essencia_eterna"):0);
    }
    public static float damage(LupusCompanionEntity c,float base,boolean ultimate){
        var p=c.talents();double fury=1;
        if(p.fury()>0){var r=SoulboundSavedData.get(c.level().getServer()).find(c.getOwnerUuid());if(r!=null && r.progression().furyEnd()>c.level().getServer().overworld().getGameTime())fury=1.1;}
        double effective=c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if(ultimate)effective*=base/Math.max(.001,br.com.soulbound.Config.JUNIOR_ATTACK_DAMAGE.get());
        return (float)(effective*p.damage()*(ultimate?p.ultimate():p.basic())*fury);
    }
    public static int interval(LupusCompanionEntity c,int base){double speed=c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);return Math.max(5,(int)Math.min(1200,Math.ceil(base/c.talents().attackSpeed()*4/Math.max(.1,speed))));}
    public static int mineralRadius(LupusCompanionEntity c){return Math.min(32,br.com.soulbound.Config.MINERAL_RADIUS.get()+c.talents().minerals()*br.com.soulbound.Config.TALENT_MINERAL_INCREMENT.get());}
    public static void kill(MinecraftServer server,java.util.UUID owner){
        var data=SoulboundSavedData.get(server);var r=data.find(owner);long now=server.overworld().getGameTime();
        if(r==null || !r.activeForm().equals("JUNIOR") || rank(r,"furia_ancestral")==0 || r.companionStatus()!=CompanionStatus.ACTIVE || now<r.progression().furyCooldownEnd())return;
        data.put(r.withProgression(r.progression().fury(now+100,now+400)));data.flush(server);
    }
    public static void tick(LupusCompanionEntity c){
        if(!(c.level() instanceof ServerLevel level) || !c.isAlive() || !SoulboundIdentity.isCanonical(c))return;
        long now=level.getServer().overworld().getGameTime();var p=c.talents();
        if(c.tickCount%20==0){
            br.com.soulbound.companion.CompanionSurvival.regenerate(c);
            var owner=c.getOwnerPlayer();int radius=4+2*p.aura();
            if(p.aura()>0 && owner!=null && owner.isAlive() && !owner.isSpectator() && c.distanceToSqr(owner)<=radius*radius)
                owner.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,40,0,true,false,true));
        }
    }
    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event){
        if(event.getEntity() instanceof LupusCompanionEntity c && !c.level().isClientSide() && c.isAlive()){
            if(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity)c.markCombat();event.setAmount((float)(event.getAmount()*(1-c.talents().reduction())));
        }
    }
}
