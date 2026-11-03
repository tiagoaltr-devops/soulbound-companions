package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;

public final class JuniorCorePolicy {
    private JuniorCorePolicy() {}
    public static boolean accepts(SoulboundRecord record,String owner,String bond,boolean spent){
        return record!=null && !spent && record.ownerUuid().toString().equals(owner) && (bond==null || record.bondUuid().toString().equals(bond))
            && record.archetypeId().equals(CompanionArchetype.LUPUS) && record.evolutionStage().equals("BABY") && record.activeForm().equals("BABY")
            && !record.unlockedForms().contains("JUNIOR") && record.companionStatus()==CompanionStatus.ACTIVE && !record.resurrectionRequired();
    }
}
