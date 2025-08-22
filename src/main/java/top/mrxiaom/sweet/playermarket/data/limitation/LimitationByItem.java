package top.mrxiaom.sweet.playermarket.data.limitation;

import com.google.common.collect.Sets;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.limitation.item.*;

import java.util.*;

public class LimitationByItem extends BaseLimitation {
    private final @NotNull ItemMatcherType matcherType;
    private final @NotNull List<ItemMatcher> matchers = new ArrayList<>();
    protected LimitationByItem(SweetPlayerMarket plugin, ConfigurationSection config) {
        super(plugin, config);

        String matcherTypeStr = config.getString("matcher-type");
        ItemMatcherType matcherType = parseMatcherType(matcherTypeStr);
        if (matcherType == null) {
            plugin.warn("[limitation] 输入的物品匹配器类型 " + matcherTypeStr + " 无效，已使用缺省值 ANY");
        }
        this.matcherType = matcherType == null ? TypeAny.INSTANCE : matcherType;

        List<ConfigurationSection> matchers = Util.getSectionList(config, "matchers");
        for (ConfigurationSection s : matchers) {
            ItemMatcher matcher = parseMatcher(s);
            if (matcher == null) {
                plugin.warn("[limitation] 出现无效的物品匹配器 " + String.join(", ", s.getKeys(false)));
            } else {
                this.matchers.add(matcher);
            }
        }
    }

    /**
     * 获取物品是否匹配该限制条件
     * @param item 物品
     */
    public boolean isItemMatch(@Nullable ItemStack item) {
        if (item == null || AdventureItemStack.isEmpty(item)) return false;
        return matcherType.match(matchers, item);
    }

    private static ItemMatcherType parseMatcherType(String str) {
        if (str == null || str.isEmpty()) return null;
        if (str.equalsIgnoreCase("ALL")) {
            return TypeAll.INSTANCE;
        }
        if (str.equalsIgnoreCase("ANY")) {
            return TypeAny.INSTANCE;
        }
        if (str.toUpperCase().startsWith("ONLY ")) {
            Integer num = Util.parseInt(str.substring(5)).orElse(null);
            if (num != null) {
                return new TypeOnlyNum(num);
            }
        }
        return null;
    }

    private static ItemMatcher parseMatcher(ConfigurationSection config) {
        if (config.contains("any-nbt-tag")) {
            List<String> nbtTags = config.getStringList("any-nbt-tag");
            if (!nbtTags.isEmpty()) {
                return new MatcherAnyNbtTag(Sets.newHashSet(nbtTags));
            }
        }
        if (config.contains("nbt-strings")) {
            ConfigurationSection section = config.getConfigurationSection("nbt-strings");
            if (section != null) {
                Map<String, String> nbtStrings = new HashMap<>();
                for (String key : section.getKeys(false)) {
                    String value = section.getString(key);
                    if (value != null) {
                        nbtStrings.put(key, value);
                    }
                }
                if (!nbtStrings.isEmpty()) {
                    return new MatcherNbtStrings(nbtStrings);
                }
            }
        }
        if (config.contains("nbt-ints")) {
            ConfigurationSection section = config.getConfigurationSection("nbt-ints");
            if (section != null) {
                Map<String, Integer> nbtInts = new HashMap<>();
                for (String key : section.getKeys(false)) {
                    nbtInts.put(key, section.getInt(key));
                }
                if (!nbtInts.isEmpty()) {
                    return new MatcherNbtInts(nbtInts);
                }
            }
        }
        if (config.contains("any-materials")) {
            Set<Material> materials = new HashSet<>();
            for (String s : config.getStringList("any-materials")) {
                Material material = Util.valueOrNull(Material.class, s);
                if (material != null) {
                    materials.add(material);
                }
            }
            if (!materials.isEmpty()) {
                return new MatcherAnyMaterials(materials);
            }
        }
        if (config.contains("contains-name")) {
            List<String> strings = config.getStringList("contains-name");
            if (!strings.isEmpty()) {
                return new MatcherContainsName(Sets.newHashSet(strings));
            }
        }
        if (config.contains("contains-lore")) {
            List<String> strings = config.getStringList("contains-lore");
            if (!strings.isEmpty()) {
                return new MatcherContainsLore(Sets.newHashSet(strings));
            }
        }
        return null;
    }

    public static LimitationByItem of(SweetPlayerMarket plugin, ConfigurationSection config) {
        return new LimitationByItem(plugin, config);
    }
}
