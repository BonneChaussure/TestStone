package org.BonneChaussure.blocks;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.BonneChaussure.network.RemoveBoundaryBoxPacket;
import org.BonneChaussure.network.SetBoundaryBoxPacket;
import org.BonneChaussure.network.SyncTestResultPacket;
import org.jetbrains.annotations.Nullable;

public class TestBenchBlock extends Block implements BlockEntityProvider {

    public TestBenchBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TestBenchBlockEntity(pos, state);
    }

    // ── Ticker ────────────────────────────────────────────────────────────────
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        // On ne tick que côté serveur
        if (world.isClient) return null;
        // Vérifie que le type correspond bien avant de caster
        if (type != TestBenchBlockEntity.TYPE) return null;
        return (w, pos, s, be) -> ((TestBenchBlockEntity) be).tick((ServerWorld) w);
    }

    // ── Pose ──────────────────────────────────────────────────────────────────
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.isClient) return;
        if (world.getBlockEntity(pos) instanceof TestBenchBlockEntity be) {
            be.initBoundaryBox();
            sendBoxToAll((ServerWorld) world, be);
        }
    }

    // ── Destruction ───────────────────────────────────────────────────────────
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        if (!world.isClient && !state.isOf(newState.getBlock())) {
            sendRemoveBoxToAll((ServerWorld) world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // ── Interaction ───────────────────────────────────────────────────────────
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;
        if (world.getBlockEntity(pos) instanceof TestBenchBlockEntity be) {
            player.openHandledScreen(be);
        }
        return ActionResult.SUCCESS;
    }

    // ── Utilitaires réseau ────────────────────────────────────────────────────

    public static void sendBoxToAll(ServerWorld world, TestBenchBlockEntity be) {
        var packet = new SetBoundaryBoxPacket(be.getPos(), be.getCorner1(), be.getCorner2(), be.getColor());
        world.getPlayers().forEach(p -> ServerPlayNetworking.send(p, packet));
    }

    public static void sendRemoveBoxToAll(ServerWorld world, BlockPos pos) {
        world.getPlayers().forEach(p ->
                ServerPlayNetworking.send(p, new RemoveBoundaryBoxPacket(pos)));
    }

    /** Résultat d'un cas individuel — envoyé au fil de l'eau pendant l'exécution. */
    public static void sendCaseResult(ServerWorld world, TestBenchBlockEntity be,
                                      int caseIdx, boolean pass) {
        // allResults vide pour un résultat partiel (caseIdx >= 0)
        var packet = new SyncTestResultPacket(be.getPos(), caseIdx, pass, new boolean[0]);
        world.getPlayers().forEach(p -> ServerPlayNetworking.send(p, packet));
    }

    /** Résultats complets en fin de session (caseIdx = -1). */
    public static void sendAllResults(ServerWorld world, TestBenchBlockEntity be,
                                      Boolean[] results) {
        boolean[] flat = new boolean[results.length];
        for (int i = 0; i < results.length; i++)
            flat[i] = results[i] != null && results[i];
        // caseIdx = -1 signale la fin de session
        var packet = new SyncTestResultPacket(be.getPos(), -1, false, flat);
        world.getPlayers().forEach(p -> ServerPlayNetworking.send(p, packet));
    }
}
