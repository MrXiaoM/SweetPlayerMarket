package top.mrxiaom.sweet.playermarket.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.func.AbstractGuiModule;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {

    @NotNull
    public static LoadedIcon requireIconNotNull(@NotNull AbstractGuiModule module, @Nullable String resourceFile, @Nullable LoadedIcon loaded, @NotNull String key) {
        if (loaded != null) return loaded;
        module.warn(module.warningPrefix() + " 无法在配置中找到 " + key + "，正在尝试从默认配置中读取");
        InputStream resource = resourceFile == null ? null : module.plugin.getResource(resourceFile);
        if (resource == null) {
            return LoadedIcon.load(new MemoryConfiguration());
        }
        YamlConfiguration config = new YamlConfiguration();
        try (InputStream stream = resource) {
            config.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (FileNotFoundException ignored) {
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + resourceFile, ex);
        }
        ConfigurationSection section = config.getConfigurationSection(key);
        return LoadedIcon.load(section == null ? new MemoryConfiguration() : section);
    }

    @Nullable
    public static <K, V> V get(@Nullable List<Pair<K, V>> list, @NotNull K key) {
        return getOrDefault(list, key, null);
    }
    public static <K, V> V getOrDefault(@Nullable List<Pair<K, V>> list, @NotNull K key, @Nullable V def) {
        if (list == null) return def;
        for (Pair<K, V> pair : list) {
            if (key.equals(pair.key())) {
                return pair.value();
            }
        }
        return def;
    }

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

    public static int getItemAmount(Player player, ItemStack sample) {
        int invAmount = 0;
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (ItemStack content : contents) {
            if (content != null && content.isSimilar(sample)) {
                invAmount += content.getAmount();
            }
        }
        return invAmount;
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
