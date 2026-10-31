package br.com.soulbound.client;

import br.com.soulbound.ritual.ForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    public ForgeScreen(ForgeMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=310;imageHeight=300;}
    @Override protected void init(){
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.forge_confirm"),b->send(0)).bounds(leftPos+12,topPos+185,286,20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.core_exchange"),b->send(1)).bounds(leftPos+12,topPos+210,286,20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.ritual_confirm"),b->send(2)).bounds(leftPos+12,topPos+235,286,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(leftPos+100,topPos+267,110,20).build());
    }
    private void send(int id){if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff142c40);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,12,12,0xffa7e8ff,false);
        g.drawString(font,Component.translatable("menu.soulbound.forge_state."+menu.info.get(0)),12,32,0xffffffff,false);
        g.drawString(font,Component.translatable("menu.soulbound.forge_attempts",menu.info.get(1)),12,48,0xffffffff,false);
        g.drawString(font,Component.translatable("menu.soulbound.forge_risk",String.format(java.util.Locale.ROOT,"%.2f",menu.info.get(2)/100.0),String.format(java.util.Locale.ROOT,"%.2f",menu.info.get(2)*menu.info.get(4)/1000000.0)),12,64,0xffffcc80,false);
        g.drawString(font,menu.coreName,12,82,0xffa7e8ff,false);
        g.drawString(font,Component.translatable("menu.soulbound.forge_materials"),12,96,0xffffffff,false);
        for(int i=0;i<menu.materials.size();i++)g.drawString(font,menu.materials.get(i),12,110+i*13,0xffffffff,false);
        g.drawString(font,Component.translatable("menu.soulbound.forge_essence"),12,153,0xffffffff,false);
        g.drawString(font,Component.translatable("menu.soulbound.ritual_cost",menu.info.get(5),menu.info.get(6)),12,169,0xffffcc80,false);
    }
}
