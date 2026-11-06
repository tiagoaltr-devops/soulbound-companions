package br.com.soulbound.client;

import java.util.*;
import br.com.soulbound.menu.SoulCommandMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.FormattedCharSequence;

/** Responsive slotless hub. Existing server menus own all inventory/talent actions. */
public final class GrimoireScreen extends AbstractContainerScreen<SoulCommandMenu> {
    public record Layout(int width,int height,int side,int row) {}
    public static Layout layout(int width,int height){int w=Math.max(1,Math.min(500,width-8)),h=Math.max(1,Math.min(290,height-8));return new Layout(w,h,Math.min(112,Math.max(80,w/4)),Math.max(12,Math.min(26,(h-56)/7)));}
    private static final String[] TABS={"overview","inventory","talents","abilities","stats","commands","bond"};
    private int tab,page,side,content,contentWidth,actionTop,branch,selectedTalent;private Button invest;private Button previous,next;
    private final List<Button> commands=new ArrayList<>();private int pages=1;
    public GrimoireScreen(SoulCommandMenu menu,Inventory inv,Component title){super(menu,inv,title);}
    private static Component tr(String key){return Component.translatable("grimoire.soulbound."+key);}
    @Override protected void init(){
        var layout=layout(width,height);imageWidth=layout.width();imageHeight=layout.height();super.init();isQuickCrafting=false;quickCraftSlots.clear();hoveredSlot=null;commands.clear();invest=null;tab=menu.tab();
        side=layout.side();content=leftPos+side+12;contentWidth=Math.max(30,imageWidth-side-24);actionTop=topPos+imageHeight-92;
        int row=layout.row();
        for(int i=0;i<TABS.length;i++){int selected=i;var b=new GrimoireButton(leftPos+6,topPos+28+i*row,side-6,row-2,tr("tab."+TABS[i]),v->selectTab(selected));b.selected=tab==i;b.icon="icon_"+TABS[i];b.setTooltip(Tooltip.create(tr("tab."+TABS[i]).copy().append(" — ").append(Component.translatable("grimoire.soulbound.shortcut",i+1))));addRenderableWidget(b);}
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(leftPos+6,topPos+imageHeight-24,side-6,18).build());
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->{page=Math.max(0,page-1);}).bounds(content,topPos+imageHeight-24,28,18).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->{page=Math.min(pages-1,page+1);}).bounds(leftPos+imageWidth-40,topPos+imageHeight-24,28,18).build());
        menu.layoutInventory(side+12,Math.min(50,imageHeight-132));
        if(tab==2)initTalents();
        if(tab==3)action("ultimate",SoulCommandMenu.ULTIMATE,0,0);
        if(tab==5){action("follow",SoulCommandMenu.FOLLOW,0,0);action("stay",SoulCommandMenu.STAY,0,1);action("passive",SoulCommandMenu.PASSIVE,1,0);action("recall",SoulCommandMenu.RECALL,1,1);action("cancel_target",SoulCommandMenu.CANCEL_TARGET,2,0);action("resonate",SoulCommandMenu.LOCATE_RUIN,2,1);}
        if(tab==0){action("baby",SoulCommandMenu.BABY_FORM,0,0);action("junior",SoulCommandMenu.JUNIOR_FORM,0,1);action("pleno",SoulCommandMenu.PLENO_FORM,1,0);action("god",SoulCommandMenu.GOD,1,1);}
        update();
    }
    private void selectTab(int value){
        isQuickCrafting=false;quickCraftSlots.clear();hoveredSlot=null;page=0;tab=value;menu.previewTab(value);
        if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,SoulCommandMenu.TAB_FIRST+value);
        rebuildWidgets();
    }
    private void initTalents(){
        var definitions=br.com.soulbound.companion.progression.TalentDefinitions.ALL;
        if(definitions.get(selectedTalent).branch().ordinal()!=branch)for(int i=0;i<definitions.size();i++)if(definitions.get(i).branch().ordinal()==branch){selectedTalent=i;break;}
        int buttonWidth=(contentWidth-4)/5;
        for(int i=0;i<5;i++){int chosen=i;var b=new GrimoireButton(content+i*(buttonWidth+1),topPos+29,buttonWidth,18,Component.translatable("menu.soulbound.branch."+i),v->{branch=chosen;page=0;rebuildWidgets();});b.selected=branch==i;b.setTooltip(Tooltip.create(Component.translatable("menu.soulbound.branch."+i)));addRenderableWidget(b);}
        var nodes=branchNodes();for(int i=0;i<nodes.size();i++){int definition=nodes.get(i);addRenderableWidget(new TalentNode(definition,nodeX(i),nodeY(i)));}
        int detailX=content+contentWidth/2+5,detailWidth=contentWidth-contentWidth/2-5;
        invest=addRenderableWidget(new GrimoireButton(detailX,topPos+imageHeight-46,detailWidth,18,Component.translatable("menu.soulbound.invest"),v->{if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,SoulCommandMenu.TALENT_FIRST+selectedTalent);}));
        invest.setTooltip(Tooltip.create(tr("point_cost")));
    }
    private List<Integer> branchNodes(){var list=new ArrayList<Integer>();var defs=br.com.soulbound.companion.progression.TalentDefinitions.ALL;for(int i=0;i<defs.size();i++)if(defs.get(i).branch().ordinal()==branch)list.add(i);return list;}
    private int nodeX(int index){return content+8+index%2*(contentWidth/2-38);}
    private int nodeY(int index){return topPos+64+index/2*Math.min(38,Math.max(22,(imageHeight-100)/3));}
    private final class TalentNode extends Button {
        private final int definition;
        TalentNode(int definition,int x,int y){super(x,y,24,18,Component.translatable(br.com.soulbound.companion.progression.TalentDefinitions.ALL.get(definition).nameKey()),v->{selectedTalent=definition;page=0;update();},DEFAULT_NARRATION);this.definition=definition;var t=br.com.soulbound.companion.progression.TalentDefinitions.ALL.get(definition);setTooltip(Tooltip.create(Component.translatable(t.nameKey()).append("\n").append(Component.translatable(t.nameKey()+".description"))));}
        @Override protected void renderContents(GuiGraphics g,int x,int y,float partial){int color=definition==selectedTalent?0xffeccb79:menu.talentRank(definition)>0?0xffad8748:menu.talentReason(definition)==0?0xff73dbe3:0xff675277;g.fill(getX()-1,getY()-1,getX()+25,getY()+19,color);g.fill(getX()+1,getY()+1,getX()+23,getY()+17,0xff201a2c);g.drawCenteredString(font,menu.talentRank(definition)+"/"+br.com.soulbound.companion.progression.TalentDefinitions.ALL.get(definition).maximum(),getX()+12,getY()+5,color);}
    }
    private void action(String name,int id,int row,int col){
        int w=(contentWidth-4)/2;var b=new GrimoireButton(content+col*(w+4),actionTop+row*20,w,18,tr(name),v->{if(minecraft!=null && minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);});
        b.setTooltip(Tooltip.create(tr(name).copy().append(" — ").append(tr("hint."+name))));commands.add(addRenderableWidget(b));
    }
    private void update(){
        if(invest!=null)invest.active=menu.talentReason(selectedTalent)==0;
        if(tab==3 && !commands.isEmpty())commands.getFirst().active=menu.ultimateState()==0;
        if(previous!=null){previous.active=page>0;next.active=page+1<pages;}
    }
    @Override protected void containerTick(){super.containerTick();if(tab!=menu.tab()){tab=menu.tab();page=0;rebuildWidgets();}update();}
    private List<Component> lines(){
        var v=menu.view;if(v==null)return List.of(tr("loading"));
        return switch(tab){case 0->v.overview();case 1->List.of(tr("inventory_info"),tr(menu.canInventory()?"inventory_near":"inventory_far"));case 2->List.of(tr("talents_info"),Component.translatable("grimoire.soulbound.points",menu.talentPoints()));case 3->v.abilities();case 4->v.stats();case 5->List.of(Component.translatable("grimoire.soulbound.mode",Component.translatable("behavior.soulbound."+menu.mode().name().toLowerCase(Locale.ROOT))),Component.translatable("grimoire.soulbound.focus",TargetControls.selectedName()),Component.translatable("grimoire.soulbound.target_help",TargetControls.shortcut(),TargetControls.cancelShortcut()));case 6->v.bond();default->List.of();};
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        GrimoireTextures.panel(g,"sidebar_panel",leftPos,topPos,side+6,imageHeight,112,224,6);
        GrimoireTextures.panel(g,"content_panel",leftPos+side+6,topPos,imageWidth-side-6,imageHeight,256,224,6);
        g.drawString(font,tr("title"),leftPos+8,topPos+9,0xffe3c780,false);
        g.drawString(font,tr("tab."+TABS[tab]),content,topPos+12,0xff97e8f0,false);
        if(tab==0)g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,GrimoireTextures.id("icon_paw"),leftPos+imageWidth-30,topPos+8,0,0,18,18,24,24,24,24);
        if(tab==1){renderInventory(g);return;}if(tab==2){renderTalents(g);return;}
        var wrapped=new ArrayList<FormattedCharSequence>();for(var line:lines()){wrapped.addAll(font.split(line,contentWidth));wrapped.add(FormattedCharSequence.EMPTY);}
        int limit=Math.max(1,((commands.isEmpty()?topPos+imageHeight-34:actionTop-5)-(topPos+34))/10);pages=Math.max(1,(wrapped.size()+limit-1)/limit);page=Math.min(page,pages-1);
        for(int i=page*limit,y=topPos+34;i<Math.min(wrapped.size(),(page+1)*limit);i++,y+=10)g.drawString(font,wrapped.get(i),content,y,0xffe2dbe9,false);
        g.drawCenteredString(font,Component.literal((page+1)+" / "+pages),content+contentWidth/2,topPos+imageHeight-19,0xffb5a8cb);update();
    }
    private void renderInventory(GuiGraphics g){
        pages=1;page=0;update();
        if(!menu.canInventory()){int y=topPos+34;for(var line:font.split(tr("inventory_far"),contentWidth)){g.drawString(font,line,content,y,0xffe2dbe9,false);y+=10;}return;}
        int y=topPos+Math.min(50,imageHeight-132);
        g.drawString(font,tr("gear_label"),content,y-10,0xffb6dce4,false);g.drawString(font,Component.translatable("menu.soulbound.food"),content+94,y-10,0xffb6dce4,false);
        g.drawString(font,Component.translatable("container.inventory"),content,y+19,0xffb6dce4,false);
        for(var slot:menu.slots)if(slot.isActive())GrimoireTextures.panel(g,"button_normal",leftPos+slot.x-1,topPos+slot.y-1,18,18,96,24,3);
        if(contentWidth>280){int tx=content+180,ty=y;for(var line:font.split(tr("inventory_tip"),contentWidth-180)){g.drawString(font,line,tx,ty,0xffb6a9cb,false);ty+=10;}}
    }
    private void renderTalents(GuiGraphics g){
        var defs=br.com.soulbound.companion.progression.TalentDefinitions.ALL;var nodes=branchNodes();
        g.drawString(font,Component.translatable("menu.soulbound.talent_header",menu.companionLevel(),menu.talentPoints()),content,topPos+50,0xffe3c780,false);
        for(int i=0;i<nodes.size();i++){var t=defs.get(nodes.get(i));if(t.previous()==null)continue;for(int j=0;j<nodes.size();j++)if(defs.get(nodes.get(j)).id().equals(t.previous())){int ax=nodeX(j)+12,ay=nodeY(j)+9,bx=nodeX(i)+12,by=nodeY(i)+9,color=menu.talentRank(nodes.get(j))>0?0xffad8748:0xff51405e;int mid=(ay+by)/2;g.fill(ax,Math.min(ay,mid),ax+1,Math.max(ay,mid)+1,color);g.fill(Math.min(ax,bx),mid,Math.max(ax,bx)+1,mid+1,color);g.fill(bx,Math.min(mid,by),bx+1,Math.max(mid,by)+1,color);}}
        int dx=content+contentWidth/2+5,dw=contentWidth-contentWidth/2-5;var t=defs.get(selectedTalent);
        var previous=t.previous()==null?Component.translatable("menu.soulbound.none"):Component.translatable(br.com.soulbound.companion.progression.TalentDefinitions.find(t.previous()).nameKey());
        var lines=List.of(Component.translatable(t.nameKey()),Component.translatable("menu.soulbound.talent_rank",menu.talentRank(selectedTalent),t.maximum()),Component.translatable(t.nameKey()+".description"),Component.translatable("menu.soulbound.talent_requires",t.minimumLevel(),t.branchPoints(),previous,Component.translatable("menu.soulbound.branch."+t.branch().ordinal())),tr("point_cost"),Component.translatable("menu.soulbound.talent_reason."+menu.talentReason(selectedTalent)));
        var wrapped=new ArrayList<FormattedCharSequence>();for(var line:lines){wrapped.addAll(font.split(line,dw));wrapped.add(FormattedCharSequence.EMPTY);}
        int limit=Math.max(1,(imageHeight-110)/10);pages=Math.max(1,(wrapped.size()+limit-1)/limit);page=Math.min(page,pages-1);
        for(int i=page*limit,y=topPos+64;i<Math.min(wrapped.size(),(page+1)*limit);i++,y+=10)g.drawString(font,wrapped.get(i),dx,y,0xffe2dbe9,false);
        g.drawCenteredString(font,Component.literal((page+1)+" / "+pages),content+contentWidth/2,topPos+imageHeight-19,0xffb5a8cb);update();
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);if(tab==1 && menu.getCarried().isEmpty() && hoveredSlot!=null && hoveredSlot.index<9 && !hoveredSlot.hasItem())g.setTooltipForNextFrame(font,Component.translatable(hoveredSlot.index<5?"menu.soulbound.gear."+hoveredSlot.index:"menu.soulbound.food"),mx,my);}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public boolean keyPressed(KeyEvent event){
        if(tab!=1 && event.key()>=49 && event.key()<=55){selectTab(event.key()-49);return true;}
        if(event.key()==263){page=Math.max(0,page-1);return true;}if(event.key()==262){page=Math.min(pages-1,page+1);return true;}
        if(JuniorControls.fromMenu(event))return true;
        return super.keyPressed(event);
    }
}
