package org.BonneChaussure.blocks;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.BonneChaussure.gui.TestBenchScreenHandler;
import org.BonneChaussure.gui.TestCaseScreenHandler;

public class ModBlocks {

    public static final Block INJECTOR   = register("injector",    new InjectorBlock(AbstractBlock.Settings.create()));
    public static final Block SENSOR = register("sensor", new SensorBlock(AbstractBlock.Settings.create()));
    public static final Block TEST_BENCH  = register("test_bench",  new TestBenchBlock(AbstractBlock.Settings.create()));

    private static Block register(String name, Block block) {
        Identifier id = Identifier.of("teststone", name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.add(INJECTOR);
            entries.add(SENSOR);
            entries.add(TEST_BENCH);
        });

        // Force l'enregistrement du BlockEntityType et ScreenHandlerType
        var _be = TestBenchBlockEntity.TYPE;
        var _sh = TestBenchScreenHandler.TYPE;
        var _tc = TestCaseScreenHandler.TYPE;
    }
}
