package br.com.soulbound.ritual;

import br.com.soulbound.registry.ModAltar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Worldgen provenance, not a storage inventory. Placement/creative creates an unstamped pedestal. */
public final class AncestralPedestalBlockEntity extends BlockEntity {
    private String ruinId="";
    private long naturalPosition;
    private int villageX,villageZ;
    public AncestralPedestalBlockEntity(BlockPos pos,BlockState state){super(ModAltar.NATURAL_PEDESTAL.get(),pos,state);}
    public void stamp(String id,int x,int z){ruinId=id;naturalPosition=worldPosition.asLong();villageX=x;villageZ=z;}
    public boolean matches(String id){return !ruinId.isEmpty() && ruinId.equals(id) && naturalPosition==worldPosition.asLong();}
    public String naturalId(){return ruinId;}
    public net.minecraft.world.level.ChunkPos source(){return new net.minecraft.world.level.ChunkPos(villageX,villageZ);}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putString("NaturalRuin",ruinId);out.putLong("NaturalPosition",naturalPosition);out.putInt("VillageX",villageX);out.putInt("VillageZ",villageZ);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);ruinId=in.getStringOr("NaturalRuin","");naturalPosition=in.getLongOr("NaturalPosition",0);villageX=in.getIntOr("VillageX",0);villageZ=in.getIntOr("VillageZ",0);
    }
    @Override public void onLoad(){
        super.onLoad();
        if(level instanceof ServerLevel serverLevel && matches(ruinId))RuinIndex.loaded(serverLevel,worldPosition,ruinId,villageX,villageZ);
    }
}
