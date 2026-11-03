package br.com.soulbound.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Canonical temporary God state. All deadlines use persisted Overworld game time. */
public record GodState(long end,float shield,float maximum,long ultimateEnd,long chargeEnd,long ruptureEnd) {
    public static final GodState EMPTY=new GodState(0,0,0,0,0,0);
    public static final Codec<GodState> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.LONG.optionalFieldOf("end",0L).forGetter(GodState::end),Codec.FLOAT.optionalFieldOf("shield",0f).forGetter(GodState::shield),
        Codec.FLOAT.optionalFieldOf("maximum",0f).forGetter(GodState::maximum),Codec.LONG.optionalFieldOf("ultimateEnd",0L).forGetter(GodState::ultimateEnd),
        Codec.LONG.optionalFieldOf("chargeEnd",0L).forGetter(GodState::chargeEnd),Codec.LONG.optionalFieldOf("ruptureEnd",0L).forGetter(GodState::ruptureEnd)
    ).apply(i,GodState::new));
    public GodState{if(end<0 || !Float.isFinite(shield) || !Float.isFinite(maximum) || shield<0 || maximum<0 || shield>maximum || end==0 && (shield!=0 || maximum!=0))throw new IllegalArgumentException("Invalid God barrier");}
    public GodState activate(long now,float maximum){return new GodState(now+1200,maximum,maximum,ultimateEnd,chargeEnd,ruptureEnd);}
    public GodState finish(){return new GodState(0,0,0,ultimateEnd,chargeEnd,ruptureEnd);}
    public GodState absorb(float damage){return new GodState(end,Math.max(0,shield-Math.max(0,damage)),maximum,ultimateEnd,chargeEnd,ruptureEnd);}
    public GodState ultimate(long deadline){return new GodState(end,shield,maximum,deadline,chargeEnd,ruptureEnd);}
    public GodState secondary(boolean charge,long deadline){return new GodState(end,shield,maximum,ultimateEnd,charge?deadline:chargeEnd,charge?ruptureEnd:deadline);}
}
