package br.com.soulbound.ritual;

import com.mojang.serialization.JsonOps;

public final class AltarChecks {
    private static int count;
    private static void check(boolean b){if(!b)throw new AssertionError("Altar check "+count);count++;}
    public static void main(String[] args) throws Exception {
        check(AltarPolicy.symbols(0)==3);check(AltarPolicy.symbols(100000)==7);
        check(AltarPolicy.guardians(100000)==12);check(AltarPolicy.bondTicks(100000)==500);
        double[] expected={.01,.2,.4,.6,.8,.8};for(int i=0;i<expected.length;i++)check(AltarPolicy.failure(i)==expected[i]);
        for(int i=0;i<4;i++){var r=AltarPolicy.rotate(0,-2,i);check(Math.abs(r[0])+Math.abs(r[1])==2);}
        check(AltarPolicy.accepts("awakening","BABY","ACTIVE",false));
        check(!AltarPolicy.accepts("awakening","BABY","MISSING",false));
        check(!AltarPolicy.accepts("divine","PLENO","ACTIVE",true));
        check(AltarPolicy.accepts("restoration","PLENO","DEAD",true));
        check(AltarPolicy.resolveForge(true,0,0,0,1,1).state().equals("ACTIVE"));
        check(AltarPolicy.resolveForge(false,0,.01,0,.01,.25).state().equals("ACTIVE"));
        check(AltarPolicy.resolveForge(false,1,.1,.25,.2,.25).state().equals("INACTIVE"));
        check(AltarPolicy.resolveForge(false,1,.1,.249,.2,.25).state().equals("DESTROYED"));
        check(AltarPolicy.resolveForge(false,4,.9,.1,.8,.25).attempts()==5);
        for(int turn=0;turn<4;turn++){
            var layout=new java.util.HashMap<String,Integer>();
            for(var v:new int[][]{{0,-2,0},{-2,0,0},{2,0,0},{0,2,1},{0,-1,2},{-1,0,2},{1,0,2},{0,1,2}}){var xy=AltarPolicy.rotate(v[0],v[1],turn);layout.put(xy[0]+","+xy[1],v[2]);}
            check(AltarPolicy.rotation((x,z,k)->layout.getOrDefault(x+","+z,-1)==k)==turn);
            layout.remove("0,-1");check(AltarPolicy.rotation((x,z,k)->layout.getOrDefault(x+","+z,-1)==k)==-1);
        }
        for(String kind:new String[]{"awakening","ascension","divine","restoration"})for(String status:new String[]{"READY","SUMMONING","MISSING"})
            check(!AltarPolicy.accepts(kind,"BABY",status,false));
        var d=new AltarSavedData();var r=new AltarSavedData.Ruin();r.id="ruin";r.dimension="minecraft:overworld";r.village="1,2";
        r.completed.add("player");r.cooldowns.put("other",36000L);r.completions=3;d.state.ruins.put(r.id,r);
        var t=AltarTokens.issue(d,"player","awakening");t.spent=true;
        t.applicationPending=true;t.applicationOwner="owner";t.applicationBond="bond";
        var forge=new AltarSavedData.Forge();forge.id="forge";forge.owner="owner";forge.attempts=4;forge.state="INACTIVE";forge.dimension="minecraft:overworld";forge.placed=false;d.state.forges.put(forge.id,forge);
        var ritual=new AltarSavedData.Ritual();ritual.id="operation";ritual.forge=forge.id;ritual.player="player";ritual.kind="awakening";ritual.core="raw core snapshot";ritual.offerings.put("altar","offering snapshot");ritual.end=12000;d.state.rituals.put(ritual.id,ritual);
        var transfer=new AltarSavedData.Transfer();transfer.id="transfer";transfer.player="player";transfer.target="altar";transfer.stack="snapshot";transfer.deposit=true;d.state.transfers.put(transfer.id,transfer);
        var copy=AltarSavedData.CODEC.parse(JsonOps.INSTANCE,AltarSavedData.CODEC.encodeStart(JsonOps.INSTANCE,d).getOrThrow()).getOrThrow();
        check(copy.state.ruins.get("ruin").completed.contains("player"));check(copy.state.ruins.get("ruin").cooldowns.get("other")==36000L);
        check(copy.state.tokens.get(t.id).spent);check(copy.state.ruins.get("ruin").completions==3);
        check(copy.state.tokens.get(t.id).applicationPending);check(copy.state.tokens.get(t.id).applicationBond.equals("bond"));
        check(copy.state.forges.get("forge").attempts==4);check(copy.state.forges.get("forge").owner.equals("owner"));
        check(copy.state.rituals.get("operation").offerings.get("altar").equals("offering snapshot"));
        check(copy.state.transfers.get("transfer").deposit);
        var root=new net.minecraft.nbt.CompoundTag();root.put("data",AltarSavedData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,d).getOrThrow());
        var temp=java.nio.file.Files.createTempFile("soulbound-altar-check-",".dat");temp.toFile().deleteOnExit();
        net.minecraft.nbt.NbtIo.writeCompressed(root,temp);
        var disk=AltarSavedData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,net.minecraft.nbt.NbtIo.readCompressed(temp,net.minecraft.nbt.NbtAccounter.unlimitedHeap()).get("data")).getOrThrow();
        check(disk.state.rituals.get("operation").core.equals("raw core snapshot"));check(disk.state.tokens.get(t.id).spent);
        var identity=br.com.soulbound.companion.identity.SoulboundRecord.create(java.util.UUID.randomUUID(),"Owner").withState(br.com.soulbound.companion.identity.CompanionStatus.ACTIVE,false,"STAY");
        for(String stage:new String[]{"JUNIOR","PLENO"}){
            var evolved=identity.withEvolution(stage,73.5,18000);
            check(evolved.matches(identity.ownerUuid(),identity.bondUuid(),identity.companionUuid()));
            check(evolved.behaviorMode().equals("STAY") && evolved.recall().equals(identity.recall()));
            check(br.com.soulbound.companion.SoulStaffAccess.allows(evolved,evolved.ownerUuid(),evolved.ownerUuid(),evolved.bondUuid()));
        }
        for(String id:new String[]{"ancestral_spark","reactivation_essence","draconic_essence","awakening_core","ascension_core","divine_core","restoration_core","charged_core"}){
            try(var stream=AltarChecks.class.getResourceAsStream("/assets/soulbound/textures/item/"+id+".png")){
                check(stream!=null);var image=javax.imageio.ImageIO.read(stream);check(image!=null && image.getWidth()==16 && image.getHeight()==16);
            }
        }
        for(String kind:new String[]{"activation","awakening","ascension","divine","restoration"}){
            try(var stream=AltarChecks.class.getResourceAsStream("/data/soulbound/soul_ritual/"+kind+".json")){
                check(stream!=null);var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();check(json.getAsJsonArray("offerings").size()==3);
            }
        }
        System.out.println("Altar checks passed: "+count);
    }
}
