package br.com.soulbound.companion;

/** Explicit read-only diagnostics. Never invoked by the normal build/check tasks. */
public final class IdentitySnapshot {
    public static void main(String[] args)throws Exception{
        var root=java.nio.file.Path.of("run/saves");if(!java.nio.file.Files.isDirectory(root))return;
        try(var worlds=java.nio.file.Files.list(root)){
            for(var world:worlds.filter(java.nio.file.Files::isDirectory).toList()){
                var file=world.resolve("data/soulbound_identity.dat");if(!java.nio.file.Files.isRegularFile(file))continue;
                var nbt=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                for(var r:br.com.soulbound.companion.identity.SoulboundRecord.CODEC.listOf().parse(net.minecraft.nbt.NbtOps.INSTANCE,nbt.get("data")).getOrThrow())
                    System.out.println("IDENTITY_SNAPSHOT world="+world.getFileName()+" name="+r.ownerLastKnownName()+" status="+r.companionStatus()+" resurrection="+r.resurrectionRequired()+" stage="+r.evolutionStage()+" active="+r.activeForm()+" unlocked="+r.unlockedForms()+" mode="+r.behaviorMode());
            }
        }
    }
}
