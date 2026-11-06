package br.com.soulbound.client;

import br.com.soulbound.network.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.lwjgl.glfw.GLFW;

/** Client selection is only an intent; all authorization and damage policy stay on the server. */
public final class TargetControls {
    private static final KeyMapping.Category CATEGORY=new KeyMapping.Category(Identifier.fromNamespaceAndPath("soulbound","target_commands"));
    private static final KeyMapping TARGET=new KeyMapping("key.soulbound.target",GLFW.GLFW_KEY_G,CATEGORY);
    private static final KeyMapping CANCEL=new KeyMapping("key.soulbound.cancel_target",GLFW.GLFW_KEY_V,CATEGORY);
    private static TargetStatusPayload status=new TargetStatusPayload(-1,Component.empty(),24);
    private static Object connection;
    private TargetControls(){}
    public static Component shortcut(){return TARGET.getTranslatedKeyMessage();}
    public static Component cancelShortcut(){return CANCEL.getTranslatedKeyMessage();}
    public static Component selectedName(){return status.entityId()<0?Component.translatable("menu.soulbound.none"):status.name();}
    public static void register(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e){e.registerCategory(CATEGORY);e.register(TARGET);e.register(CANCEL);}
    public static void payloads(net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent e){
        e.register(TargetStatusPayload.TYPE,(p,c)->{status=p;connection=Minecraft.getInstance().getConnection();});
        e.register(GrimoirePayload.TYPE,(p,c)->{var mc=Minecraft.getInstance();if(mc.player!=null && mc.player.containerMenu instanceof br.com.soulbound.menu.SoulCommandMenu menu && menu.containerId==p.menu())menu.view=p;});
    }
    public static void layer(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e){e.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR,Identifier.fromNamespaceAndPath("soulbound","target_reticle"),TargetControls::render);}
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(mc.getConnection()!=connection){status=new TargetStatusPayload(-1,Component.empty(),24);connection=mc.getConnection();}
        while(CANCEL.consumeClick())if(mc.screen==null && mc.player!=null){if(JuniorControls.conflicts(CANCEL))mc.player.displayClientMessage(Component.translatable("target.soulbound.key_conflict"),true);else net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new TargetCommandPayload(-1));}
        while(TARGET.consumeClick()){
            if(mc.screen!=null || mc.player==null || mc.level==null)continue;
            if(JuniorControls.conflicts(TARGET) || TARGET.getKey().equals(CANCEL.getKey())){mc.player.displayClientMessage(Component.translatable("target.soulbound.key_conflict"),true);continue;}
            var eye=mc.player.getEyePosition();double range=status.range();var delta=mc.player.getViewVector(1).scale(range);
            var hit=ProjectileUtil.getEntityHitResult(mc.player,eye,eye.add(delta),mc.player.getBoundingBox().expandTowards(delta).inflate(1),t->t instanceof LivingEntity && t.isPickable(),range*range);
            if(hit==null && status.entityId()<0){mc.player.displayClientMessage(Component.translatable("target.soulbound.error.invalid"),true);continue;}
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new TargetCommandPayload(hit==null?-1:hit.getEntity().getId()));
        }
    }
    private static void render(GuiGraphics g,net.minecraft.client.DeltaTracker delta){
        var mc=Minecraft.getInstance();if(status.entityId()<0 || mc.player==null || mc.level==null || mc.screen!=null || mc.options.hideGui || connection!=mc.getConnection())return;
        int x=g.guiWidth()/2,y=g.guiHeight()/2;
        g.blit(RenderPipelines.GUI_TEXTURED,GrimoireTextures.id("target_reticle"),x-12,y-12,0,0,24,24,32,32,32,32);
        g.drawCenteredString(mc.font,net.minecraft.locale.Language.getInstance().getVisualOrder(mc.font.substrByWidth(status.name(),160)),x,y+17,0xffe8cd8c);
    }
}
