package br.com.soulbound.companion;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Bond-owned snapshots. Stacks use the existing registry-aware ItemStack serializer, never item IDs alone. */
public record SurvivalState(List<String> slots,long combatUntil,long foodEnd,boolean defensive,long regenEnd) {
    public static final SurvivalState EMPTY=new SurvivalState(Collections.nCopies(9,""),0,0,false);
    public static final Codec<SurvivalState> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.listOf().optionalFieldOf("slots",Collections.nCopies(9,"")).forGetter(SurvivalState::slots),
        Codec.LONG.optionalFieldOf("combatUntil",0L).forGetter(SurvivalState::combatUntil),Codec.LONG.optionalFieldOf("foodEnd",0L).forGetter(SurvivalState::foodEnd),
        Codec.BOOL.optionalFieldOf("defensive",false).forGetter(SurvivalState::defensive),Codec.LONG.optionalFieldOf("regenEnd",0L).forGetter(SurvivalState::regenEnd)
    ).apply(i,SurvivalState::new));
    public SurvivalState(List<String> slots,long combat,long food,boolean defensive){this(slots,combat,food,defensive,0);}
    public SurvivalState regenerated(long end){return new SurvivalState(slots,combatUntil,foodEnd,defensive,end);}
    public SurvivalState{slots=List.copyOf(slots);if(slots.size()!=9 || regenEnd<0 || combatUntil<0 || foodEnd<0)throw new IllegalArgumentException("Invalid survival state");}
    public SurvivalState contents(List<String> next){return new SurvivalState(next,combatUntil,foodEnd,defensive,regenEnd);}
    public SurvivalState combat(long end){return new SurvivalState(slots,Math.max(end,combatUntil),foodEnd,defensive,regenEnd);}
    public SurvivalState fed(long end){return new SurvivalState(slots,combatUntil,end,defensive,regenEnd);}
    public SurvivalState defense(boolean state){return new SurvivalState(slots,combatUntil,foodEnd,state,regenEnd);}
}
