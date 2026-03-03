package top.mrxiaom.sweet.playermarket.api.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用于从 CraftEngine 这样的特殊插件中提取物品名的物品名称提供器
 */
public interface ItemNameProvider {
    default int priority() {
        return 1000;
    }
    @Nullable
    String getDisplayName(@NotNull ItemStack item);
}
