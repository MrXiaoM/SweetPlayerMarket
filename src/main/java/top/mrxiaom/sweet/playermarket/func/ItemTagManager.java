package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.ItemTagResolver;
import top.mrxiaom.sweet.playermarket.data.MarketItem;

import java.io.File;

@AutoRegister
public class ItemTagManager extends AbstractModule implements ItemTagResolver {
    public ItemTagManager(SweetPlayerMarket plugin) {
        super(plugin);
        plugin.itemTagResolver(this);
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        File file = plugin.resolve("./item-tags.yml");
        // TODO: 读取分类配置
        if (!file.exists()) {

        }
    }

    @Override
    public @Nullable String resolve(@NotNull MarketItem item) {
        Material type = item.item().getType();
        // TODO: 商品分类
        return "default";
    }

    public static ItemTagManager inst() {
        return instanceOf(ItemTagManager.class);
    }
}
