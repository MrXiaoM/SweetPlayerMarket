package top.mrxiaom.sweet.playermarket.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;

import java.util.UUID;
import java.util.logging.Logger;

public class Utils {

    private static int first(Inventory inv, ItemStack item) {
        if (item == null) {
            return -1;
        } else {
            ItemStack[] inventory = inv.getContents(); // modified
            int i = 0;
            while (true) {
                if (i >= inventory.length) return -1;
                if (inventory[i] != null && item.isSimilar(inventory[i])) break;
                ++i;
            }
            return i;
        }
    }

    public static void takeItem(Player player, ItemStack sample, int count) {
        PlayerInventory inv = player.getInventory();
        int toDelete = count;
        while (true) {
            int first = first(inv, sample);
            if (first == -1) {
                Logger logger = SweetPlayerMarket.getInstance().getLogger();
                logger.warning("预料中的问题，在扣除玩家 " + player.getName() + " 的物品 " + sample.getType().name() + " 时，有 " + toDelete + " 个物品没有成功扣除");
                break;
            }

            ItemStack itemStack = inv.getItem(first);
            if (itemStack == null) continue;
            int amount = itemStack.getAmount();
            if (amount <= toDelete) {
                toDelete -= amount;
                inv.setItem(first, null);
            } else {
                itemStack.setAmount(amount - toDelete);
                inv.setItem(first, itemStack);
                toDelete = 0;
            }
            if (toDelete <= 0) break;
        }
    }

    public static UUID parseUUID(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
