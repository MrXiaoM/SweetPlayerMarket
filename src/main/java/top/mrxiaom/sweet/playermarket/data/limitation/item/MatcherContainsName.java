package top.mrxiaom.sweet.playermarket.data.limitation.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;

import java.util.Set;

public class MatcherContainsName implements ItemMatcher {
    public final Set<String> names;
    public MatcherContainsName(Set<String> names) {
        this.names = names;
    }


    @Override
    public boolean match(@NotNull ItemStack item) {
        String displayName = AdventureItemStack.getItemDisplayNameAsMiniMessage(item);
        if (displayName == null) return false;
        for (String name : names) {
            if (displayName.contains(name)) {
                return true;
            }
        }
        return false;
    }
}
