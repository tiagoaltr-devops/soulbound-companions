package br.com.soulbound.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Species-independent visual inputs; no ownership or movement authority resides here. */
public final class BabyCompanionRenderState extends LivingEntityRenderState {
    public float flightAmount;
    public float discoveryAmount;
    public boolean staying;
    public boolean alerting;
    public boolean retreating;
}
