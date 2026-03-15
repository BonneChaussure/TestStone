package org.BonneChaussure.tests;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record TestCase(
        String name,
        Map<BlockPos, Boolean> injectorValues,
        List<Map<BlockPos, Boolean>> observations   // une entrée → N observations séquentielles
) {

    // ── Constructeur de commodité : cas linéaire (une seule observation) ──────
    public static TestCase ofSingle(String name,
                                    Map<BlockPos, Boolean> injectorValues,
                                    Map<BlockPos, Boolean> sensorExpected) {
        List<Map<BlockPos, Boolean>> obs = new ArrayList<>();
        obs.add(new HashMap<>(sensorExpected));
        return new TestCase(name, injectorValues, obs);
    }

    // ── Sérialisation NBT ─────────────────────────────────────────────────────

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", name);

        // Injectors
        NbtList injList = new NbtList();
        injectorValues.forEach((pos, val) -> {
            NbtCompound entry = new NbtCompound();
            entry.putLong("pos", pos.asLong());
            entry.putBoolean("val", val);
            injList.add(entry);
        });
        nbt.put("injectors", injList);

        // Observations (liste de maps sensors)
        NbtList obsList = new NbtList();
        for (Map<BlockPos, Boolean> obs : observations) {
            NbtList senList = new NbtList();
            obs.forEach((pos, val) -> {
                NbtCompound entry = new NbtCompound();
                entry.putLong("pos", pos.asLong());
                entry.putBoolean("val", val);
                senList.add(entry);
            });
            NbtCompound obsEntry = new NbtCompound();
            obsEntry.put("sensors", senList);
            obsList.add(obsEntry);
        }
        nbt.put("observations", obsList);

        return nbt;
    }

    public static TestCase fromNbt(NbtCompound nbt) {
        String name = nbt.getString("name");

        // Injectors
        Map<BlockPos, Boolean> injectors = new HashMap<>();
        NbtList injList = nbt.getList("injectors", 10);
        for (int i = 0; i < injList.size(); i++) {
            NbtCompound e = injList.getCompound(i);
            injectors.put(BlockPos.fromLong(e.getLong("pos")), e.getBoolean("val"));
        }

        // Observations — rétrocompat : ancien format "sensors" → liste à 1 élément
        List<Map<BlockPos, Boolean>> observations = new ArrayList<>();
        if (nbt.contains("observations")) {
            NbtList obsList = nbt.getList("observations", 10);
            for (int i = 0; i < obsList.size(); i++) {
                NbtList senList = obsList.getCompound(i).getList("sensors", 10);
                Map<BlockPos, Boolean> obs = new HashMap<>();
                for (int j = 0; j < senList.size(); j++) {
                    NbtCompound e = senList.getCompound(j);
                    obs.put(BlockPos.fromLong(e.getLong("pos")), e.getBoolean("val"));
                }
                observations.add(obs);
            }
        } else if (nbt.contains("sensors")) {
            // Ancien format — un seul bloc "sensors" au niveau racine
            NbtList senList = nbt.getList("sensors", 10);
            Map<BlockPos, Boolean> obs = new HashMap<>();
            for (int i = 0; i < senList.size(); i++) {
                NbtCompound e = senList.getCompound(i);
                obs.put(BlockPos.fromLong(e.getLong("pos")), e.getBoolean("val"));
            }
            observations.add(obs);
        }

        // Garantit au moins une observation vide plutôt que null
        if (observations.isEmpty()) observations.add(new HashMap<>());

        return new TestCase(name, injectors, observations);
    }
}