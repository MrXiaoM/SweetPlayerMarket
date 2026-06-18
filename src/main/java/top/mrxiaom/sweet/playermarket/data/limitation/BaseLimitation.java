package top.mrxiaom.sweet.playermarket.data.limitation;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.DisplayNames;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class BaseLimitation {
    private final @NotNull Map<EnumMarketType, CreateCost> createCostByType = new HashMap<>();
    private final @Nullable CreateCost createCostAll;
    private final @NotNull List<String> description;
    private final @NotNull List<String> createCostDescription;
    private final @NotNull Set<EnumMarketType> typeBlackList = new HashSet<>();
    private final @NotNull Set<EnumMarketType> typeWhiteList = new HashSet<>();
    private final @NotNull Set<String> currencyBlackList = new HashSet<>();
    private final @NotNull Set<String> currencyWhiteList = new HashSet<>();
    protected BaseLimitation(SweetPlayerMarket plugin, ConfigurationSection config) {
        ConfigurationSection section;

        CreateCost createCostAll = null;
        section = config.getConfigurationSection("create-cost");
        if (section != null) for (String key : section.getKeys(false)) {

            CreateCost createCost = parseCreateCost(plugin, section.getConfigurationSection(key));
            if (createCost == null) continue;

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
        this.description = config.getStringList("description");
        this.createCostDescription = config.getStringList("create-cost-description");

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

    private static CreateCost parseCreateCost(SweetPlayerMarket plugin, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        IEconomy currency;
        String currencyName = section.getString("currency", "");
        if ("INHERIT".equalsIgnoreCase(currencyName)) {
            currency = null;
        } else {
            currency = plugin.parseEconomy(currencyName);
            if (currency == null) {
                plugin.warn("[limitation] 找不到货币类型 " + currencyName);
                return null;
            }
        }
        Function<Double, Double> moneyFunc;
        if (section.isList("money")) {
            List<Function<Double, Double>> list = new ArrayList<>();
            boolean error = false;
            for (String moneyStr : section.getStringList("money")) {
                Function<Double, Double> result = parseMoneyFunc(plugin, moneyStr);
                if (result != null) {
                    list.add(result);
                } else {
                    error = true;
                }
            }
            if (error) {
                return null;
            }
            moneyFunc = total -> {
                double result = 0.0;
                for (Function<Double, Double> func : list) {
                    result += func.apply(total);
                }
                return result;
            };
        } else {
            String moneyStr = section.getString("money", "");
            Function<Double, Double> result = parseMoneyFunc(plugin, moneyStr);
            if (result == null) {
                return null;
            }
            moneyFunc = result;
        }

        List<CreateCost> more = new ArrayList<>();
        List<ConfigurationSection> sectionList = ConfigUtils.getSectionList(section, "more");
        for (ConfigurationSection section1 : sectionList) {
            CreateCost createCost = parseCreateCost(plugin, section1);
            if (createCost != null) {
                more.add(createCost);
            }
        }

        return new CreateCost(currency, moneyFunc, more);
    }

    private static Function<Double, Double> parseMoneyFunc(SweetPlayerMarket plugin, String moneyStr) {
        if (moneyStr.endsWith("%")) {
            Double percent = ConfigUtils.getPercentAsDouble(moneyStr, null);
            if (percent == null) {
                plugin.warn("[limitation] 输入的货币数量百分比 " + moneyStr + " 不正确");
                return null;
            }
            return total -> total * percent;
        } else {
            Double money = Util.parseDouble(moneyStr).orElse(null);
            if (money == null) {
                plugin.warn("[limitation] 输入的货币数量 " + moneyStr + " 不正确");
                return null;
            }
            return total -> money;
        }
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

    public @NotNull List<String> getDescription() {
        return description;
    }

    public @NotNull List<String> getBakedDescription(@Nullable CreateCost cost, @NotNull IEconomy economy, double totalMoney) {
        return getBakedDescription(new ListPair<>(), cost, economy, totalMoney);
    }

    public @NotNull List<String> getBakedDescription(@NotNull List<Pair<String, Object>> r, @Nullable CreateCost cost, @NotNull IEconomy economy, double totalMoney) {
        if (description.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> lore = new ArrayList<>();
        addDescriptionReplacements(r, cost, economy, totalMoney);
        for (String line : description) {
            if (line.equals("create costs")) {
                DisplayNames displayNames = DisplayNames.inst();
                Map<IEconomy, Double> costMap = new HashMap<>();
                if (cost != null) {
                    cost.collectCosts(costMap, economy, totalMoney);
                }
                costMap.forEach((currency, moneyValue) -> {
                    ListPair<String, Object> r1 = new ListPair<>();
                    String currencyName = displayNames.getCurrencyName(currency);
                    String money = displayNames.formatMoney(moneyValue);
                    r1.add(Pair.of("%currency%", currencyName));
                    r1.add(Pair.of("%money%", money));
                    r1.addAll(r);
                    lore.addAll(Pair.replace(createCostDescription, r1));
                });
                continue;
            }
            lore.add(Pair.replace(line, r));
        }
        return lore;
    }

    public void addDescriptionReplacements(@NotNull List<Pair<String, Object>> r, @Nullable CreateCost cost, @NotNull IEconomy economy, double totalMoney) {
        Map<IEconomy, Double> costMap = new HashMap<>();
        if (cost != null) {
            cost.collectCosts(costMap, economy, totalMoney);
        }
        addDescriptionReplacements(r, costMap, economy, totalMoney);
    }

    public void addDescriptionReplacements(@NotNull List<Pair<String, Object>> r, @NotNull Map<IEconomy, Double> costMap, @NotNull IEconomy economy, double totalMoney) {
        DisplayNames displayNames = DisplayNames.inst();
        AtomicBoolean addedFirstCost = new AtomicBoolean(false);
        costMap.forEach((currency, moneyValue) -> {
            if (addedFirstCost.compareAndSet(false, true)) {
                // 为第一种货币添加变量，以支持旧的配置格式
                String currencyName = currency == null ? "" : displayNames.getCurrencyName(currency);
                String money = displayNames.formatMoney(moneyValue);
                r.add(Pair.of("%currency%", currencyName));
                r.add(Pair.of("%money%", money));
            }
        });
        if (addedFirstCost.compareAndSet(false, true)) {
            r.add(Pair.of("%currency%", ""));
            r.add(Pair.of("%money%", "0"));
        }
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
     * @param currency 货币类型
     */
    @Contract("null->false")
    public boolean canUseCurrency(@Nullable IEconomy currency) {
        if (currency == null) return false;
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
