package br.com.soulbound.client;

import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.menu.SoulCommandMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.world.entity.player.Inventory;

/** All controls send vanilla menu button packets; this screen never changes entity state locally. */
public final class SoulCommandScreen extends AbstractContainerScreen<SoulCommandMenu> {
    private Button equipment;private Button god;private Button follow;
    private Button stay;
    private Button recall;
    private Button resonate;
    private Button passive,baby,junior,pleno;
    private Button talents;

    public SoulCommandScreen(SoulCommandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 356;
    }

    @Override
    protected void init() {
        super.init();
        follow = addRenderableWidget(Button.builder(Component.translatable("behavior.soulbound.follow_combat"), button -> send(SoulCommandMenu.FOLLOW))
                .bounds(leftPos + 12, topPos + 160, 109, 20).build());
        stay = addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.stay"), button -> send(SoulCommandMenu.STAY))
                .bounds(leftPos + 129, topPos + 160, 109, 20).build());
        recall = addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.recall"), button -> send(SoulCommandMenu.RECALL))
                .bounds(leftPos + 12, topPos + 184, 226, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(leftPos + 75, topPos + 330, 100, 20).build());
        god=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.god_short"),button->send(SoulCommandMenu.GOD))
                .bounds(leftPos+12,topPos+212,109,20).build());
        resonate=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.locate_ruin"),button->send(SoulCommandMenu.LOCATE_RUIN))
                .bounds(leftPos+129,topPos+212,109,20).build());
        baby=addRenderableWidget(Button.builder(Component.translatable("stage.soulbound.baby"),b->send(SoulCommandMenu.BABY_FORM)).bounds(leftPos+12,topPos+240,72,20).build());
        junior=addRenderableWidget(Button.builder(Component.translatable("stage.soulbound.junior"),b->send(SoulCommandMenu.JUNIOR_FORM)).bounds(leftPos+89,topPos+240,72,20).build());
        pleno=addRenderableWidget(Button.builder(Component.translatable("stage.soulbound.pleno"),b->send(SoulCommandMenu.PLENO_FORM)).bounds(leftPos+166,topPos+240,72,20).build());
        passive=addRenderableWidget(Button.builder(Component.translatable("behavior.soulbound.follow_passive"),b->send(SoulCommandMenu.PASSIVE)).bounds(leftPos+12,topPos+264,109,20).build());
        talents=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.talents"),b->send(SoulCommandMenu.TALENTS)).bounds(leftPos+12,topPos+100,226,14).build());
        equipment=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.inventory"),b->send(SoulCommandMenu.INVENTORY)).bounds(leftPos+129,topPos+264,109,20).build());
        updateButtons();
    }

    private void send(int button) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event){return !event.isEscape() && JuniorControls.fromMenu(event) || super.keyPressed(event);}

    private void updateButtons() {
        equipment.active=menu.canInventory();
        god.active=menu.canCommand() && menu.plenoActive() && menu.godUnlocked() && menu.godCooldownSeconds()==0;god.setMessage(Component.translatable(menu.godActive()?"menu.soulbound.god_active_time":!menu.godUnlocked()?"menu.soulbound.god_locked":menu.godCooldownSeconds()>0?"menu.soulbound.god_wait":"menu.soulbound.god_short",menu.godActive()?menu.godSeconds():menu.godCooldownSeconds()));
        talents.setMessage(Component.translatable("menu.soulbound.progress_summary",menu.companionLevel(),menu.companionXp(),menu.nextLevelXp(),menu.talentPoints()));
        follow.active = menu.canCommand() && menu.mode() != BehaviorMode.FOLLOW_COMBAT;
        passive.active=menu.canCommand() && menu.mode()!=BehaviorMode.FOLLOW_PASSIVE;
        baby.active=!menu.godActive() && menu.canCommand() && !menu.babyActive();junior.active=!menu.godActive() && menu.canCommand() && menu.juniorUnlocked() && !menu.juniorActive();
        pleno.active=!menu.godActive() && menu.canCommand() && menu.plenoUnlocked() && !menu.plenoActive();pleno.setMessage(Component.translatable(menu.plenoUnlocked()?"stage.soulbound.pleno":"menu.soulbound.pleno_locked"));
        junior.setMessage(Component.translatable(menu.juniorUnlocked()?"stage.soulbound.junior":"menu.soulbound.junior_locked"));
        stay.active = menu.canCommand() && menu.mode() != BehaviorMode.STAY;
        recall.active = menu.canRecall();
        resonate.active=menu.canResonate();
        resonate.setMessage(Component.translatable(menu.resonancePending()?"menu.soulbound.resonance_searching":menu.resonanceSeconds()>0?"menu.soulbound.resonance_cooldown":"menu.soulbound.locate_ruin",menu.resonanceSeconds()));
        recall.setMessage(menu.recallPending() ? Component.translatable("menu.soulbound.recall_pending")
                : menu.recallSeconds() > 0 ? Component.translatable("menu.soulbound.recall_cooldown", menu.recallSeconds())
                : Component.translatable("menu.soulbound.recall"));
    }

    @Override
    protected void containerTick() { super.containerTick(); updateButtons(); }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF10243D);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF1C3551);
        graphics.fill(leftPos + 10, topPos + 30, leftPos + imageWidth - 10, topPos + 31, 0xFF65BEDF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(title, imageWidth - 24)), 12, 12, 0xFFA7E8FF, false);
        line(graphics, "tooltip.soulbound.species", Component.translatable(menu.isLupus() ? "species.soulbound.lupus" : "menu.soulbound.unknown"), 40);
        line(graphics, "tooltip.soulbound.stage", Component.translatable(menu.stageKey()), 54);
        line(graphics, "tooltip.soulbound.status", Component.translatable("status.soulbound." + menu.status().name().toLowerCase(java.util.Locale.ROOT)), 68);
        line(graphics, "tooltip.soulbound.mode", Component.translatable("behavior.soulbound." + menu.mode().name().toLowerCase(java.util.Locale.ROOT)), 84);
        graphics.fill(12,115,238,119,0xff18202a);graphics.fill(12,115,12+(menu.nextLevelXp()==0?226:226*menu.companionXp()/Math.max(1,menu.nextLevelXp())),119,0xff48cfe0);
        line(graphics, "menu.soulbound.threat", indicator(menu.threatIndicator(), "threat"), 124);
        line(graphics, "menu.soulbound.mineral", indicator(menu.mineralIndicator(), "mineral"), 140);
        graphics.drawString(font,Component.translatable("menu.soulbound.active_form",Component.translatable(menu.godActive()?"stage.soulbound.god":menu.juniorActive()?"stage.soulbound.junior":menu.babyActive()?"stage.soulbound.baby":"stage.soulbound.pleno")),12,290,0xffa7e8ff,false);
        graphics.drawString(font,Component.translatable(menu.fighting()?"menu.soulbound.in_combat":menu.combatEnabled()?"menu.soulbound.combat_enabled":"menu.soulbound.combat_disabled"),12,303,0xffffffff,false);
        var ultimate=menu.ultimateState()==6?Component.translatable(menu.godActive()?"menu.soulbound.apoc_seconds":menu.plenoActive()?"menu.soulbound.cat_seconds":"menu.soulbound.ultimate_seconds",menu.ultimateSeconds()):Component.translatable((menu.godActive()?"menu.soulbound.apoc_state.":menu.plenoActive()?"menu.soulbound.cat_state.":"menu.soulbound.ultimate_state.")+menu.ultimateState());
        graphics.drawString(font,Language.getInstance().getVisualOrder(font.substrByWidth(Component.translatable("menu.soulbound.ultimate_key",JuniorControls.shortcut(),ultimate),imageWidth-24)),12,316,0xffffffff,false);
    }

    private Component indicator(int value, String skill) {
        return Component.translatable(value == 0 ? "menu.soulbound.unavailable" : value == 2
                ? "menu.soulbound." + skill + "_detected" : "menu.soulbound.enabled");
    }

    private void line(GuiGraphics graphics, String key, Component value, int y) {
        graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(Component.translatable(key, value), imageWidth - 24)), 12, y, 0xFFE2F5FF, false);
    }
}
