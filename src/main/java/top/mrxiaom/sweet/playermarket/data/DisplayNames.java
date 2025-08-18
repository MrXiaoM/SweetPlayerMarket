package top.mrxiaom.sweet.playermarket.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.economy.VaultEconomy;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.economy.MPointsEconomy;
import top.mrxiaom.sweet.playermarket.economy.PlayerPointsEconomy;
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

    public DisplayNames(SweetPlayerMarket plugin) {
        super(plugin);
    }

    @NotNull
    public String getMarketTypeName(@Nullable EnumMarketType type) {
        if (type == null) {
            return marketTypeAll;
        }
        String s = marketTypeNames.get(type);
        return s != null ? s : type.name();
    }

    @NotNull
    public String getCurrencyName(@Nullable String currency) {
        if (currency == null) {
            return currencyAll;
        }
        if (currency.equals("Vault")) {
            return currencyVault;
        }
        if (currency.equals("PlayerPoints")) {
            return currencyPlayerPoints;
        }
        if (currency.startsWith("MPoints:") && currency.length() > 8) {
            String sign = currency.substring(8);
            return currencyMPoints.getOrDefault(sign, sign);
        }
        return currency;
    }

    @NotNull
    public String getCurrencyName(@Nullable IEconomy currency) {
        if (currency == null) {
            return currencyAll;
        }
        if (currency instanceof VaultEconomy) {
            return currencyVault;
        }
        if (currency instanceof PlayerPointsEconomy) {
            return currencyPlayerPoints;
        }
        if (currency instanceof MPointsEconomy) {
            String sign = ((MPointsEconomy) currency).sign();
            return currencyMPoints.getOrDefault(sign, sign);
        }
        return currency.getName();
    }

    @NotNull
    public String getColumnName(@NotNull String column) {
        return columnNames.getOrDefault(column, column);
    }

    @NotNull
    public String getSortName(@NotNull EnumSort sort) {
        String s = sortNames.get(sort);
        return s != null ? s : sort.name();
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

    public List<String> columnList() {
        return columnList;
    }

    public static DisplayNames inst() {
        return instanceOf(DisplayNames.class);
    }
}
