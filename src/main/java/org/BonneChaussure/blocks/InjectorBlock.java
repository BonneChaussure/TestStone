package org.BonneChaussure.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class InjectorBlock extends Block implements BlockEntityProvider {

    public static final BooleanProperty POWERED = Properties.POWERED;

    public InjectorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InjectorBlockEntity(pos, state);
    }

    // ── Signal redstone ───────────────────────────────────────────────────────

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world,
                                    BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public int getStrongRedstonePower(BlockState state, BlockView world,
                                      BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    // ── Notifie les blocs voisins quand POWERED change ────────────────────────

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        if (!moved && !state.isOf(newState.getBlock())) {
            // Ne notifie les voisins que si c'était alimenté — et seulement depuis l'ancien state
            if (state.get(POWERED)) {
                world.updateNeighborsAlways(pos, this);
                for (Direction dir : Direction.values()) {
                    world.updateNeighborsAlways(pos.offset(dir), this);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // ── Clic droit → GUI de renommage ─────────────────────────────────────────

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.PASS;
        if (world.getBlockEntity(pos) instanceof InjectorBlockEntity be) {
            player.openHandledScreen(be);
        }

        // Force la resynchronisation côté client pour éviter les ghost blocks
        // quand le joueur avait un item en main au moment du clic droit
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(
                    new net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket(world, pos)
            );
        }

        return ActionResult.SUCCESS;
    }
}
