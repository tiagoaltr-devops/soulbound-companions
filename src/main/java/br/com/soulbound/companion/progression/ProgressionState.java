package br.com.soulbound.companion.progression;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable bond-owned progression. XP is the remainder within the current level. */
public record ProgressionState(int level,long xp,int earnedPoints,Map<String,Integer> talents,Set<String> rewards,long furyEnd,long furyCooldownEnd,int bonusCarry) {
    public static final int MAX_LEVEL=30;
    public static final ProgressionState INITIAL=new ProgressionState(1,0,0,Map.of(),Set.of());
    public static final Codec<ProgressionState> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.intRange(1,30).fieldOf("level").forGetter(ProgressionState::level),Codec.LONG.fieldOf("xp").forGetter(ProgressionState::xp),
        Codec.intRange(0,29).fieldOf("earnedPoints").forGetter(ProgressionState::earnedPoints),
        Codec.unboundedMap(Codec.STRING,Codec.intRange(1,5)).optionalFieldOf("talents",Map.of()).forGetter(ProgressionState::talents),
        Codec.STRING.listOf().xmap(Set::copyOf,ArrayList::new).optionalFieldOf("rewards",Set.of()).forGetter(ProgressionState::rewards),
        Codec.LONG.optionalFieldOf("furyEnd",0L).forGetter(ProgressionState::furyEnd),Codec.LONG.optionalFieldOf("furyCooldownEnd",0L).forGetter(ProgressionState::furyCooldownEnd),
        Codec.intRange(0,99).optionalFieldOf("bonusCarry",0).forGetter(ProgressionState::bonusCarry)
    ).apply(i,ProgressionState::new));
    public ProgressionState {
        talents=Map.copyOf(talents);rewards=Set.copyOf(rewards);
        if(level<1 || level>30 || earnedPoints!=level-1 || xp<0 || level==30 && xp!=0 || level<30 && xp>=required(level)
                || talents.values().stream().anyMatch(v->v<1 || v>5) || talents.values().stream().mapToLong(Integer::longValue).sum()>earnedPoints)throw new IllegalArgumentException("Invalid companion progression");
    }
    public ProgressionState(int level,long xp,int earnedPoints,Map<String,Integer> talents,Set<String> rewards){this(level,xp,earnedPoints,talents,rewards,0,0,0);}
    public ProgressionState fury(long end,long cooldown){return new ProgressionState(level,xp,earnedPoints,talents,rewards,end,cooldown,bonusCarry);}
    public ProgressionState rewardAdjusted(String key,int base,int bonusPercent){
        if(base<=0 || bonusPercent<0 || bonusPercent>100 || rewards.contains(key))return this;
        long scaled=(long)Math.min(base,1000000)*(100+bonusPercent)+bonusCarry;var next=reward(key,scaled/100);
        return new ProgressionState(next.level,next.xp,next.earnedPoints,next.talents,next.rewards,furyEnd,furyCooldownEnd,next.level==30?0:(int)(scaled%100));
    }
    public static long required(int level){if(level<1 || level>30)throw new IllegalArgumentException("Invalid level");long n=level-1;return 50+20*n+5*n*n;}
    public int spent(){return talents.values().stream().mapToInt(Integer::intValue).sum();}
    public int available(){return earnedPoints-spent();}
    public int rank(String id){return talents.getOrDefault(id,0);}
    public ProgressionState gain(long amount){
        if(amount<=0 || level==30)return this;long rest=xp+Math.min(amount,100000);int next=level;
        while(next<30 && rest>=required(next)){rest-=required(next);next++;}
        return new ProgressionState(next,next==30?0:rest,next-1,talents,rewards,furyEnd,furyCooldownEnd,next==30?0:bonusCarry);
    }
    public ProgressionState reward(String key,long amount){
        if(key==null || key.isBlank() || rewards.contains(key) || amount<=0)return this;
        var receipts=new HashSet<>(rewards);receipts.add(key);var gained=gain(amount);
        return new ProgressionState(gained.level,gained.xp,gained.earnedPoints,talents,receipts,furyEnd,furyCooldownEnd,gained.bonusCarry);
    }
    public ProgressionState invest(String id,int maximum){
        if(available()<1 || maximum<1 || rank(id)>=maximum)throw new IllegalArgumentException("Insufficient points or maximum rank");
        var next=new HashMap<>(talents);next.put(id,rank(id)+1);return new ProgressionState(level,xp,earnedPoints,next,rewards,furyEnd,furyCooldownEnd,bonusCarry);
    }
    public ProgressionState reset(){return new ProgressionState(level,xp,earnedPoints,Map.of(),rewards,furyEnd,furyCooldownEnd,bonusCarry);}
}
