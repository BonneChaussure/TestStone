package org.BonneChaussure.tests;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public record TestCase(String name, Map<BlockPos, Boolean> injectorValues, Map<BlockPos, Boolean> sensorExpected) {

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", name);

        NbtList injList = new NbtList();
        injectorValues.forEach((pos, val) -> {
            NbtCompound entry = new NbtCompound();
            entry.putLong("pos", pos.asLong());
            entry.putBoolean("val", val);
            injList.add(entry);
        });
        nbt.put("injectors", injList);

        NbtList senList = new NbtList();
        sensorExpected.forEach((pos, val) -> {
            NbtCompound entry = new NbtCompound();
            entry.putLong("pos", pos.asLong());
            entry.putBoolean("val", val);
            senList.add(entry);
        });
        nbt.put("sensors", senList);

        return nbt;
    }

    public static TestCase fromNbt(NbtCompound nbt) {
        String name = nbt.getString("name");

        Map<BlockPos, Boolean> injectors = new HashMap<>();
        NbtList injList = nbt.getList("injectors", 10);
        for (int i = 0; i < injList.size(); i++) {
            NbtCompound e = injList.getCompound(i);
            injectors.put(BlockPos.fromLong(e.getLong("pos")), e.getBoolean("val"));
        }

        Map<BlockPos, Boolean> sensors = new HashMap<>();
        NbtList senList = nbt.getList("sensors", 10);
        for (int i = 0; i < senList.size(); i++) {
            NbtCompound e = senList.getCompound(i);
            sensors.put(BlockPos.fromLong(e.getLong("pos")), e.getBoolean("val"));
        }

        return new TestCase(name, injectors, sensors);
    }
}
