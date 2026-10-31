package br.com.soulbound.companion;

import java.util.UUID;
import br.com.soulbound.companion.identity.CompanionStatus;
import br.com.soulbound.companion.identity.SoulboundRecord;
import org.jspecify.annotations.Nullable;

/** Pure access policy, separate from the cosmetic item form. */
public final class SoulStaffAccess {
    private SoulStaffAccess() {}

    public static boolean allows(@Nullable SoulboundRecord record, UUID player, @Nullable UUID itemOwner, @Nullable UUID itemBond) {
        return record != null && record.ownerUuid().equals(player) && player.equals(itemOwner)
                && record.bondUuid().equals(itemBond) && record.companionStatus() == CompanionStatus.ACTIVE
                && !record.resurrectionRequired() && record.species().equals("LUPUS")
                && java.util.Set.of("BABY","JUNIOR","PLENO").contains(record.evolutionStage());
    }
}
