package br.com.soulbound.menu;

import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.companion.SoulStaffCommands;
import br.com.soulbound.companion.identity.CompanionStatus;
import br.com.soulbound.companion.identity.SoulboundRecord;
import br.com.soulbound.registry.ModMenus;
import br.com.soulbound.companion.recall.SoulRecallService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Slotless vanilla menu. Clients send only container/button IDs, never an owner or target entity UUID. */
public final class SoulCommandMenu extends CompanionInventoryMenu {
    public static final int FOLLOW = 0;
    public static final int STAY = 1;
    public static final int RECALL = 2;
    public static final int GOD = 3;
    public static final int LOCATE_RUIN = 4;
    public static final int PASSIVE=5,BABY_FORM=6,JUNIOR_FORM=7,TALENTS=8,PLENO_FORM=9,INVENTORY=10,CANCEL_TARGET=11,ULTIMATE=12;
    private final SimpleContainerData data = new SimpleContainerData(28);
    public br.com.soulbound.network.GrimoirePayload view;
    private br.com.soulbound.network.GrimoirePayload sentView;
    public static final int TAB_FIRST=100,TALENT_FIRST=200;
    public final SimpleContainerData talents=new SimpleContainerData(TalentMenu.DATA_SIZE);
    private final Player owner;
    private final InteractionHand hand;
    private final @Nullable SoulboundRecord session;

    /** Client construction; server values arrive through vanilla menu DataSlots. */
    public SoulCommandMenu(int id, Inventory inventory) { this(id, inventory, InteractionHand.MAIN_HAND, null); }

    public SoulCommandMenu(int id, Inventory inventory, InteractionHand hand, @Nullable SoulboundRecord session) {
        super(ModMenus.SOUL_COMMAND.get(),id,inventory,session);
        owner = inventory.player;
        this.hand = hand;
        this.session = session;
        addDataSlots(data);addDataSlots(talents);
        refresh();
    }

