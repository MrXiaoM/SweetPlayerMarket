package top.mrxiaom.sweet.playermarket.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.limitation.BaseLimitation;
import top.mrxiaom.sweet.playermarket.data.limitation.LimitationByItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class LimitationManager extends AbstractModule {
    private BaseLimitation limitDefault;
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
        FileConfiguration config = plugin.resolveGotoFlag(Util.load(file));
        limitDefault = BaseLimitation.of(plugin, getSection(config,"default"));

        limitByItems.clear();
        for (ConfigurationSection section : Util.getSectionList(config, "by-items")) {
            limitByItems.add(LimitationByItem.of(plugin, section));
        }

        info("读取了 " + limitByItems.size() + " 个特殊物品上架限制");
    }

    @NotNull
    public BaseLimitation getLimitByItem(@Nullable ItemStack item) {
        for (LimitationByItem limitation : limitByItems) {
            if (limitation.isItemMatch(item)) {
                return limitation;
            }
        }
        return limitDefault;
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
