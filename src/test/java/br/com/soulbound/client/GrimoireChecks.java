package br.com.soulbound.client;

/** Layout geometry and resource checks; never substitutes for real GUI/scale inspection. */
public final class GrimoireChecks {
    private static int count;
    private static void check(boolean b,String message){count++;if(!b)throw new AssertionError(message);}
    public static void run()throws Exception{
        count=0;for(int[] viewport:new int[][]{{320,180},{320,240},{426,240},{640,360},{960,540},{854,480}}){
            var l=GrimoireScreen.layout(viewport[0],viewport[1]);check(l.width()<=viewport[0] && l.height()<=viewport[1],"Panel fits viewport");
            check(28+7*l.row()-2<=l.height()-24,"Seven tabs clear footer");
            check(l.width()-l.side()-24>120,"Content buttons have positive usable width");
            check(l.height()-92>34 && l.height()-92+2*20+18<l.height()-24,"Actions clear text/header/footer");
            var seen=new java.util.HashSet<String>();for(int i=0;i<45;i++){
                var pos=br.com.soulbound.menu.CompanionInventoryMenu.inventoryPosition(i,l.side()+12,Math.min(50,l.height()-132));
                check(pos.x()>=l.side()+12 && pos.x()+16<=l.width()-8 && pos.y()>=34 && pos.y()+16<=l.height()-28,"Integrated slot within content bounds");
                for(int y=pos.y();y<pos.y()+16;y++)for(int x=pos.x();x<pos.x()+16;x++)if(!seen.add(x+","+y))throw new AssertionError("Overlapping native slots");
            }
        }
        for(String name:new String[]{"content_panel","sidebar_panel","button_normal","button_hover","button_selected","target_reticle","icon_paw","icon_abilities","icon_overview","icon_inventory","icon_talents","icon_stats","icon_commands","icon_bond"}){
            try(var stream=GrimoireChecks.class.getResourceAsStream("/assets/soulbound/textures/gui/grimoire/"+name+".png")){check(stream!=null,"Missing GUI texture "+name);check(javax.imageio.ImageIO.read(stream)!=null,"Valid PNG");}
        }
        for(String language:new String[]{"pt_br","en_us"})try(var stream=GrimoireChecks.class.getResourceAsStream("/assets/soulbound/lang/"+language+".json")){
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(String tab:new String[]{"overview","inventory","talents","abilities","stats","commands","bond"})check(json.has("grimoire.soulbound.tab."+tab),"Localized tab");
            check(json.has("key.soulbound.target") && json.has("target.soulbound.error.pvp"),"Localized controls/errors");
        }
        check(br.com.soulbound.item.SoulItemForm.forStatus(br.com.soulbound.companion.identity.CompanionStatus.ACTIVE).model().toString().equals("soulbound:soul_grimoire"),"Canonical active projection is grimoire");
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{
            var packet=new net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket(31,br.com.soulbound.menu.SoulCommandMenu.TALENT_FIRST+23);
            net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket.STREAM_CODEC.encode(buffer,packet);
            check(packet.equals(net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket.STREAM_CODEC.decode(buffer)),"Integrated talent IDs survive actual native button packet codec");
        }finally{buffer.release();}
        System.out.println("Grimoire resource/layout checks passed: "+count);
    }
}
