package br.com.soulbound.companion;

import br.com.soulbound.network.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.progression.ProgressionState;

/** Presentation math and wire snapshots only; no OpenGL/client or gameplay claim. */
public final class CompanionHudChecks {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("Companion HUD check "+checks);}
    public static void run()throws Exception{
        var r=SoulboundRecord.create(java.util.UUID.randomUUID(),"HudOwner");
        check(CompanionHudData.project(null,"Lupus",false,0,0).equals(CompanionHudData.EMPTY));
        var ready=CompanionHudData.project(r,"Lupus",true,12,12);check(!ready.healthKnown());check(ready.healthFraction()==0);check(ready.level()==1 && ready.xp()==0 && ready.required()==50);
        r=r.withState(CompanionStatus.ACTIVE,false,"FOLLOW_PASSIVE");var full=CompanionHudData.project(r,"Lupus",true,12,12);
        check(full.healthKnown() && full.healthFraction()==1);check(full.owner().equals(r.ownerUuid()) && full.bond().equals(r.bondUuid()));
        var hurt=CompanionHudData.project(r,"Lupus",true,9,12);check(hurt.healthFraction()==.75f);check(!hurt.equals(full));
        var healed=CompanionHudData.project(r,"Lupus",true,9.1f,12);check(healed.health()>hurt.health());check(!healed.equals(hurt));
        var maximum=CompanionHudData.project(r,"Lupus",true,6.24f,12.48f);check(Math.abs(maximum.healthFraction()-.5)<.00001);
        var progressed=r.withProgression(ProgressionState.INITIAL.gain(50));var levelUp=CompanionHudData.project(progressed,"Lupus",true,12,12);
        check(levelUp.level()==2 && levelUp.xp()==0 && levelUp.required()==75);check(levelUp.xpFraction()==0);
        var max=CompanionHudData.project(r.withProgression(ProgressionState.INITIAL.gain(Long.MAX_VALUE)),"Lupus",true,12,12);
        check(max.level()==30 && max.required()==0 && max.xpFraction()==1);
        var junior=r.unlockJunior();junior=junior.withForms(junior.forms().select("JUNIOR",100));var transformed=CompanionHudData.project(junior,"Lupus",true,24,24);
        check(transformed.form().equals("JUNIOR") && transformed.maxHealth()==24);check(!transformed.equals(full));
        var deadRecord=junior.withState(CompanionStatus.DEAD,true,"STAY").withProgression(progressed.progression());
        var dead=CompanionHudData.project(deadRecord,"Lupus",true,24,24);check(dead.status()==CompanionStatus.DEAD && dead.health()==0 && !dead.healthKnown());check(dead.level()==2 && dead.required()==75);
        var recovered=CompanionHudData.project(deadRecord.recoverAfterDeath(),"Lupus",false,24,24);check(recovered.form().equals("BABY") && recovered.status()==CompanionStatus.READY && !recovered.healthKnown());check(recovered.level()==dead.level() && recovered.xp()==dead.xp());
        var unavailable=CompanionHudData.project(junior,"Lupus",false,24,24);check(!unavailable.healthKnown() && unavailable.maxHealth()==0);
        for(var status:CompanionStatus.values())if(status!=CompanionStatus.ACTIVE)check(!CompanionHudData.project(r.withState(status,false,"STAY"),"Lupus",true,12,12).healthKnown());
        check(!CompanionHudData.project(r,"Lupus",true,Float.NaN,12).healthKnown());check(!CompanionHudData.project(r,"Lupus",true,12,Float.POSITIVE_INFINITY).healthKnown());
        check(CompanionHudData.project(r,"Lupus",true,20,12).health()==12);check(CompanionHudData.project(r,"Lupus",true,-1,12).health()==0);
        check(CompanionHudData.fraction(5,0)==0);check(CompanionHudData.fraction(Double.NaN,10)==0);check(CompanionHudData.fraction(20,10)==1);
        check(CompanionHudData.project(r,"x".repeat(200),true,12,12).name().length()==128);
        for(var snapshot:new CompanionHudData[]{CompanionHudData.EMPTY,ready,full,hurt,healed,maximum,levelUp,max,transformed,dead,recovered,unavailable}){
            var buf=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{var payload=new CompanionHudPayload(snapshot);CompanionHudPayload.CODEC.encode(buf,payload);check(CompanionHudPayload.CODEC.decode(buf).equals(payload));check(buf.readableBytes()==0);}finally{buf.release();}
        }
        check(full.equals(CompanionHudData.project(r,"Lupus",true,12,12)));
        for(String lang:new String[]{"pt_br","en_us"})try(var stream=CompanionHudChecks.class.getResourceAsStream("/assets/soulbound/lang/"+lang+".json")){
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            check(json.has("hud.soulbound.health") && json.has("hud.soulbound.level_max") && json.has("hud.soulbound.state.ready"));
        }
        System.out.println("Companion HUD checks passed: "+checks);
    }
}