    private @Nullable SoulboundRecord current(ServerPlayer player) {
        if (session == null || player != owner) return null;
        var record = SoulStaffCommands.information(player, player.getItemInHand(hand));
        if(record==null)record=SoulStaffCommands.information(player,getCarried());
        if(record==null)for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(br.com.soulbound.companion.identity.SoulboundIdentity.isEgg(stack)){record=SoulStaffCommands.information(player,stack);if(record!=null)break;}}
        return record != null && record.matches(session.ownerUuid(), session.bondUuid(), session.companionUuid()) ? record : null;
    }

    private void refresh() {
        if (!(owner instanceof ServerPlayer player)) return;
        var record = current(player);
        if (record == null) { data.set(2, 0); return; }
        TalentMenu.sync(talents,player,record);
        var companion = SoulStaffCommands.loadedCompanion(player, record);
        data.set(0, record.companionStatus().ordinal());
        data.set(1, BehaviorMode.fromSavedName(record.behaviorMode()).ordinal());
        data.set(2, companion == null ? 0 : 1);
        data.set(3, companion == null ? 0 : companion.getThreatAlertEnd() > player.level().getGameTime() ? 2 : 1);
        data.set(4, companion == null ? 0 : companion.getMineralDiscoveryEnd() > player.level().getGameTime() ? 2 : 1);
        data.set(5, record.species().equals("LUPUS") ? 1 : 0);
        data.set(6, switch(record.evolutionStage()){case "BABY"->1;case "JUNIOR"->2;case "PLENO"->3;default->0;});
        data.set(9,record.unlockedForms().contains("GOD")?1:0);
        data.set(7, Math.min(32767, record.recall().remainingSeconds(SoulRecallService.now(player.level().getServer()))));
        data.set(8, SoulRecallService.inProgress(player.level().getServer(), record.bondUuid()) ? 1 : 0);
        data.set(10,br.com.soulbound.companion.AncestralResonance.remainingSeconds(player.level().getServer(),player.getUUID()));
        data.set(11,br.com.soulbound.companion.AncestralResonance.pending(player.level().getServer(),record.bondUuid()) || companion!=null && companion.isResonating()?1:0);
        data.set(12,record.activeForm().equals("GOD")?3:record.activeForm().equals("JUNIOR")?1:record.activeForm().equals("BABY")?0:2);data.set(13,record.unlockedForms().contains("JUNIOR")?1:0);
        data.set(14,(int)Math.min(32767,Math.max(0,(record.forms().activeUltimateEnd()-SoulRecallService.now(player.level().getServer())+19)/20)));
        data.set(15,companion!=null && companion.getTarget()!=null?1:0);
        data.set(17,companion!=null && companion.canFight()?1:0);
        data.set(18,record.progression().level());data.set(19,(int)record.progression().xp());
        data.set(20,record.progression().level()==30?0:(int)br.com.soulbound.companion.progression.ProgressionState.required(record.progression().level()));
        data.set(21,record.progression().available());data.set(22,record.progression().spent());
        data.set(23,record.unlockedForms().contains("PLENO")?1:0);data.set(26,companion!=null && companion.isAlive() && companion.distanceToSqr(player)<=64 && !br.com.soulbound.companion.CompanionFormService.ritualBusy(player.level().getServer(),player.getUUID())?1:0);
        long now=SoulRecallService.now(player.level().getServer());data.set(24,(int)Math.max(0,Math.min(900,(record.godCooldownEnd()-now+19)/20)));data.set(25,record.activeForm().equals("GOD")?(int)Math.max(0,(record.forms().god().end()-now+19)/20):0);
        data.set(16,br.com.soulbound.companion.JuniorCombat.ultimateState(record.activeForm(),companion!=null && br.com.soulbound.companion.target.TargetCommandService.has(companion)?BehaviorMode.FOLLOW_COMBAT:BehaviorMode.fromSavedName(record.behaviorMode()),companion!=null && companion.distanceToSqr(player)<=32*32,
            companion!=null && br.com.soulbound.companion.CompanionFormService.busy(companion),record.forms().activeUltimateEnd(),now));
    }

    @Override
    public void broadcastChanges() { refresh(); if(owner instanceof ServerPlayer p && p.containerMenu==this && (sentView==null || p.tickCount%10==0)){var r=current(p);if(r!=null){var next=br.com.soulbound.companion.GrimoireSnapshot.create(containerId,p,r);if(!next.equals(sentView)){sentView=next;net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,next);}}} super.broadcastChanges(); }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide() || (player instanceof ServerPlayer serverPlayer && current(serverPlayer) != null);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.containerMenu != this
                || current(serverPlayer) == null || session == null) return false;
        if(button==CANCEL_TARGET){br.com.soulbound.companion.target.TargetCommandService.cancel(serverPlayer);return true;}
        if(button==ULTIMATE)return br.com.soulbound.companion.CompanionFormService.ultimate(serverPlayer);
        if(button>=TAB_FIRST && button<TAB_FIRST+7){resetQuickCraft();data.set(27,button-TAB_FIRST);broadcastChanges();return true;}
        if(button>=TALENT_FIRST && button<TALENT_FIRST+br.com.soulbound.companion.progression.TalentDefinitions.ALL.size()){
            if(tab()!=2)return false;boolean ok=TalentMenu.invest(serverPlayer,current(serverPlayer),button-TALENT_FIRST);refresh();broadcastChanges();return ok;
        }
        if(button==INVENTORY){refresh();if(!canInventory())return false;resetQuickCraft();data.set(27,1);broadcastChanges();return true;}
        if(button==TALENTS){resetQuickCraft();data.set(27,2);broadcastChanges();return true;}
        if(button<0 || button>ULTIMATE)return false;
        if(button==BABY_FORM || button==JUNIOR_FORM)return br.com.soulbound.companion.CompanionFormService.select(serverPlayer,hand,button==BABY_FORM?"BABY":"JUNIOR");
        if(button==PLENO_FORM)return br.com.soulbound.companion.CompanionFormService.select(serverPlayer,hand,"PLENO");
        if(button==LOCATE_RUIN)return br.com.soulbound.companion.AncestralResonance.request(serverPlayer,hand,session);
        if(button==GOD)return br.com.soulbound.ritual.CompanionEvolution.god(serverPlayer);
        if (button == RECALL) return SoulRecallService.request(serverPlayer, hand, session);
        return SoulStaffCommands.setMode(serverPlayer, player.getItemInHand(hand), session,
                button == FOLLOW ? BehaviorMode.FOLLOW_COMBAT : button==PASSIVE?BehaviorMode.FOLLOW_PASSIVE:BehaviorMode.STAY);
    }

    @Override protected boolean inventoryVisible(){return data!=null && tab()==1 && canInventory();}
    public int tab(){return Math.max(0,Math.min(6,data.get(27)));}
    public void previewTab(int tab){if(owner.level().isClientSide() && tab>=0 && tab<7){resetQuickCraft();data.set(27,tab);}}
    public int talentRank(int index){return talents.get(7+index);}
    public int talentReason(int index){return talents.get(7+br.com.soulbound.companion.progression.TalentDefinitions.ALL.size()+index);}
    public boolean canInventory(){return data.get(26)==1;}
    public boolean canCommand() { return data.get(2) == 1; }
    public int recallSeconds() { return Math.max(0, data.get(7)); }
    public boolean recallPending() { return data.get(8) == 1; }
    public boolean canRecall() { return status() == CompanionStatus.ACTIVE && recallSeconds() == 0 && !recallPending(); }
    public BehaviorMode mode() {int i=data.get(1);return i>=0 && i<BehaviorMode.values().length?BehaviorMode.values()[i]:BehaviorMode.FOLLOW_PASSIVE;}
    public boolean juniorActive(){return data.get(12)==1;}
    public boolean babyActive(){return data.get(12)==0;}
    public boolean juniorUnlocked(){return data.get(13)==1;}
    public boolean plenoUnlocked(){return data.get(23)==1;}
    public boolean plenoActive(){return data.get(12)==2;}
    public int ultimateSeconds(){return data.get(14);}
    public boolean fighting(){return data.get(15)==1;}
    public boolean combatEnabled(){return data.get(17)==1;}
    public int companionLevel(){return Math.max(1,data.get(18));}
    public int companionXp(){return Math.max(0,data.get(19));}
    public int nextLevelXp(){return Math.max(0,data.get(20));}
    public int talentPoints(){return Math.max(0,data.get(21));}
    public int ultimateState(){return data.get(16);}
    public CompanionStatus status() {
        int index = data.get(0);
        return index >= 0 && index < CompanionStatus.values().length ? CompanionStatus.values()[index] : CompanionStatus.MISSING;
    }
    public int threatIndicator() { return data.get(3); }
    public int mineralIndicator() { return data.get(4); }
    public boolean isLupus() { return data.get(5) == 1; }
    public boolean isBaby() { return data.get(6) == 1; }
    public String stageKey(){return switch(data.get(6)){case 1->"stage.soulbound.baby";case 2->"stage.soulbound.junior";case 3->"stage.soulbound.pleno";default->"menu.soulbound.unknown";};}
    public boolean godActive(){return data.get(12)==3;} public int godCooldownSeconds(){return data.get(24);} public int godSeconds(){return data.get(25);}
    public boolean godUnlocked(){return data.get(9)==1;}
    public int resonanceSeconds(){return data.get(10);}
    public boolean resonancePending(){return data.get(11)==1;}
    public boolean canResonate(){return canCommand() && status()==CompanionStatus.ACTIVE && resonanceSeconds()==0 && !resonancePending() && !recallPending();}
}
