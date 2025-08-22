package top.mrxiaom.sweet.playermarket.data.limitation.item;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MatcherNbtStrings implements ItemMatcher {
    public final Map<String, String> nbtStrings;
    public MatcherNbtStrings(Map<String, String> nbtStrings) {
        this.nbtStrings = nbtStrings;
    }

    @Override
    public boolean match(@NotNull ItemStack item) {
        return NBT.get(item, nbt -> {
            for (Map.Entry<String, String> entry : nbtStrings.entrySet()) {
                String value = nbt.getString(entry.getKey());
                if (!entry.getValue().equals(value)) {
                    return false;
                }
            }
            return true;
        });
    }
}
