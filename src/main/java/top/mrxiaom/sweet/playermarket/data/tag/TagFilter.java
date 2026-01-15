package top.mrxiaom.sweet.playermarket.data.tag;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.MaterialMeta;
import top.mrxiaom.sweet.playermarket.data.limitation.LimitationByItem;
import top.mrxiaom.sweet.playermarket.data.limitation.item.ItemMatcher;
import top.mrxiaom.sweet.playermarket.data.limitation.item.TypeAny;

import java.util.ArrayList;
import java.util.List;

public class TagFilter {
    private final String tag;
    private final int priority;
    private final @NotNull List<Material> materialList = new ArrayList<>();
    private final @NotNull List<MaterialMeta> materialMetaList = new ArrayList<>();
    private final @NotNull List<ItemMatcher> matchers = new ArrayList<>();

    public TagFilter(SweetPlayerMarket plugin, String tag, ConfigurationSection config) {
        this.tag = tag;
        this.priority = config.getInt("priority", 1000);

        for (String str : config.getStringList("material")) {
            Material material = Util.valueOrNull(Material.class, str);
            // 以免在旧版本找不到新版本物品时刷屏，不输出警告
            if (material != null) {
                materialList.add(material);
            }
        }

        for (String str : config.getStringList("material-meta")) {
            MaterialMeta meta = Util.valueOrNull(MaterialMeta.class, str.replace("-", "_"));
            if (meta == null) {
                plugin.warn("[tag-filter] 出现无效的物品特性 " + str);
                continue;
            }
            materialMetaList.add(meta);
        }

        List<ConfigurationSection> matchers = ConfigUtils.getSectionList(config, "matchers");
        for (ConfigurationSection s : matchers) {
            ItemMatcher matcher = LimitationByItem.parseMatcher(s);
            if (matcher == null) {
                plugin.warn("[tag-filter] 出现无效的物品匹配器 " + String.join(", ", s.getKeys(false)));
            } else {
                this.matchers.add(matcher);
            }
        }
    }

    public String tag() {
        return tag;
    }

    public int priority() {
        return priority;
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean resolve(MarketItem marketItem) {
        ItemStack item = marketItem.item();
        Material material = item.getType();
        if (material.equals(Material.AIR)) {
            return false;
        }
        if (materialList.contains(material)) {
            return true;
        }
        for (MaterialMeta meta : materialMetaList) {
            if (meta.check(material)) {
                return true;
            }
        }
        if (TypeAny.INSTANCE.match(matchers, item)) {
            return true;
        }
        return false;
    }
}
