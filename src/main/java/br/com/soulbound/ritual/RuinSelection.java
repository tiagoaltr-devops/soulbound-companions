package br.com.soulbound.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Deterministic thinning independent of chunk generation order. */
public final class RuinSelection {
    public static final int MAX_CENTER_OFFSET = 128;
    private RuinSelection() {}
    public static BoundingBox replacementBounds(BlockPos center,int radius,long[] bells){
        var bounds=new BoundingBox(center.getX()-radius,center.getY()-1,center.getZ()-radius,center.getX()+radius,center.getY()+4,center.getZ()+radius);
        for(long value:bells)bounds.encapsulate(BlockPos.of(value));return bounds;
    }
    public static boolean replacesBell(BlockPos pos,BoundingBox pass,long[] bells){
        if(!pass.isInside(pos))return false;for(long value:bells)if(value==pos.asLong())return true;return false;
    }
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
    public record Site(int x,int z) {}

    /** Two bounded local rounds. Round one retains every previous winner; round two fills its gaps. */
    public static final class Distribution {
        private final long seed;
        private final int separation;
        private final java.util.function.Function<Site,java.util.List<Site>> vicinity;
        private final java.util.function.Predicate<Site> eligible;
        private final java.util.Map<Site,java.util.List<Site>> neighbors=new java.util.HashMap<>();
        private final java.util.Map<Site,Boolean> eligibility=new java.util.HashMap<>(),first=new java.util.HashMap<>(),available=new java.util.HashMap<>();
        public Distribution(long seed,int separation,java.util.function.Function<Site,java.util.List<Site>> vicinity,java.util.function.Predicate<Site> eligible){
            this.seed=seed;this.separation=separation;this.vicinity=vicinity;this.eligible=eligible;
        }
        private java.util.List<Site> neighbors(Site site){
            return neighbors.computeIfAbsent(site,s->vicinity.apply(s).stream().filter(n->{
                long dx=((long)s.x-n.x)*16,dz=((long)s.z-n.z)*16,radius=separation+2L*MAX_CENTER_OFFSET;
                return dx*dx+dz*dz<radius*radius && eligibility.computeIfAbsent(n,eligible::test);
            }).toList());
        }
        private boolean first(Site site){return first.computeIfAbsent(site,s->neighbors(s).stream().allMatch(n->wins(seed,s.x,s.z,n.x,n.z,separation)));}
        private boolean available(Site site){return available.computeIfAbsent(site,s->neighbors(s).stream().noneMatch(this::first));}
        public boolean selected(Site site){
            if(!eligibility.computeIfAbsent(site,eligible::test))return false;
            return first(site) || available(site) && neighbors(site).stream().allMatch(n->!available(n) || wins(seed,site.x,site.z,n.x,n.z,separation));
        }
    }
}
