package top.mrxiaom.sweet.playermarket.data.limitation;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class BaseLimitation {
    private final @NotNull Map<EnumMarketType, CreateCost> createCostByType = new HashMap<>();
    private final @Nullable CreateCost createCostAll;
    private final @NotNull Set<EnumMarketType> typeBlackList = new HashSet<>();
    private final @NotNull Set<EnumMarketType> typeWhiteList = new HashSet<>();
    private final @NotNull Set<String> currencyBlackList = new HashSet<>();
    private final @NotNull Set<String> currencyWhiteList = new HashSet<>();
    protected BaseLimitation(SweetPlayerMarket plugin, ConfigurationSection config) {
        ConfigurationSection section;

        CreateCost createCostAll = null;
        section = config.getConfigurationSection("create-cost");
        if (section != null) for (String key : section.getKeys(false)) {
            IEconomy currency;
            String currencyName = section.getString(key + ".currency", "");
            if ("INHERIT".equalsIgnoreCase(currencyName)) {
                currency = null;
            } else {
                currency = plugin.parseEconomy(currencyName);
                if (currency == null) {
                    plugin.warn("[limitation] 找不到货币类型 " + currencyName);
                    continue;
                }
            }
            Function<Double, Double> moneyFunc;
            String moneyStr = section.getString(key + ".money", "");
            if (moneyStr.endsWith("%")) {
                Double percent = Util.getPercentAsDouble(moneyStr, null);
                if (percent == null) {
                    plugin.warn("[limitation] 输入的货币数量百分比 " + moneyStr + " 不正确");
                    continue;
                }
                moneyFunc = total -> total * percent;
            } else {
                Double money = Util.parseDouble(moneyStr).orElse(null);
                if (money == null) {
                    plugin.warn("[limitation] 输入的货币数量 " + moneyStr + " 不正确");
                    continue;
                }
                moneyFunc = total -> money;
            }
            CreateCost createCost = new CreateCost(currency, moneyFunc);
            if (key.equalsIgnoreCase("ALL")) {
                createCostAll = createCost;
            } else {
                EnumMarketType type = Util.valueOrNull(EnumMarketType.class, key);
                if (type == null) {
                    plugin.warn("[limitation] 找不到商品类型 " + key);
                }
                createCostByType.put(type, createCost);
            }
        }
        this.createCostAll = createCostAll;

        for (String s : config.getStringList("shop-type-blacklist")) {
            EnumMarketType type = Util.valueOrNull(EnumMarketType.class, s);
            if (type != null) {
                typeBlackList.add(type);
            }
        }
        for (String s : config.getStringList("shop-type-whitelist")) {
            EnumMarketType type = Util.valueOrNull(EnumMarketType.class, s);
            if (type != null) {
                typeWhiteList.add(type);
            }
        }
        this.currencyBlackList.addAll(config.getStringList("currency-blacklist"));
        this.currencyWhiteList.addAll(config.getStringList("currency-whitelist"));
    }

    /**
     * 获取商品类型所需的手续费
     * @param type 商品类型
     * @return <code>null</code> 代表无需手续费
     */
    @Nullable
    public CreateCost getCreateCost(EnumMarketType type) {
        return createCostByType.getOrDefault(type, createCostAll);
    }

    /**
     * 获取是否允许使用该商品类型上架商品
     * @param type 商品类型
     */
    public boolean canUseMarketType(EnumMarketType type) {
        if (typeBlackList.contains(type)) {
            return false;
        }
        if (!typeWhiteList.isEmpty()) {
            return typeWhiteList.contains(type);
        }
        return true;
    }

    /**
     * 获取是否允许使用该货币类型上架商品
     * @param currency
     * @return
     */
    public boolean canUseCurrency(@NotNull IEconomy currency) {
        String id = currency.id();
        if (id.contains(":")) {
            String type = id.split(":", 2)[0];
            if (currencyBlackList.contains(type) || currencyBlackList.contains(id)) {
                return false;
            }
            if (!currencyWhiteList.isEmpty()) {
                if (currencyWhiteList.contains(type)) return true;
                return currencyWhiteList.contains(id);
            }
        } else {
            if (currencyBlackList.contains(id)) {
                return false;
            }
            if (!currencyWhiteList.isEmpty()) {
                return currencyWhiteList.contains(id);
            }
        }
        return true;
    }

    public static BaseLimitation of(SweetPlayerMarket plugin, ConfigurationSection config) {
        return new BaseLimitation(plugin, config);
    }
}
