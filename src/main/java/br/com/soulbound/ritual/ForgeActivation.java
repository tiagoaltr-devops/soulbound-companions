package br.com.soulbound.ritual;

import java.util.Map;

/** Persisted activation transitions shared by the server and focused checks. */
public final class ForgeActivation {
    public static final int DURATION=300;
    private ForgeActivation() {}
    public static boolean offeringMatches(String actual,int amount,String required,int count){return count>0 && amount>=count && actual.equals(required);}
    public static int view(AltarSavedData.Forge f){
        return f.activation!=null?4:f.state.equals("DESTROYED")?3:f.state.equals("ACTIVE")?2:f.owner==null?0:1;
    }
    public static String rejection(AltarSavedData.Forge f,String player){
        if(f.activation!=null)return "busy";
        if(f.owner!=null && !f.owner.equals(player))return "permission";
        if(!f.placed || !(f.owner==null && f.state.equals("UNBOUND") || f.owner!=null && f.state.equals("INACTIVE")))return "state";
        return null;
    }
    public static boolean coreAllowed(AltarSavedData.Forge f,String player,boolean essence){
        return f.activation==null && (f.state.equals("ACTIVE") || f.state.equals("INACTIVE") && player.equals(f.owner) && essence);
    }
    public static boolean begin(AltarSavedData d,AltarSavedData.Forge f,AltarSavedData.Activation a,Map<String,String> remainders){
        if(rejection(f,a.player)!=null)return false;
        d.state.slots.putAll(remainders);if(!a.core.isEmpty())f.core="";f.activation=a;return true;
    }
    public static AltarSavedData.Activation complete(AltarSavedData.Forge f,long now){
        var a=f.activation;if(a==null || now<a.end)return null;
        if(f.owner==null)f.owner=a.player;
        f.state=a.result;f.attempts=a.attempts;f.activation=null;return a;
    }
}
