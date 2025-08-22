package top.mrxiaom.sweet.playermarket.data.limitation.item;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TypeOnlyNum implements ItemMatcherType {
    private final int num;
    public TypeOnlyNum(int num) {
        this.num = num;
    }

    @Override
    public boolean match(List<ItemMatcher> matchers, ItemStack item) {
        int n = 0;
        for (ItemMatcher matcher : matchers) {
            if (matcher.match(item)) {
                if (++n >= num) return true;
            }
        }
        return false;
    }
}
