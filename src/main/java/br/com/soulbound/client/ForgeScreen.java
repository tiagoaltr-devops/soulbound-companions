package br.com.soulbound.client;

import br.com.soulbound.ritual.ForgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private Button activate,core,ritual,reset;
    public ForgeScreen(ForgeMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=310;imageHeight=300;}
    @Override protected void init(){
        super.init();
        activate=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.forge_activate"),b->send(0)).bounds(leftPos+12,topPos+185,286,20).build());
        core=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.core_short"),b->send(1)).bounds(leftPos+12,topPos+210,140,20).build());
        reset=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.talent_reset"),b->send(3)).bounds(leftPos+158,topPos+210,140,20).build());
        reset.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("menu.soulbound.talent_reset_cost")));
        ritual=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.ritual_confirm"),b->send(2)).bounds(leftPos+12,topPos+235,286,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(leftPos+100,topPos+267,110,20).build());
        updateButtons();
    }
    private void updateButtons(){
        int state=menu.info.get(0);activate.visible=state==0 || state==1;activate.active=activate.visible;
        activate.setMessage(Component.translatable(state==1?"menu.soulbound.forge_reactivate":"menu.soulbound.forge_activate"));
        core.visible=state==2;core.active=core.visible;ritual.visible=state==2;ritual.active=ritual.visible;
        reset.visible=state==2;reset.active=reset.visible && menu.info.get(11)==0;
        ritual.setMessage(Component.translatable(menu.info.get(9)==1?"menu.soulbound.junior_ritual":"menu.soulbound.ritual_confirm"));
        if(menu.info.get(13)==1)ritual.setMessage(Component.translatable("menu.soulbound.god_ritual"));
        if(menu.info.get(12)==1)ritual.setMessage(Component.translatable("menu.soulbound.pleno_ritual"));
        if(menu.info.get(10)==1)ritual.setMessage(Component.translatable("menu.soulbound.resurrect"));
        if(menu.info.get(11)>0){core.active=false;ritual.active=false;ritual.setMessage(Component.translatable("menu.soulbound.ritual_remaining",menu.info.get(11)));}
    }
    @Override protected void containerTick(){super.containerTick();updateButtons();}
    private void send(int id){if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xff142c40);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,title,12,12,0xffa7e8ff,false);
        int state=menu.info.get(0);
        if(state!=3){int feedbackY=state==2?45:211;int lines=0;for(var line:font.split(menu.feedback,280)){if(lines++==3)break;g.drawString(font,line,12,feedbackY,0xffffa080,false);feedbackY+=11;}}
        if(state==0){
            int lineY=48;for(var line:font.split(Component.translatable("menu.soulbound.forge_dormant"),280)){g.drawString(font,line,12,lineY,0xffffffff,false);lineY+=12;}return;
        }
        if(state==4){
            g.drawString(font,Component.translatable("menu.soulbound.forge_awakening"),12,48,0xffa7e8ff,false);
            g.drawString(font,Component.translatable("menu.soulbound.forge_remaining",menu.info.get(7)),12,70,0xffffffff,false);return;
        }
        g.drawString(font,Component.translatable("menu.soulbound.forge_state."+state),12,32,0xffffffff,false);
        if(state==3)return;
        if(state==1){
            g.drawString(font,Component.translatable("menu.soulbound.forge_risk",String.format(java.util.Locale.ROOT,"%.2f",menu.info.get(2)/100.0),String.format(java.util.Locale.ROOT,"%.2f",menu.info.get(2)*menu.info.get(4)/1000000.0)),12,54,0xffffcc80,false);
            g.drawString(font,Component.translatable("menu.soulbound.forge_essence_hand"),12,78,0xffffffff,false);
            offerings(g);return;
        }
        g.drawString(font,menu.coreName,12,82,0xffa7e8ff,false);
        offerings(g);
        if(menu.info.get(9)==1)g.drawString(font,Component.translatable(menu.info.get(8)==1?"menu.soulbound.junior_ready":"menu.soulbound.junior_requires"),12,153,0xffa7e8ff,false);
        if(menu.info.get(13)==1)g.drawString(font,Component.translatable("menu.soulbound.god_requires"),12,153,0xffa7e8ff,false);
        if(menu.info.get(12)==1)g.drawString(font,Component.translatable("menu.soulbound.pleno_requires"),12,153,0xffa7e8ff,false);
        g.drawString(font,Component.translatable("menu.soulbound.ritual_cost",menu.info.get(5),menu.info.get(6)),12,169,0xffffcc80,false);
    }
    private void offerings(GuiGraphics g){
        g.drawString(font,Component.translatable("menu.soulbound.forge_materials"),12,96,0xffffffff,false);
        for(int i=0;i<menu.materials.size();i++)g.drawString(font,menu.materials.get(i),12,110+i*13,0xffffffff,false);
    }
}
