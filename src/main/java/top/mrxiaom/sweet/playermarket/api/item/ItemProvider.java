package top.mrxiaom.sweet.playermarket.api.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ItemProvider {
    int priority();
    @Nullable ItemStack get(String inputText);
}
