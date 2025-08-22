package top.mrxiaom.sweet.playermarket.data.limitation.item;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TypeAny implements ItemMatcherType {
    public static final TypeAny INSTANCE = new TypeAny();
    private TypeAny() {}
    @Override
    public boolean match(List<ItemMatcher> matchers, ItemStack item) {
        for (ItemMatcher matcher : matchers) {
            if (matcher.match(item)) {
                return true;
            }
        }
        return false;
    }
}
