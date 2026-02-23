package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.economy.CustomEconomy;

import java.io.File;
import java.util.*;

public class CurrencyManager extends AbstractModule {
    private final Map<String, CustomEconomy.Data> loadedData = new HashMap<>();
    public CurrencyManager(SweetPlayerMarket plugin) {
        super(plugin);
    }

    @Override
    public int priority() {
        return 990;
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        File file = plugin.resolve("./currencies.yml");
        if (!file.exists()) {
            plugin.saveResource("currencies.yml", file);
        }
        FileConfiguration config = plugin.resolveGotoFlag(ConfigUtils.load(file));

        loadedData.clear();
        ConfigurationSection section = config.getConfigurationSection("custom-currencies");
        if (section != null) for (String currencyId : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(currencyId);
            if (s == null) continue;
            String name = s.getString("name", currencyId);
            String get = s.getString("get");
            List<IAction> give = ActionProviders.loadActions(s, "give");
            List<IAction> take = ActionProviders.loadActions(s, "take");
            CustomEconomy.Data data = new CustomEconomy.Data(currencyId, name, get, give, take);
            loadedData.put(currencyId, data);
        }
        CustomEconomy.refresh(this);
    }

    public Set<String> keySet() {
        return Collections.unmodifiableSet(loadedData.keySet());
    }

    @Nullable
    public CustomEconomy.Data get(String currencyId) {
        return loadedData.get(currencyId);
    }

    public Map<String, CustomEconomy.Data> loadedData() {
        return Collections.unmodifiableMap(loadedData);
    }
}
