package br.com.soulbound.companion;

import java.util.Set;
import java.util.HashSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Canonical form selection, independent from the highest permanent evolution stage. */
public record CompanionForms(String archetypeId,Set<String> unlockedForms,String activeForm,long switchEnd,long ultimateEnd,long plenoUltimateEnd,long chargeEnd,long quakeEnd,GodState god) {
    public static final CompanionForms LEGACY=new CompanionForms("soulbound:lupus",Set.of(),"BABY",0,0);
    public static final Codec<CompanionForms> CODEC=RecordCodecBuilder.create(i->i.group(
        Codec.STRING.fieldOf("archetypeId").forGetter(CompanionForms::archetypeId),
        Codec.STRING.listOf().xmap(Set::copyOf,java.util.ArrayList::new).fieldOf("unlockedForms").forGetter(CompanionForms::unlockedForms),
        Codec.STRING.fieldOf("activeForm").forGetter(CompanionForms::activeForm),
        Codec.LONG.optionalFieldOf("switchEnd",0L).forGetter(CompanionForms::switchEnd),
        Codec.LONG.optionalFieldOf("ultimateEnd",0L).forGetter(CompanionForms::ultimateEnd),
        Codec.LONG.optionalFieldOf("plenoUltimateEnd",0L).forGetter(CompanionForms::plenoUltimateEnd),
        Codec.LONG.optionalFieldOf("chargeEnd",0L).forGetter(CompanionForms::chargeEnd),Codec.LONG.optionalFieldOf("quakeEnd",0L).forGetter(CompanionForms::quakeEnd),GodState.CODEC.optionalFieldOf("god",GodState.EMPTY).forGetter(CompanionForms::god)
    ).apply(i,CompanionForms::new));
    public CompanionForms {unlockedForms=Set.copyOf(unlockedForms);if(!archetypeId.equals("soulbound:lupus") || !Set.of("BABY","JUNIOR","PLENO","GOD").containsAll(unlockedForms) || !unlockedForms.isEmpty() && !unlockedForms.contains(activeForm))throw new IllegalArgumentException("Invalid companion forms");}
    public CompanionForms(String archetype,Set<String> forms,String active,long change,long juniorUltimate){this(archetype,forms,active,change,juniorUltimate,0,0,0,GodState.EMPTY);}
    public CompanionForms(String archetype,Set<String> forms,String active,long change,long juniorUltimate,long plenoUltimate,long charge,long quake){this(archetype,forms,active,change,juniorUltimate,plenoUltimate,charge,quake,GodState.EMPTY);}
    public static CompanionForms migrate(String stage){var forms=new HashSet<String>();forms.add("BABY");if(!stage.equals("BABY"))forms.add("JUNIOR");if(stage.equals("PLENO"))forms.add("PLENO");return new CompanionForms("soulbound:lupus",forms,stage,0,0);}
    public CompanionForms unlockJunior(){var forms=new HashSet<>(unlockedForms);forms.add("JUNIOR");return new CompanionForms(archetypeId,forms,activeForm,switchEnd,ultimateEnd,plenoUltimateEnd,chargeEnd,quakeEnd,god);}
    public CompanionForms unlockPleno(){var forms=new HashSet<>(unlockedForms);forms.addAll(Set.of("BABY","JUNIOR","PLENO"));return new CompanionForms(archetypeId,forms,activeForm,switchEnd,ultimateEnd,plenoUltimateEnd,chargeEnd,quakeEnd,god);}
    public boolean canSelect(String form,long now){return Set.of("BABY","JUNIOR","PLENO").contains(form) && unlockedForms.contains(form) && !activeForm.equals("GOD") && !activeForm.equals(form) && now>=switchEnd;}
    public CompanionForms select(String form,long now){if(!canSelect(form,now))throw new IllegalArgumentException("Locked form or cooldown");return new CompanionForms(archetypeId,unlockedForms,form,now+60,ultimateEnd,plenoUltimateEnd,chargeEnd,quakeEnd,god);}
    public CompanionForms ultimate(long end){return new CompanionForms(archetypeId,unlockedForms,activeForm,switchEnd,end,plenoUltimateEnd,chargeEnd,quakeEnd,god);}
    public CompanionForms plenoUltimate(long end){return new CompanionForms(archetypeId,unlockedForms,activeForm,switchEnd,ultimateEnd,end,chargeEnd,quakeEnd,god);}
    public CompanionForms secondary(boolean charge,long end){return new CompanionForms(archetypeId,unlockedForms,activeForm,switchEnd,ultimateEnd,plenoUltimateEnd,charge?end:chargeEnd,charge?quakeEnd:end,god);}
    public CompanionForms recoverBaby(){return new CompanionForms(archetypeId,unlockedForms,"BABY",switchEnd,ultimateEnd,plenoUltimateEnd,chargeEnd,quakeEnd,god.finish());}
    public long activeUltimateEnd(){return activeForm.equals("GOD")?god.ultimateEnd():activeForm.equals("PLENO")?plenoUltimateEnd:ultimateEnd;}
    public boolean canUltimate(BehaviorMode mode,boolean available,long now){return Set.of("JUNIOR","PLENO","GOD").contains(activeForm) && unlockedForms.contains(activeForm) && mode==BehaviorMode.FOLLOW_COMBAT && available && now>=activeUltimateEnd();}
    public CompanionForms withGod(GodState state){return new CompanionForms(archetypeId,unlockedForms,activeForm,switchEnd,ultimateEnd,plenoUltimateEnd,chargeEnd,quakeEnd,state);}
    public CompanionForms unlockGod(){var forms=new HashSet<>(unlockedForms);forms.addAll(Set.of("BABY","JUNIOR","PLENO","GOD"));return new CompanionForms(archetypeId,forms,activeForm,switchEnd,ultimateEnd,plenoUltimateEnd,chargeEnd,quakeEnd,god);}
    public CompanionForms activateGod(long now,float shield){if(!activeForm.equals("PLENO") || !unlockedForms.contains("GOD"))throw new IllegalStateException("God activation requires unlocked Pleno");return new CompanionForms(archetypeId,unlockedForms,"GOD",now+60,ultimateEnd,plenoUltimateEnd,chargeEnd,quakeEnd,god.activate(now,shield));}
    public CompanionForms endGod(){return new CompanionForms(archetypeId,unlockedForms,activeForm.equals("GOD")?"PLENO":activeForm,switchEnd,ultimateEnd,plenoUltimateEnd,chargeEnd,quakeEnd,god.finish());}}
