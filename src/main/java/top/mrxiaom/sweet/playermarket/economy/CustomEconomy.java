package top.mrxiaom.sweet.playermarket.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.playermarket.api.IEconomyResolver;
import top.mrxiaom.sweet.playermarket.func.CurrencyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomEconomy implements IEconomyWithSign, IEconomy {
    public static class Resolver implements IEconomyResolver {
        private final CurrencyManager manager;
        public Resolver(CurrencyManager manager) {
            this.manager = manager;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            if (str.startsWith("Custom:") && str.length() > 7) {
                String currencyId = str.substring(7);
                IEconomyWithSign withSign = manager.plugin.getCustomEconomy();
                return withSign.of(currencyId);
            }
            return null;
        }

        @Override
        public @Nullable String parseName(String str) {
            if (str.startsWith("Custom:") && str.length() > 7) {
                String currencyId = str.substring(7);
                Data data = manager.get(currencyId);
                if (data != null) {
                    return data.name;
                }
            }
            return null;
        }

        @Override
        public @Nullable String getName(IEconomy economy) {
            if (economy instanceof CustomEconomy) {
                Data data = ((CustomEconomy) economy).data;
                if (data != null) {
                    return data.name;
                }
            }
            return null;
        }
    }
    @ApiStatus.Internal
    public static class Data {
        public final String currencyId;
        public final String name;
        public final String get;
        public final List<IAction> give;
        public final List<IAction> take;
        public Data(String currencyId, String name, String get, List<IAction> give, List<IAction> take) {
            this.currencyId = currencyId;
            this.name = name;
            this.get = get;
            this.give = give;
            this.take = take;
        }
    }
    private static final Map<String, CustomEconomy> caches = new HashMap<>();

    public static void refresh(CurrencyManager manager) {
        List<String> toRemove = new ArrayList<>(caches.keySet());
        Map<String, Data> toAdd = new HashMap<>(manager.loadedData());
        for (Map.Entry<String, CustomEconomy> entry : caches.entrySet()) {
            String currencyId = entry.getKey();
            CustomEconomy economy = entry.getValue();
            Data data = toAdd.remove(currencyId);
            if (data != null) {
                economy.data = data;
                toRemove.remove(currencyId);
            }
        }
        for (String currencyId : toRemove) {
            caches.remove(currencyId);
        }
        for (Map.Entry<String, Data> entry : toAdd.entrySet()) {
            String currencyId = entry.getKey();
            Data data = entry.getValue();
            CustomEconomy economy = new CustomEconomy(manager, data);
            caches.put(currencyId, economy);
        }
    }
    private final CurrencyManager manager;
    private Data data;
    public CustomEconomy(CurrencyManager manager, Data data) {
        this.manager = manager;
        this.data = data;
    }

    @Override
    public String id() {
        return data == null ? "Custom" : ("Custom:" + data.currencyId);
    }

    @Override
    public String getName() {
        return data == null ? "Custom" : ("Custom{" + data.currencyId + "}");
    }

    @Override
    public boolean hasPermission(Permissible p) {
        return data != null && p.hasPermission(PERM_PREFIX + "custom." + data.currencyId);
    }

    @Override
    public double get(OfflinePlayer player) {
        if (data == null) throw new UnsupportedOperationException("");
        String str = PAPI.setPlaceholders(player, data.get);
        return Util.parseDouble(str).orElse(0.0);
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return get(player) >= money;
    }

    @Override
    public boolean giveMoney(OfflinePlayer player, double money) {
        if (data == null) throw new UnsupportedOperationException("");
        ActionProviders.run(manager.plugin, player.getPlayer(), data.give);
        return true;
    }

    @Override
    public boolean takeMoney(OfflinePlayer player, double money) {
        if (data == null) throw new UnsupportedOperationException("");
        if (has(player, money)) {
            ActionProviders.run(manager.plugin, player.getPlayer(), data.take);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getSigns() {
        return new ArrayList<>(manager.keySet());
    }

    @Override
    public IEconomy of(String sign) {
        CustomEconomy exists = caches.get(sign);
        if (exists != null) return exists;
        Data data = manager.get(sign);
        if (data == null) return null;
        CustomEconomy economy = new CustomEconomy(manager, data);
        caches.put(sign, economy);
        return economy;
    }

}
