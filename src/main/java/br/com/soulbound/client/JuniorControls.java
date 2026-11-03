package br.com.soulbound.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class JuniorControls {
    private static final KeyMapping.Category CATEGORY=new KeyMapping.Category(Identifier.fromNamespaceAndPath("soulbound","companions"));
    private static final KeyMapping ULTIMATE=new KeyMapping("key.soulbound.ultimate",GLFW.GLFW_KEY_R,CATEGORY);
    public static net.minecraft.network.chat.Component shortcut(){return ULTIMATE.getTranslatedKeyMessage();}
    public static boolean fromMenu(net.minecraft.client.input.KeyEvent event){
        if(!ULTIMATE.matches(event))return false;
        if(Minecraft.getInstance().getConnection()!=null)net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(br.com.soulbound.network.JuniorUltimatePayload.INSTANCE);
        return true;
    }
    public static void register(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event){event.registerCategory(CATEGORY);event.register(ULTIMATE);}
    public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        while(ULTIMATE.consumeClick())if(Minecraft.getInstance().screen==null && Minecraft.getInstance().player!=null)
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(br.com.soulbound.network.JuniorUltimatePayload.INSTANCE);
    }
}
