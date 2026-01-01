package top.mrxiaom.sweet.playermarket.depend.mythic;

import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class Mythic5 implements IMythic {
    private final MythicBukkit mythic = MythicBukkit.inst();
    @Override
    public Optional<ItemStack> getItem(String str) {
        if (str == null || str.isEmpty()) return Optional.empty();
        return mythic.getItemManager().getItem(str)
                .map(it -> it.generateItemStack(1))
                .map(BukkitAdapter::adapt);
    }
}
