package br.com.soulbound.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Nine-slice with fixed corner sizes and separately stretched interior/edges. */
final class GrimoireTextures {
    private static final java.util.Map<String,Identifier> CACHE=new java.util.HashMap<>();
    private GrimoireTextures(){}
    static Identifier id(String name){return CACHE.computeIfAbsent(name,k->Identifier.fromNamespaceAndPath("soulbound","textures/gui/grimoire/"+k+".png"));}
    static void panel(GuiGraphics g,String name,int x,int y,int w,int h,int tw,int th,int border){
        int b=Math.min(border,Math.min(w/2,h/2));if(b<=0)return;
        int[] dx={0,b,w-b,w},dy={0,b,h-b,h},sx={0,b,tw-b,tw},sy={0,b,th-b,th};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++){
            int dw=dx[col+1]-dx[col],dh=dy[row+1]-dy[row];if(dw>0 && dh>0)g.blit(RenderPipelines.GUI_TEXTURED,id(name),x+dx[col],y+dy[row],sx[col],sy[row],dw,dh,sx[col+1]-sx[col],sy[row+1]-sy[row],tw,th);
        }
    }
}
