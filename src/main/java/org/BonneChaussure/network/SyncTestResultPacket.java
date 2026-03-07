package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Envoyé après chaque cas (résultat partiel) et à la fin de la session.
 * Si caseIdx == -1, c'est un résultat final complet (tableau allResults non null).
 */
public record SyncTestResultPacket(
        BlockPos bench,
        int caseIdx,      // -1 = session terminée
        boolean pass,
        boolean[] allResults // utilisé seulement si caseIdx == -1
) implements CustomPayload {

    public static final Id<SyncTestResultPacket> ID =
            new Id<>(Identifier.of("teststone", "sync_test_result"));

    public static final PacketCodec<RegistryByteBuf, SyncTestResultPacket> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeBlockPos(p.bench());
                buf.writeInt(p.caseIdx());
                buf.writeBoolean(p.pass());
                buf.writeInt(p.allResults().length);
                for (boolean b : p.allResults()) buf.writeBoolean(b);
            },
            buf -> {
                BlockPos bench = buf.readBlockPos();
                int idx = buf.readInt();
                boolean pass = buf.readBoolean();
                int len = buf.readInt();
                boolean[] all = new boolean[len];
                for (int i = 0; i < len; i++) all[i] = buf.readBoolean();
                return new SyncTestResultPacket(bench, idx, pass, all);
            }
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
