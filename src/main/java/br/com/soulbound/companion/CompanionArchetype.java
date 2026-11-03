package br.com.soulbound.companion;

/** Form profiles select movement and combat independently of anatomy/renderers. No random selection yet. */
public final class CompanionArchetype {
    public static final String LUPUS="soulbound:lupus";
    public enum Movement { FLYING,GROUND }
    public record Combat(boolean offensive) {}
    public record Form(String id,Movement movement,Combat combat) {}
    public record Ability(String id,int cooldownTicks,double radius) {}
    public static final Ability ANCESTRAL_RUPTURE=new Ability("soulbound:ancestral_rupture",1200,5);
    public static Form form(String id){boolean warrior=id.equals("JUNIOR") || id.equals("PLENO") || id.equals("GOD");return new Form(id,warrior?Movement.GROUND:Movement.FLYING,new Combat(warrior));}
    private CompanionArchetype() {}
}
