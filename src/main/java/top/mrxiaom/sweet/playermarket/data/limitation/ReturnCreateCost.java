package top.mrxiaom.sweet.playermarket.data.limitation;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ReturnCreateCost {
    public static final Comparator<ReturnCreateCost> BY_PRIORITY = Comparator.comparingInt(ReturnCreateCost::priority);
    public static final ReturnCreateCost ZERO = ReturnCreateCost.of("default", new MemoryConfiguration());
    private final String id;
    private final String permission;
    private final int priority;
    private final double returnRateCommon;
    private final Map<String, Double> returnRateByCurrency = new HashMap<>();
    private ReturnCreateCost(String id, ConfigurationSection config) {
        this.id = id;
        this.permission = "sweet.playermarket.rcc." + id;
        this.priority = config.getInt("priority", 1000);
        this.returnRateCommon = Utils.parseRate(config.getString("return-rate.common"), 0.0);
        ConfigurationSection section = config.getConfigurationSection("return-rate.by-currency");
        if (section != null) for (String key : section.getKeys(false)) {
            Double rate = Utils.parseRate(section.getString(key), null);
            if (rate != null) {
                this.returnRateByCurrency.put(key, rate);
            }
        }
    }

    public String id() {
        return id;
    }

    public int priority() {
        return priority;
    }

    public String permission() {
        return permission;
    }

    public boolean hasPermission(Permissible p) {
        return p.hasPermission(permission);
    }

    public double returnRateCommon() {
        return returnRateCommon;
    }

    public Map<String, Double> returnRateByCurrency() {
        return returnRateByCurrency;
    }

    public double returnRateByCurrency(String currencyId) {
        Double v = returnRateByCurrency.get(currencyId);
        if (v != null) {
            return v;
        }
        if (currencyId.contains(":")) {
            return returnRateByCurrency.getOrDefault(currencyId.split(":", 2)[0], returnRateCommon);
        }
        return returnRateCommon;
    }

    @NotNull
    public static ReturnCreateCost of(@NotNull String id, @NotNull ConfigurationSection section) {
        return new ReturnCreateCost(id, section);
    }
}
