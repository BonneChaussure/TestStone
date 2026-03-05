package org.BonneChaussure.teststone;

import net.fabricmc.api.ModInitializer;
import org.BonneChaussure.blocks.ModBlocks;
import org.BonneChaussure.blocks.TestBenchBlockEntity;

public class Teststone implements ModInitializer {

    @Override
    public void onInitialize() {
        ModBlocks.initialize();

        var oui = TestBenchBlockEntity.TYPE;
    }
}
