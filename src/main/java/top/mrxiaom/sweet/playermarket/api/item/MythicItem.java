package top.mrxiaom.sweet.playermarket.api.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.depend.mythic.IMythic;

public class MythicItem implements ItemProvider {
    private final IMythic mythic;
    public MythicItem(IMythic mythic) {
        this.mythic = mythic;
    }

    @Override
    public int priority() {
        return 990;
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("mythic:")) {
            String mythicId = inputText.substring(7);
            return mythic.getItem(mythicId).orElse(null);
        }
        return null;
    }
}
