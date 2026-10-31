package br.com.soulbound.ritual;

import br.com.soulbound.registry.ModAltar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Read-only display snapshot. SavedData owns every stored item and all transfers. */
@EventBusSubscriber(modid="soulbound")
public final class OfferingBlockEntity extends BlockEntity {
    private record Pending(ServerLevel level,BlockPos pos) {}
    private static final java.util.concurrent.ConcurrentLinkedQueue<Pending> PENDING=new java.util.concurrent.ConcurrentLinkedQueue<>();
    private ItemStack display=ItemStack.EMPTY;
    public OfferingBlockEntity(BlockPos pos,BlockState state){super(ModAltar.OFFERING_DISPLAY.get(),pos,state);}
    public ItemStack display(){return display;}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel server)refresh(server,worldPosition);}
    public static void refresh(ServerLevel level,String key){
        if(!key.startsWith(level.dimension().identifier()+":"))return;
        refresh(level,BlockPos.of(Long.parseLong(key.substring(key.lastIndexOf(':')+1))));
    }
    public static void refresh(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof OfferingBlockEntity be))return;
        var stack=AltarInventory.decode(level,AltarSavedData.get(level.getServer()).state.slots.get(AltarInventory.key(level,pos)));
        if(stack.getCount()==be.display.getCount() && ItemStack.isSameItemSameComponents(stack,be.display))return;
        be.display=stack.copy();be.setChanged();level.sendBlockUpdated(pos,be.getBlockState(),be.getBlockState(),3);
    }
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(!display.isEmpty())out.store("Display",ItemStack.CODEC,display);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);display=in.read("Display",ItemStack.CODEC).orElse(ItemStack.EMPTY);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registry){return saveCustomOnly(registry);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var chunk=event.getChunk();var sections=chunk.getSections();
        // Old chunks have altar blocks without a BlockEntity. Inspect only sections whose palette contains this block.
        for(int i=0;i<sections.length;i++){
            var section=sections[i];if(!section.maybeHas(state->state.is(ModAltar.OFFERING.get())))continue;
            int baseY=chunk.getMinY()+i*16;
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++)if(section.getBlockState(x,y,z).is(ModAltar.OFFERING.get())){
                var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,baseY+y,chunk.getPos().getMinBlockZ()+z);
                PENDING.add(new Pending(level,pos));
            }
        }
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        // Chunk load events may precede FULL promotion. Wait until a server tick before asking for BlockEntities.
        for(int i=0;i<32;i++){var pending=PENDING.poll();if(pending==null)break;
            if(pending.level.getServer()==event.getServer())refresh(pending.level,pending.pos);else PENDING.add(pending);
        }
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event){PENDING.removeIf(p->p.level.getServer()==event.getServer());}
}
