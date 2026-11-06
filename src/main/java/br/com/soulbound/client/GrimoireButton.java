package br.com.soulbound.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class GrimoireButton extends Button {
    boolean selected;String icon;
    GrimoireButton(int x,int y,int width,int height,Component text,OnPress press){super(x,y,width,height,text,press,DEFAULT_NARRATION);}
    @Override public void renderContents(GuiGraphics g,int mx,int my,float partial){
        GrimoireTextures.panel(g,selected?"button_selected":isHoveredOrFocused()?"button_hover":"button_normal",getX(),getY(),getWidth(),getHeight(),96,24,4);
        int inset=icon==null?0:18;if(icon!=null)g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,GrimoireTextures.id(icon),getX()+3,getY()+(getHeight()-14)/2,0,0,14,14,24,24,24,24);
        var font=Minecraft.getInstance().font;var text=net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(getMessage(),getWidth()-6-inset));
        g.drawString(font,text,getX()+inset+(getWidth()-inset-font.width(text))/2,getY()+(getHeight()-8)/2,active?0xffe7d3a0:0xff766e80,false);
    }
}
