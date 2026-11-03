package br.com.soulbound.client;

import br.com.soulbound.network.*;
import br.com.soulbound.companion.identity.CompanionStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.locale.Language;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** A read-only GUI layer. All numbers come from the owner-only server snapshot. */
public final class CompanionStatusHud {
    private static final Identifier HEART=Identifier.withDefaultNamespace("hud/heart/full");
    private static CompanionHudData data=CompanionHudData.EMPTY;
    private static Object connection;
    private static int bossBottom;
    private CompanionStatusHud() {}
    public static void registerPayload(RegisterClientPayloadHandlersEvent event){event.register(CompanionHudPayload.TYPE,(payload,context)->{
        data=payload.data();connection=Minecraft.getInstance().getConnection();
    });}
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){data=CompanionHudData.EMPTY;connection=null;}
    public static void registerLayer(RegisterGuiLayersEvent event){event.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.BOSS_OVERLAY,Identifier.fromNamespaceAndPath("soulbound","companion_status"),CompanionStatusHud::render);}
    public static void beforeLayer(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event){if(event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.BOSS_OVERLAY))bossBottom=0;}
    public static void boss(net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.BossEventProgress event){if(!event.isCanceled() && event.getX()<186)bossBottom=Math.max(bossBottom,event.getY()+event.getIncrement());}
    private static String hp(float amount){return amount==(int)amount?Integer.toString((int)amount):String.format(java.util.Locale.ROOT,"%.1f",amount);}
    private static void bar(GuiGraphics g,int x,int y,int width,float fraction,int color){g.fill(x,y,x+width,y+5,0xff101923);int fill=Math.round(width*fraction);if(fill>0)g.fill(x,y,x+fill,y+5,color);}
    private static void render(GuiGraphics g,net.minecraft.client.DeltaTracker delta){
        var mc=Minecraft.getInstance();var d=data;
        if(mc.player==null || mc.level==null || mc.screen!=null || mc.options.hideGui || mc.options.keyPlayerList.isDown() || mc.getDebugOverlay().showDebugScreen()
                || !d.present() || connection!=mc.getConnection() || !d.owner().equals(mc.player.getUUID()))return;
        int panelHeight=d.form().equals("GOD")?98:d.godCooldownSeconds()>0?78:62;
        int x=6,y=Math.max(6,bossBottom+4),width=Math.min(180,g.guiWidth()-12);if(width<140 || g.guiHeight()<140 || y+panelHeight>g.guiHeight()-65)return;
        g.fill(x,y,x+width,y+panelHeight,0xce10243d);g.fill(x,y,x+width,y+1,0xff65bedf);
        var font=mc.font;var level=Component.translatable(d.level()>=30?"hud.soulbound.level_max":"hud.soulbound.level",d.level());int levelWidth=font.width(level);
        var title=Component.translatable("hud.soulbound.name_form",d.name(),Component.translatable("stage.soulbound."+d.form().toLowerCase(java.util.Locale.ROOT)));
        g.drawString(font,Language.getInstance().getVisualOrder(font.substrByWidth(title,width-levelWidth-18)),x+6,y+6,0xffd7f5ff,false);g.drawString(font,level,x+width-levelWidth-6,y+6,0xffeac773,false);
        g.blitSprite(RenderPipelines.GUI_TEXTURED,HEART,x+6,y+19,9,9);
        Component health;
        if(d.status()==CompanionStatus.DEAD || d.healthKnown() && d.health()<=0)health=Component.translatable("hud.soulbound.dead");
        else if(d.healthKnown())health=Component.translatable("hud.soulbound.health",hp(d.health()),hp(d.maxHealth()));
        else health=Component.translatable("hud.soulbound.state."+d.status().name().toLowerCase(java.util.Locale.ROOT));
        g.drawString(font,Language.getInstance().getVisualOrder(font.substrByWidth(health,width-28)),x+20,y+19,0xfff1dce1,false);bar(g,x+6,y+30,width-12,d.healthFraction(),0xffcd4155);
        var xp=d.level()>=30?Component.translatable("hud.soulbound.xp_max"):Component.translatable("hud.soulbound.xp",d.xp(),d.required());
        g.drawString(font,xp,x+6,y+40,0xff98e8f5,false);bar(g,x+6,y+51,width-12,d.xpFraction(),0xff3ac9db);
        if(d.form().equals("GOD")){g.drawString(font,Component.translatable("hud.soulbound.barrier",hp(d.shield()),hp(d.maxShield())),x+6,y+62,0xffa2edff,false);bar(g,x+6,y+73,width-12,br.com.soulbound.network.CompanionHudData.fraction(d.shield(),d.maxShield()),0xff79dcff);g.drawString(font,Component.translatable("hud.soulbound.god_time",d.godSeconds(),d.godCooldownSeconds()),x+6,y+84,0xffffd787,false);}
        else if(d.godCooldownSeconds()>0)g.drawString(font,Component.translatable("hud.soulbound.god_cooldown",d.godCooldownSeconds()),x+6,y+62,0xffffd787,false);
    }
}
