package top.mrxiaom.sweet.playermarket.data.limitation.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ItemMatcher {
    boolean match(@NotNull ItemStack item);
}
