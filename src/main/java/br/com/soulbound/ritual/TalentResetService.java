package br.com.soulbound.ritual;

import java.util.UUID;
import br.com.soulbound.companion.identity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/** Uses the existing altar escrow and a journaled token; no second progression identity. */
public final class TalentResetService {
    public static final int LEVELS=10,AMETHYST=8,DURATION=200;
    private TalentResetService() {}
    public static String problem(ServerPlayer p,AltarSavedData.Forge f){
        var s=p.level().getServer();var d=AltarSavedData.get(s);var r=SoulboundSavedData.get(s).find(p.getUUID());
        if(!p.isAlive() || p.isSpectator() || r==null || r.companionStatus()==CompanionStatus.MISSING || r.companionStatus()==CompanionStatus.SUMMONING)return "bond";
        if(!f.placed || !f.state.equals("ACTIVE") || RitualService.busy(p.level(),f.id) || br.com.soulbound.companion.CompanionFormService.ritualBusy(s,p.getUUID()))return "busy";
        if(r.progression().spent()==0)return "points";int rotation=ForgeService.rotation(p.level(),new BlockPos(f.x,f.y,f.z));if(rotation<0)return "structure";
        if(!RitualService.platform(p,f,rotation))return "platform";if(!f.core.isEmpty())return "core";
        if(p.experienceLevel<LEVELS)return "levels";
        var key=ForgeService.offeringKeys(p.level(),f,rotation).getFirst();var stack=AltarInventory.decode(p.level(),d.state.slots.get(key));
        if(!stack.is(Items.AMETHYST_SHARD) || stack.getCount()<AMETHYST)return "materials";
        if(InventoryTransfers.pending(d,key) || InventoryTransfers.pending(d,"forge/"+f.id) || RitualService.locked(p.level(),BlockPos.of(Long.parseLong(key.substring(key.lastIndexOf(':')+1)))))return "busy";
        return "ready";
    }
    public static boolean start(ServerPlayer p,AltarSavedData.Forge f){
        String reason=problem(p,f);if(!reason.equals("ready"))return ForgeService.reject(p,net.minecraft.network.chat.Component.translatable("message.soulbound.reset."+reason));
        var s=p.level().getServer();var d=AltarSavedData.get(s);var record=SoulboundSavedData.get(s).find(p.getUUID());
        var r=new AltarSavedData.Ritual();r.id=UUID.randomUUID().toString();r.player=p.getStringUUID();r.bond=record.bondUuid().toString();r.forge=f.id;r.kind="talent_reset";r.core="";
        r.rotation=ForgeService.rotation(p.level(),new BlockPos(f.x,f.y,f.z));r.end=s.overworld().getGameTime()+DURATION;
        var key=ForgeService.offeringKeys(p.level(),f,r.rotation).getFirst();var stack=AltarInventory.decode(p.level(),d.state.slots.get(key));
        p.giveExperienceLevels(-LEVELS);InventoryTransfers.receipt(p,"ritual:"+r.id);InventoryTransfers.saveReceipt(p,"ritual:"+r.id);
        r.offerings.put(key,AltarInventory.encode(p.level(),stack.split(AMETHYST)));d.state.slots.put(key,AltarInventory.encode(p.level(),stack));
        d.state.rituals.put(r.id,r);d.flush(s);OfferingBlockEntity.refresh(p.level(),key);return true;
    }
    public static void tick(MinecraftServer s,AltarSavedData d,AltarSavedData.Ritual r){
        var p=s.getPlayerList().getPlayer(UUID.fromString(r.player));var f=d.state.forges.get(r.forge);var record=SoulboundSavedData.get(s).find(UUID.fromString(r.player));
        if(p==null || !p.isAlive() || p.isSpectator() || !f.placed || !f.state.equals("ACTIVE") || record==null || !record.bondUuid().toString().equals(r.bond)
                || record.companionStatus()==CompanionStatus.MISSING || record.companionStatus()==CompanionStatus.SUMMONING || !RitualService.platform(p,f,r.rotation)
                || ForgeService.rotation(p.level(),new BlockPos(f.x,f.y,f.z))!=r.rotation){RitualService.refund(d,r,s);return;}
        if(s.overworld().getGameTime()<r.end){if(s.overworld().getGameTime()%10==0)p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,f.x+.5,f.y+1.5,f.z+.5,4,.3,.3,.3,.02);return;}
        var token=AltarTokens.issue(d,r.player,"talent_reset");token.spent=true;token.delivered=true;token.applicationOwner=r.player;token.applicationBond=r.bond;token.applicationPending=true;
        d.state.rituals.remove(r.id);d.flush(s);replay(s,d,token);
        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.soulbound.reset_complete"),false);
        p.level().playSound(null,new BlockPos(f.x,f.y,f.z),net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,net.minecraft.sounds.SoundSource.BLOCKS,.8f,1.2f);
    }
    public static void replay(MinecraftServer s,AltarSavedData d,AltarSavedData.Token token){
        if(!token.applicationPending)return;
        var identities=SoulboundSavedData.get(s);var owner=UUID.fromString(token.applicationOwner);var r=identities.find(owner);
        if(r==null || !r.bondUuid().toString().equals(token.applicationBond))throw new IllegalStateException("Talent reset bond mismatch");
        var next=r.withProgression(r.progression().reset());identities.put(next);identities.flush(s);token.applicationPending=false;d.flush(s);
        for(var level:s.getAllLevels())if(level.getEntity(next.companionUuid()) instanceof br.com.soulbound.entity.LupusCompanionEntity c)c.applyIdentity(next);
        var p=s.getPlayerList().getPlayer(owner);if(p!=null)SoulboundIdentity.synchronizePlayer(p);
    }
}
