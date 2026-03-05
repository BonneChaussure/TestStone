package org.BonneChaussure.blocks;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.BonneChaussure.network.RemoveBoundaryBoxPacket;
import org.BonneChaussure.network.SetBoundaryBoxPacket;
import org.jetbrains.annotations.Nullable;

public class TestBenchBlock extends Block implements BlockEntityProvider {

    public TestBenchBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TestBenchBlockEntity(pos, state);
    }

    // Appelé juste après que le bloc est placé dans le monde
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);

        if (world.isClient) return; // logique serveur uniquement

        if (world.getBlockEntity(pos) instanceof TestBenchBlockEntity be) {
            be.initBoundaryBox(); // calcule + markDirty()
            sendBoxToAll((ServerWorld) world, be);
        }
    }


    // Appelé quand le bloc est détruit
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        if (!world.isClient && !state.isOf(newState.getBlock())) {
            // Envoie le packet de suppression à tous les joueurs
            sendRemoveBoxToALl((ServerWorld) world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // Utilitaire : envoie SetBoundaryBoxPacket à tous les joueurs connectés
    public static void sendBoxToAll(ServerWorld world, TestBenchBlockEntity be) {
        var packet = new SetBoundaryBoxPacket(be.getPos(), be.getCorner1(), be.getCorner2(), be.getColor());
        world.getPlayers().forEach(player ->
                ServerPlayNetworking.send(player, packet)
        );
    }

    public static void sendRemoveBoxToALl(ServerWorld world, BlockPos pos){
        world.getPlayers().forEach(player ->
                ServerPlayNetworking.send(player,
                        new RemoveBoundaryBoxPacket(pos))
        );
    }
}
