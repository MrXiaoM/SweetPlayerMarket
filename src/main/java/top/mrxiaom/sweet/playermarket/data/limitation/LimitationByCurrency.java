package top.mrxiaom.sweet.playermarket.data.limitation;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.limitation.item.*;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.util.*;

public class LimitationByCurrency extends BaseLimitation {
    private final @NotNull Set<String> currencyList = new HashSet<>();
    protected LimitationByCurrency(SweetPlayerMarket plugin, ConfigurationSection config) {
        super(plugin, config);
        this.currencyList.addAll(config.getStringList("currency"));
    }

    /**
     * 获取货币是否匹配该限制条件
     */
    public boolean isCurrencyMatch(@Nullable IEconomy currency) {
        if (currency == null) return false;
        String id = currency.id();
        if (id.contains(":")) {
            String type = id.split(":", 2)[0];
            if (currencyList.contains(type)) return true;
        }
        return currencyList.contains(id);
    }

    public static LimitationByCurrency of(SweetPlayerMarket plugin, ConfigurationSection config) {
        return new LimitationByCurrency(plugin, config);
    }
}
