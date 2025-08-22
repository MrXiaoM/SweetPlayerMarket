package top.mrxiaom.sweet.playermarket.data.limitation.item;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MatcherNbtInts implements ItemMatcher {
    public final Map<String, Integer> nbtInts;
    public MatcherNbtInts(Map<String, Integer> nbtInts) {
        this.nbtInts = nbtInts;
    }

    @Override
    public boolean match(@NotNull ItemStack item) {
        return NBT.get(item, nbt -> {
            for (Map.Entry<String, Integer> entry : nbtInts.entrySet()) {
                Integer value = nbt.getInteger(entry.getKey());
                if (!entry.getValue().equals(value)) {
                    return false;
                }
            }
            return true;
        });
    }
}
