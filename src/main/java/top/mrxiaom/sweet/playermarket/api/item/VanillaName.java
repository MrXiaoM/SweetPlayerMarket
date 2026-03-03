package top.mrxiaom.sweet.playermarket.api.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;

public class VanillaName implements ItemNameProvider {
    public static final VanillaName INSTANCE = new VanillaName();
    @Override
    public int priority() {
        return 2000;
    }
    @Override
    public @Nullable String getDisplayName(@NotNull ItemStack item) {
        String displayName = AdventureItemStack.getItemDisplayNameAsMiniMessage(item);
        if (displayName != null) {
            return displayName.replace("&", "&&");
        }
        return null;
    }
}
