package br.com.soulbound.client;

import br.com.soulbound.entity.SpiritGuardianEntity;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

public final class SpiritGuardianRenderer extends MobRenderer<SpiritGuardianEntity,ZombieRenderState,ZombieModel<ZombieRenderState>> {
    public SpiritGuardianRenderer(EntityRendererProvider.Context context){super(context,new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)),.5f);}
    @Override public ZombieRenderState createRenderState(){return new ZombieRenderState();}
    @Override public Identifier getTextureLocation(ZombieRenderState state){return Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");}
}
