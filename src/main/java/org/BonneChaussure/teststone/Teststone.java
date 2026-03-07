package org.BonneChaussure.teststone;

import net.fabricmc.api.ModInitializer;
import org.BonneChaussure.blocks.ModBlocks;
import org.BonneChaussure.blocks.TestBenchBlockEntity;
import org.BonneChaussure.gui.TestCaseScreenHandler;
import org.BonneChaussure.network.*;

public class Teststone implements ModInitializer {

    @Override
    public void onInitialize() {
        ModBlocks.initialize(); // tout est initialisé ici, dans le bon ordre
        SetBoundaryBoxPacket.register();
        RemoveBoundaryBoxPacket.register();
        UpdateBenchPacket.register();
        ScanBenchPacket.register();
        SyncScannedBlocksPacket.register();
        SaveTestCasesPacket.register();
        // Plus de var oui ici
    }
}
