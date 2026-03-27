package top.mrxiaom.sweet.playermarket.depend;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.processor.ItemNameProcessor;
import net.momirealms.craftengine.core.item.processor.ItemProcessor;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.item.ItemNameProvider;
import top.mrxiaom.sweet.playermarket.api.item.ItemProvider;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

@AutoRegister(requirePlugins = "CraftEngine")
public class DependencyCraftEngine extends AbstractModule implements ItemProvider, ItemNameProvider {
    private static final String MINECRAFT_NAMESPACE = "minecraft";
    public DependencyCraftEngine(SweetPlayerMarket plugin) {
        super(plugin);
        plugin.registerItemProvider(this);
        plugin.registerItemNameProvider(this);
        info("已挂钩 CraftEngine");
    }

    private static Key of(String namespacedId) {
        String[] strings = new String[]{MINECRAFT_NAMESPACE, namespacedId};
        int i = namespacedId.indexOf(':');
        if (i >= 0) {
            strings[1] = namespacedId.substring(i + 1);
            if (i >= 1) {
                strings[0] = namespacedId.substring(0, i);
            }
        }
        return of(strings);
    }

    private static Key of(String[] id) {
        return new Key(id[0], id[1]);
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("ce:")) {
            String itemId = inputText.substring(3);
            CustomItem<ItemStack> customItem = CraftEngineItems.byId(of(itemId));
            return customItem == null ? null : customItem.buildItemStack();
        }
        return null;
    }

    @Override
    public @Nullable String getDisplayName(@NotNull ItemStack item) {
        CustomItem<ItemStack> customItem = CraftEngineItems.byItemStack(item);
        if (customItem != null && !customItem.isEmpty()) {
            // 有自定义名字就用自定义名字，没自定义名字再用语言文本
            String displayName = AdventureItemStack.getItemDisplayNameAsMiniMessage(item);
            if (displayName != null) {
                return displayName.replace("&", "&&");
            }
            // 如果还有通过物品处理器添加的名字，优先返回
            for (ItemProcessor processor : customItem.dataModifiers()) {
                if (processor instanceof ItemNameProcessor) {
                    return ((ItemNameProcessor) processor).itemName();
                }
            }
            // 最后再返回翻译键
            return "<lang:" + customItem.translationKey() + ">";
        }
        return null;
    }

    @Override
    public void onDisable() {
        plugin.unregisterItemProvider(this);
    }
}
