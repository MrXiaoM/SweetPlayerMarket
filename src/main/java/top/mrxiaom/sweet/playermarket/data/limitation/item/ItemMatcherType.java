package top.mrxiaom.sweet.playermarket.data.limitation.item;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface ItemMatcherType {
    boolean match(List<ItemMatcher> matchers, ItemStack item);
}
