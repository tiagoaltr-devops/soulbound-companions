package br.com.soulbound.client;

import br.com.soulbound.menu.TalentMenu;
import br.com.soulbound.companion.progression.TalentDefinitions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact branch tabs and one talent card per page, usable at a 320x240 GUI viewport. */
public final class TalentScreen extends AbstractContainerScreen<TalentMenu> {
    private int selected;private Button invest;
    private final Button[] tabs=new Button[5];
    public TalentScreen(TalentMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=320;imageHeight=224;}
    @Override protected void init(){super.init();
        for(int i=0;i<5;i++){final int branch=i;tabs[i]=addRenderableWidget(Button.builder(Component.translatable(i==0?"menu.soulbound.main_short":"menu.soulbound.branch."+i),b->selectBranch(branch)).bounds(leftPos+10+i*61,topPos+29,57,18).build());}
        invest=addRenderableWidget(Button.builder(Component.translatable("menu.soulbound.invest"),b->{if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,selected);}).bounds(leftPos+10,topPos+170,300,20).build());
        addRenderableWidget(Button.builder(Component.literal("<"),b->move(-1)).bounds(leftPos+10,topPos+196,70,18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(leftPos+118,topPos+196,84,18).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->move(1)).bounds(leftPos+240,topPos+196,70,18).build());update();
    }
    private void selectBranch(int index){for(int i=0;i<TalentDefinitions.ALL.size();i++)if(TalentDefinitions.ALL.get(i).branch().ordinal()==index){selected=i;break;}update();}
    private void move(int direction){var branch=TalentDefinitions.ALL.get(selected).branch();do{selected=Math.floorMod(selected+direction,TalentDefinitions.ALL.size());}while(TalentDefinitions.ALL.get(selected).branch()!=branch);update();}
    private void update(){if(invest!=null)invest.active=menu.reason(selected)==0;if(tabs[2]!=null){tabs[2].setMessage(Component.translatable(menu.data.get(5)==0?"menu.soulbound.junior_locked":"menu.soulbound.branch.2"));tabs[4].setMessage(Component.translatable(menu.godUnlocked()?"menu.soulbound.branch.4":"menu.soulbound.god_locked"));tabs[3].setMessage(Component.translatable(menu.plenoUnlocked()?"menu.soulbound.branch.3":"menu.soulbound.pleno_locked"));}}
    @Override protected void containerTick(){super.containerTick();update();}
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y){g.fill(leftPos,topPos,leftPos+320,topPos+224,0xff142c40);}
    private void wrapped(GuiGraphics g,Component text,int y,int lines,int color){int n=0;for(var line:font.split(text,300)){if(n++>=lines)break;g.drawString(font,line,10,y,color,false);y+=10;}}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        var t=TalentDefinitions.ALL.get(selected);
        g.drawString(font,Component.translatable("menu.soulbound.talent_header",menu.data.get(0),menu.data.get(2)),10,9,0xffa7e8ff,false);
        g.drawString(font,Component.translatable(t.nameKey()),10,55,0xffffcc80,false);
        wrapped(g,Component.translatable(t.nameKey()+".description"),73,3,0xffffffff);
        g.drawString(font,Component.translatable("menu.soulbound.talent_rank",menu.rank(selected),t.maximum()),10,107,0xffffffff,false);
        var previous=t.previous()==null?Component.translatable("menu.soulbound.none"):Component.translatable(TalentDefinitions.find(t.previous()).nameKey());
        wrapped(g,Component.translatable("menu.soulbound.talent_requires",t.minimumLevel(),t.branchPoints(),previous,Component.translatable("menu.soulbound.branch."+t.branch().ordinal())),122,3,0xffcce6ef);
        wrapped(g,Component.translatable("menu.soulbound.talent_reason."+menu.reason(selected)),155,1,menu.reason(selected)==0?0xff70e4a0:0xffffa080);
    }
}
