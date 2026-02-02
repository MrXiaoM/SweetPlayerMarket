package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.ItemTagResolver;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.tag.TagFilter;

import java.io.File;
import java.util.*;

@AutoRegister
public class ItemTagManager extends AbstractModule implements ItemTagResolver {
    private final List<TagFilter> tagFilterList = new ArrayList<>();
    private final Map<String, String> tagDisplayNames = new HashMap<>();
    private final ListPair<String, Object> tagDisplayNamesReplacement = new ListPair<>();
    private String noTagDisplayName = "";
    public ItemTagManager(SweetPlayerMarket plugin) {
        super(plugin);
        plugin.itemTagResolver(this);
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        File file = plugin.resolve("./tag-filter.yml");
        if (!file.exists()) {
            plugin.saveResource("tag-filter.yml", file);
        }
        FileConfiguration config = plugin.resolveGotoFlag(ConfigUtils.load(file));

        noTagDisplayName = config.getString("no-tag-display-name", "");
        tagFilterList.clear();
        tagDisplayNames.clear();
        tagDisplayNamesReplacement.clear();
        ConfigurationSection section = config.getConfigurationSection("tag-filter-map");
        if (section != null) for (String tag : section.getKeys(false)) {
            ConfigurationSection properties = section.getConfigurationSection(tag);
            if (properties == null) continue;
            String displayName = properties.getString("display-name");
            if (displayName != null) {
                tagDisplayNames.put(tag, displayName);
                tagDisplayNamesReplacement.add("%tag_" + tag + "%", displayName);
            } else {
                tagDisplayNamesReplacement.add("%tag_" + tag + "%", tag);
            }
            if (tag.equalsIgnoreCase("default")) continue;
            try {
                tagFilterList.add(new TagFilter(plugin, tag, properties));
            } catch (Throwable t) {
                warn("[tag-filter] 读取标签过滤器 " + tag + " 时出现异常", t);
            }
        }
        tagFilterList.sort(Comparator.comparingInt(TagFilter::priority));
    }

    @Override
    public @Nullable String resolve(@NotNull MarketItem item) {
        ConfigurationSection params = item.params();
        if (params.contains("auto-deploy.tag")) {
            return params.getString("auto-deploy.tag");
        }
        for (TagFilter filter : tagFilterList) {
            if (filter.resolve(item)) {
                return filter.tag();
            }
        }
        return "default";
    }

    @NotNull
    public String getTagDisplayName(@Nullable String tag) {
        if (tag == null) {
            return noTagDisplayName;
        }
        return tagDisplayNames.getOrDefault(tag, tag);
    }

    @NotNull
    public ListPair<String, Object> getTagDisplayNames() {
        return tagDisplayNamesReplacement;
    }

    public static ItemTagManager inst() {
        return instanceOf(ItemTagManager.class);
    }
}
