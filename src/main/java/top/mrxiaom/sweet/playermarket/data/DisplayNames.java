package top.mrxiaom.sweet.playermarket.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IEconomyResolver;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.func.AbstractModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoRegister
public class DisplayNames extends AbstractModule {
    private final Map<EnumMarketType, String> marketTypeNames = new HashMap<>();
    private final Map<EnumSort, String> sortNames = new HashMap<>();
    private final Map<String, String> currencyMPoints = new HashMap<>();
    private final Map<String, String> columnNames = new HashMap<>();
    private final List<String> columnList = new ArrayList<>();
    private String currencyVault, currencyPlayerPoints, currencyAll, marketTypeAll;
    private final boolean supportTranslatable = Util.isPresent("org.bukkit.Translatable");
    private final boolean supportLangUtils = Util.isPresent("com.meowj.langutils.lang.LanguageHelper");

    public DisplayNames(SweetPlayerMarket plugin) {
        super(plugin);
    }

    /**
     * 获取商品类型的展示名
     * @param type 商品类型
     */
    @NotNull
    public String getMarketTypeName(@Nullable EnumMarketType type) {
        if (type == null) {
            return marketTypeAll;
        }
        String s = marketTypeNames.get(type);
        return s != null ? s : type.name();
    }

    public String getCurrencyNameVault() {
        return currencyVault;
    }

    public String getCurrencyNamePlayerPoints() {
        return currencyPlayerPoints;
    }

    public String getCurrencyNameMPoints(String sign) {
        return currencyMPoints.getOrDefault(sign, sign);
    }

    /**
     * 获取货币的展示名
     * @param currency 字符串形式的货币类型
     */
    @NotNull
    public String getCurrencyName(@Nullable String currency) {
        if (currency == null) {
            return currencyAll;
        }
        for (IEconomyResolver resolver : plugin.economyResolvers()) {
            String name = resolver.parseName(currency);
            if (name != null) {
                return name;
            }
        }
        return currency;
    }

    /**
     * 获取货币的展示名
     * @param currency 货币接口实现
     */
    @NotNull
    public String getCurrencyName(@Nullable IEconomy currency) {
        if (currency == null) {
            return currencyAll;
        }
        for (IEconomyResolver resolver : plugin.economyResolvers()) {
            String name = resolver.getName(currency);
            if (name != null) {
                return name;
            }
        }
        return currency.getName();
    }

    /**
     * 获取数据表列名的展示名
     * @param column 列
     */
    @NotNull
    public String getColumnName(@NotNull String column) {
        return columnNames.getOrDefault(column, column);
    }

    /**
     * 获取排序类型的展示名
     * @param sort 排序类型
     */
    @NotNull
    public String getSortName(@NotNull EnumSort sort) {
        String s = sortNames.get(sort);
        return s != null ? s : sort.name();
    }

    /**
     * 格式化金钱数值为字符串
     * @param money 输入的金额
     * @return 格式化后的金额
     */
    public String formatMoney(double money) {
        // TODO: 支持自定义简写（例如 1.23万）的同时，保证数值准确性
        return String.format("%.2f", money).replace(".00", "");
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        ConfigurationSection section;

        marketTypeNames.clear();
        marketTypeAll = "";
        section = config.getConfigurationSection("display-names.market-types");
        if (section != null) for (String key : section.getKeys(false)) {
            if (key.equals("all")) {
                marketTypeAll = section.getString(key);
                continue;
            }
            EnumMarketType type = Util.valueOr(EnumMarketType.class, key, null);
            if (type != null) {
                marketTypeNames.put(type, section.getString(key));
            }
        }
        currencyAll = config.getString("display-names.currency-types.all");
        currencyVault = config.getString("display-names.currency-types.vault");
        currencyPlayerPoints = config.getString("display-names.currency-types.points");
        currencyMPoints.clear();
        section = config.getConfigurationSection("display-names.currency-types.m-points");
        if (section != null) for (String key : section.getKeys(false)) {
            currencyMPoints.put(key, section.getString(key));
        }

        columnNames.clear();
        columnList.clear();
        section = config.getConfigurationSection("display-names.columns");
        if (section != null) for (String key : section.getKeys(false)) {
            columnNames.put(key, section.getString(key));
            columnList.add(key);
        }

        sortNames.clear();
        section = config.getConfigurationSection("display-names.sort");
        if (section != null) for (String key : section.getKeys(false)) {
            EnumSort sort = Util.valueOr(EnumSort.class, key, null);
            if (sort != null) {
                sortNames.put(sort, section.getString(key));
            }
        }
    }

    /**
     * 获取可用于排序的数据表列名
     */
    public List<String> columnList() {
        return columnList;
    }

    /**
     * 获取物品展示名。如果没有展示名，则返回物品原名
     * @param item 物品
     * @see DisplayNames#get(ItemStack, Player)
     */
    public String getDisplayName(ItemStack item, Player player) {
        String displayName = AdventureItemStack.getItemDisplayNameAsMiniMessage(item);
        if (displayName != null) {
            return displayName.replace("&", "&&");
        }
        return get(item, player);
    }

    /**
     * 获取物品原名
     * @param item 物品
     * @param player 玩家实例，用于指定语言。如果为 <code>null</code>，则使用默认语言
     */
    public String get(@NotNull ItemStack item, @Nullable Player player) {
        if (supportTranslatable) {
            return "<lang:" + item.getTranslationKey() + ">";
        }
        if (supportLangUtils) {
            if (player == null) {
                return com.meowj.langutils.lang.LanguageHelper.getItemName(item, "fallback");
            } else {
                return com.meowj.langutils.lang.LanguageHelper.getItemName(item, player);
            }
        }
        // 条件最糟糕时使用的方案: 将 _ 替换为空格，并使得每个单词的首字母大写
        String[] words = item.getType().toString().toLowerCase().split("_");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() == 1) {
                words[i] = word.toUpperCase();
            } else {
                words[i] = word.substring(0, 1).toUpperCase() + word.substring(1);
            }
        }
        return String.join(" ", words);
    }

    public static DisplayNames inst() {
        return instanceOf(DisplayNames.class);
    }
}
