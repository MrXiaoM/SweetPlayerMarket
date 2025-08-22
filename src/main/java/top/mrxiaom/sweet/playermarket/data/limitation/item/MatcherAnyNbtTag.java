package top.mrxiaom.sweet.playermarket.data.limitation.item;

import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class MatcherAnyNbtTag implements ItemMatcher {
    public final Set<String> nbtTags;
    public MatcherAnyNbtTag(Set<String> nbtTags) {
        this.nbtTags = nbtTags;
    }

    @Override
    public boolean match(@NotNull ItemStack item) {
        return NBT.get(item, nbt -> {
            for (String tag : nbtTags) {
                if (nbt.hasTag(tag)) {
                    return true;
                }
            }
            return false;
        });
    }
}
