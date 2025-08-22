package top.mrxiaom.sweet.playermarket.data.limitation.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;

import java.util.Set;

public class MatcherContainsLore implements ItemMatcher {
    public final Set<String> strings;
    public MatcherContainsLore(Set<String> strings) {
        this.strings = strings;
    }


    @Override
    public boolean match(@NotNull ItemStack item) {
        String lore = String.join("\n", AdventureItemStack.getItemLoreAsMiniMessage(item));
        for (String str : strings) {
            if (lore.contains(str)) {
                return true;
            }
        }
        return false;
    }
}
