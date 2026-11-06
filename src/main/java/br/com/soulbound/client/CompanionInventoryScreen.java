package br.com.soulbound.client;

import br.com.soulbound.menu.CompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    public CompanionInventoryScreen(CompanionInventoryMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=196;inventoryLabelY=101;}
    @Override protected void init(){super.init();addRenderableWidget(new GrimoireButton(leftPos+112,topPos+57,58,18,Component.translatable("grimoire.soulbound.back"),b->net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new br.com.soulbound.network.GrimoireOpenPayload(menu.containerId))));}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff142c40);for(var slot:menu.slots)g.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xff08151f);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){super.renderLabels(g,x,y);for(int i=0;i<5;i++)g.drawString(font,Component.translatable("menu.soulbound.gear."+i),16+i*28,24,0xffa7e8ff,false);g.drawString(font,Component.translatable("menu.soulbound.food"),12,62,0xffa7e8ff,false);}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
