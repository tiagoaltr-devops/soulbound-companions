package br.com.soulbound.ritual;

/** Deterministic thinning independent of chunk generation order. */
public final class RuinSelection {
    public static final int MAX_CENTER_OFFSET = 128;
    private RuinSelection() {}
    public static long distanceSquared(net.minecraft.core.BlockPos a,net.minecraft.core.BlockPos b){long x=(long)a.getX()-b.getX(),z=(long)a.getZ()-b.getZ();return x*x+z*z;}
    public static long priority(long seed, int x, int z) {
        long value=seed ^ (x*341873128712L) ^ (z*132897987541L) ^ 0x534f554c424f554eL;
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;
        return value^(value>>>31);
    }
    public static boolean wins(long seed,int x,int z,int otherX,int otherZ,int separation) {
        long dx=((long)x-otherX)*16,dz=((long)z-otherZ)*16;
        long exclusion=separation+2L*MAX_CENTER_OFFSET;
        if(dx*dx+dz*dz>=exclusion*exclusion)return true;
        int order=Long.compareUnsigned(priority(seed,x,z),priority(seed,otherX,otherZ));
        return order<0 || order==0 && (x<otherX || x==otherX && z<=otherZ);
    }
}
