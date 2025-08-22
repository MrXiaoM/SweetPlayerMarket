package top.mrxiaom.sweet.playermarket.data.limitation.item;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TypeAll implements ItemMatcherType {
    public static final TypeAll INSTANCE = new TypeAll();
    private TypeAll() {}
    @Override
    public boolean match(List<ItemMatcher> matchers, ItemStack item) {
        if (matchers.isEmpty()) return false;
        for (ItemMatcher matcher : matchers) {
            if (!matcher.match(item)) {
                return false;
            }
        }
        return true;
    }
}
