package br.com.soulbound.entity.ai;

/** Bounded, nonpersistent state machine; expiration always yields back to normal navigation. */
public final class ResonanceFlightState {
    public enum Phase { IDLE, OUTBOUND, HOLD, RETURN }
    private Phase phase=Phase.IDLE;
    private long started,holdEnd;
    public Phase phase(){return phase;}
    public boolean active(){return phase!=Phase.IDLE;}
    public void begin(long now){started=now;holdEnd=0;phase=Phase.OUTBOUND;}
    public void cancel(){phase=Phase.IDLE;}
    public void tick(long now,boolean reached,boolean nearOwner){
        if(!active())return;
        if(now-started>=200){cancel();return;}
        if(phase==Phase.OUTBOUND && reached){phase=Phase.HOLD;holdEnd=now+30;}
        else if(phase==Phase.OUTBOUND && now-started>=80)phase=Phase.RETURN;
        else if(phase==Phase.HOLD && now>=holdEnd)phase=Phase.RETURN;
        else if(phase==Phase.RETURN && nearOwner)cancel();
    }
}
