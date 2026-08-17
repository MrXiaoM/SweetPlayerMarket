package top.mrxiaom.sweet.playermarket.depend;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.item.ItemNameProvider;
import top.mrxiaom.sweet.playermarket.api.item.ItemProvider;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

@AutoRegister(requirePlugins = "CraftEngine")
public class DependencyCraftEngine extends AbstractModule implements ItemProvider, ItemNameProvider {
    public DependencyCraftEngine(SweetPlayerMarket plugin) {
        super(plugin);
        if (Util.isPresent("net.momirealms.craftengine.bukkit.item.BukkitItemDefinition")) {
            plugin.registerItemProvider(this);
            plugin.registerItemNameProvider(this);
            info("已挂钩 CraftEngine");
        } else {
            warn("CraftEngine 版本过低，请升级到 26.5 或以上");
        }
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("ce:")) {
            String itemId = inputText.substring(3);
            BukkitItemDefinition customItem = CraftEngineItems.byId(Key.of(itemId));
            return customItem == null ? null : customItem.buildBukkitItem();
        }
        return null;
    }

    @Override
    public @Nullable String getDisplayName(@NotNull ItemStack itemStack) {
        BukkitItem bukkitItem = BukkitAdaptor.adapt(itemStack);
        return BukkitItemManager.instance().s2c(bukkitItem.copy(), null)
                .flatMap(Item::hoverNameComponent)
                .map(AdventureHelper::componentToMiniMessage)
                .orElse(null);
    }

    @Override
    public void onDisable() {
        plugin.unregisterItemProvider(this);
        plugin.unregisterItemNameProvider(this);
    }
}
