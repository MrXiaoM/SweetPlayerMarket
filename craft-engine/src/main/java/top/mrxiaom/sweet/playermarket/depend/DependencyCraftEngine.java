package top.mrxiaom.sweet.playermarket.depend;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.item.ItemProvider;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

@AutoRegister(requirePlugins = "CraftEngine")
public class DependencyCraftEngine extends AbstractModule implements ItemProvider {
    public DependencyCraftEngine(SweetPlayerMarket plugin) {
        super(plugin);
        plugin.registerItemProvider(this);
        info("已挂钩 CraftEngine");
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("ce:")) {
            String itemId = inputText.substring(3);
            CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of(itemId));
            return customItem == null ? null : customItem.buildItemStack();
        }
        return null;
    }

    @Override
    public void onDisable() {
        plugin.unregisterItemProvider(this);
    }
}
