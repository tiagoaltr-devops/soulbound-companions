package br.com.soulbound.companion;

public final class SurvivalRules {
    public static final double BASE_HEALTH=33.6;
    private SurvivalRules(){}
    public static double health(String form){return BASE_HEALTH*(form.equals("GOD")?2:1);}
    public static float regeneration(String form,int rank){return (switch(form){case "BABY"->.5f;case "JUNIOR"->1f;case "PLENO"->1.5f;case "GOD"->2f;default->0f;})+.2f*Math.min(3,Math.max(0,rank));}
    public static boolean defensive(boolean previous,float hp,float maximum){return maximum>0 && (previous?hp/maximum<.65f:hp/maximum<=.5f);}
    /** At equal scale, raising a maximum must not grant health. God changes preserve the live percentage. */
    public static float healthAfter(float hp,double previous,double next,boolean godChange){return godChange?br.com.soulbound.ritual.CompanionEvolution.proportionalHealth(hp,previous,next):(float)Math.max(0,Math.min(hp,next));}
    public static float endGod(float hp,double activationScale,double maximum){if(!Double.isFinite(activationScale) || activationScale<=0)throw new IllegalArgumentException("Invalid activation health scale");return (float)Math.max(0,Math.min(maximum,hp/activationScale));}
}
