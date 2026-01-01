package top.mrxiaom.sweet.playermarket.depend.mythic;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class Mythic4 implements IMythic {
    private final MythicMobs mythic = MythicMobs.inst();
    @Override
    public Optional<ItemStack> getItem(String str) {
        if (str == null || str.isEmpty()) return Optional.empty();
        return mythic.getItemManager().getItem(str)
                .map(it -> it.generateItemStack(1))
                .map(BukkitAdapter::adapt);
    }
}
