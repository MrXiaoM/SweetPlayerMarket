package top.mrxiaom.sweet.playermarket.data.limitation.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class MatcherAnyMaterials implements ItemMatcher {
    public final Set<Material> materials;
    public MatcherAnyMaterials(Set<Material> materials) {
        this.materials = materials;
    }

    @Override
    public boolean match(@NotNull ItemStack item) {
        Material type = item.getType();
        for (Material material : materials) {
            if (material.equals(type)) {
                return true;
            }
        }
        return false;
    }
}
