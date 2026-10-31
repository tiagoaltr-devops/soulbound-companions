package br.com.soulbound.client;

import br.com.soulbound.ritual.TrialMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TrialScreen extends AbstractContainerScreen<TrialMenu> {
    public TrialScreen(TrialMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=300;imageHeight=230;}
    @Override protected void init(){
        super.init();
        for(int i=0;i<7;i++){final int symbol=i;addRenderableWidget(Button.builder(Component.translatable("symbol.soulbound."+i),b->send(symbol)).bounds(leftPos+12+(i%3)*94,topPos+85+(i/3)*25,90,20).build());}
        addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.trial_start"),b->send(7)).bounds(leftPos+12,topPos+168,276,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(leftPos+90,topPos+198,120,20).build());
    }
    private void send(int id){if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff142c40);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,12,12,0xffa7e8ff,false);
        g.drawString(font,Component.translatable("menu.soulbound.trial_level",menu.info.get(0)),12,30,0xffffffff,false);
        int phase=menu.info.get(1);
        g.drawString(font,Component.translatable("menu.soulbound.trial_phase."+phase),12,46,0xffffffff,false);
        if(phase==0)g.drawString(font,Component.translatable("clue.soulbound."+menu.info.get(3)),12,64,0xffffd080,false);
        if(phase==2)g.drawString(font,Component.translatable("menu.soulbound.bond_progress",menu.info.get(4)/20),12,64,0xffffd080,false);
    }
}
