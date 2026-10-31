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
    private Button follow;
    private Button stay;
    private Button recall;
    private Button resonate;

    public SoulCommandScreen(SoulCommandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 266;
    }

    @Override
    protected void init() {
        super.init();
        follow = addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.follow"), button -> send(SoulCommandMenu.FOLLOW))
                .bounds(leftPos + 12, topPos + 160, 109, 20).build());
        stay = addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.stay"), button -> send(SoulCommandMenu.STAY))
                .bounds(leftPos + 129, topPos + 160, 109, 20).build());
        recall = addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.recall"), button -> send(SoulCommandMenu.RECALL))
                .bounds(leftPos + 12, topPos + 184, 226, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(leftPos + 75, topPos + 240, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.god_short"),button->send(SoulCommandMenu.GOD))
                .bounds(leftPos+12,topPos+212,109,20).build());
        resonate=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.locate_ruin"),button->send(SoulCommandMenu.LOCATE_RUIN))
                .bounds(leftPos+129,topPos+212,109,20).build());
        updateButtons();
    }

    private void send(int button) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
    }

    private void updateButtons() {
        follow.active = menu.canCommand() && menu.mode() != BehaviorMode.FOLLOW_PASSIVE;
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
        graphics.drawString(font, Component.translatable(menu.canCommand() ? "menu.soulbound.connected" : "menu.soulbound.unloaded"), 12, 102, 0xFFCCE6EF, false);
        line(graphics, "menu.soulbound.threat", indicator(menu.threatIndicator(), "threat"), 124);
        line(graphics, "menu.soulbound.mineral", indicator(menu.mineralIndicator(), "mineral"), 140);
    }

    private Component indicator(int value, String skill) {
        return Component.translatable(value == 0 ? "menu.soulbound.unavailable" : value == 2
                ? "menu.soulbound." + skill + "_detected" : "menu.soulbound.enabled");
    }

    private void line(GuiGraphics graphics, String key, Component value, int y) {
        graphics.drawString(font, Language.getInstance().getVisualOrder(font.substrByWidth(Component.translatable(key, value), imageWidth - 24)), 12, y, 0xFFE2F5FF, false);
    }
}
