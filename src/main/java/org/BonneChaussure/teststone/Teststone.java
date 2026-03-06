package org.BonneChaussure.teststone;

import net.fabricmc.api.ModInitializer;
import org.BonneChaussure.blocks.ModBlocks;
import org.BonneChaussure.blocks.TestBenchBlockEntity;
import org.BonneChaussure.gui.TestBenchScreenHandler;
import org.BonneChaussure.network.RemoveBoundaryBoxPacket;
import org.BonneChaussure.network.SetBoundaryBoxPacket;
import org.BonneChaussure.network.UpdateBenchPacket;

public class Teststone implements ModInitializer {

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        SetBoundaryBoxPacket.register();
        RemoveBoundaryBoxPacket.register();
        UpdateBenchPacket.register();

        var oui = TestBenchBlockEntity.TYPE;
        var non = TestBenchScreenHandler.TYPE;
    }
}
