package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.limitation.BaseLimitation;
import top.mrxiaom.sweet.playermarket.data.limitation.LimitationByCurrency;
import top.mrxiaom.sweet.playermarket.data.limitation.LimitationByItem;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class LimitationManager extends AbstractModule {
    private BaseLimitation limitDefault;
    private final List<LimitationByCurrency> limitByCurrency = new ArrayList<>();
    private final List<LimitationByItem> limitByItems = new ArrayList<>();
    public LimitationManager(SweetPlayerMarket plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        File file = plugin.resolve("./limitation.yml");
        if (!file.exists()) {
            plugin.saveResource("limitation.yml", file);
        }
        FileConfiguration config = plugin.resolveGotoFlag(ConfigUtils.load(file));
        limitDefault = BaseLimitation.of(plugin, getSection(config,"default"));

        limitByCurrency.clear();
        for (ConfigurationSection section : ConfigUtils.getSectionList(config, "by-currency")) {
            limitByCurrency.add(LimitationByCurrency.of(plugin, section));
        }

        limitByItems.clear();
        for (ConfigurationSection section : ConfigUtils.getSectionList(config, "by-items")) {
            limitByItems.add(LimitationByItem.of(plugin, section));
        }

        info("读取了 " + limitByItems.size() + " 个特殊物品上架限制");
    }

    @NotNull
    @ApiStatus.Internal
    public BaseLimitation getLimitation(@Nullable ItemStack item, @Nullable IEconomy currency) {
        if (item != null) {
            BaseLimitation byItem = getLimitByItemOrNull(item);
            if (byItem != null) {
                return byItem;
            }
        }
        if (currency != null) {
            BaseLimitation byCurrency = getLimitByCurrencyOrNull(currency);
            if (byCurrency != null) {
                return byCurrency;
            }
        }
        return getLimitDefault();
    }

    @NotNull
    public BaseLimitation getLimitDefault() {
        return limitDefault;
    }

    @Nullable
    public BaseLimitation getLimitByCurrencyOrNull(@Nullable IEconomy currency) {
        for (LimitationByCurrency limitation : limitByCurrency) {
            if (limitation.isCurrencyMatch(currency)) {
                return limitation;
            }
        }
        return null;
    }

    @Nullable
    public BaseLimitation getLimitByItemOrNull(@Nullable ItemStack item) {
        for (LimitationByItem limitation : limitByItems) {
            if (limitation.isItemMatch(item)) {
                return limitation;
            }
        }
        return null;
    }

    @NotNull
    public BaseLimitation getLimitByItem(@Nullable ItemStack item) {
        BaseLimitation limit = getLimitByItemOrNull(item);
        return limit == null ? limitDefault : limit;
    }

    @SuppressWarnings("SameParameterValue")
    private static ConfigurationSection getSection(ConfigurationSection config, String key) {
        ConfigurationSection section = config.getConfigurationSection(key);
        return section != null ? section : new MemoryConfiguration();
    }

    public static LimitationManager inst() {
        return instanceOf(LimitationManager.class);
    }
}
