package top.mrxiaom.sweet.playermarket.api.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;

public class VanillaItem implements ItemProvider {
    public static final VanillaItem INSTANCE = new VanillaItem();
    private VanillaItem() {}

    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        Material material = Util.valueOrNull(Material.class, inputText);
        if (material != null) {
            return new ItemStack(material);
        }
        return null;
    }
}
