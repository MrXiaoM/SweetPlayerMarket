package top.mrxiaom.sweet.playermarket.economy;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IEconomyResolver;

import java.util.*;

public class CoinsEngineEconomy implements IEconomyWithSign, IEconomy {
    public static class Resolver implements IEconomyResolver {
        private final SweetPlayerMarket plugin;
        public Resolver(SweetPlayerMarket plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            if (str.startsWith("CoinsEngine:") && str.length() > 12) {
                IEconomyWithSign withSign = plugin.getCoinsEngine();
                if (withSign != null) {
                    return withSign.of(str.substring(12));
                }
            }
            return null;
        }

        @Override
        public @Nullable String parseName(String str) {
            if (str.startsWith("CoinsEngine:") && str.length() > 12) {
                String sign = str.substring(12);
                Currency currency = CoinsEngineAPI.getCurrency(sign);
                if (currency != null) {
                    return currency.getName();
                }
            }
            return null;
        }

        @Override
        public @Nullable String getName(IEconomy economy) {
            if (economy instanceof CoinsEngineEconomy) {
                return ((CoinsEngineEconomy) economy).name();
            }
            return null;
        }
    }
    private static final Map<String, IEconomy> caches = new HashMap<>();
    private final Currency currency;

    public CoinsEngineEconomy(Currency currency) {
        this.currency = currency;
    }

    public String sign() {
        return currency.getId();
    }

    public String name() {
        return currency.getName();
    }

    @Override
    public List<String> getSigns() {
        List<String> signs = new ArrayList<>();
        for (Currency currency : CoinsEngineAPI.getCurrencies()) {
            signs.add(currency.getId());
        }
        return signs;
    }

    @Override
    public IEconomy of(String sign) {
        IEconomy cache = caches.get(sign);
        if (cache != null) return cache;
        Currency currency = CoinsEngineAPI.getCurrency(sign);
        if (currency == null) return null;
        IEconomy economy = new CoinsEngineEconomy(currency);
        caches.put(sign, economy);
        return economy;
    }

    @Override
    public String id() {
        return currency == null ? "CoinsEngine" : ("CoinsEngine:" + currency.getId());
    }

    @Override
    public String getName() {
        return currency == null ? "CoinsEngine" : ("CoinsEngine{" + currency.getId() + "}");
    }

    @Override
    public double get(OfflinePlayer player) {
        if (currency == null) throw new UnsupportedOperationException("");
        return CoinsEngineAPI.getBalance(player.getUniqueId(), currency);
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return get(player) >= money;
    }

    @Override
    public boolean giveMoney(OfflinePlayer player, double money) {
        if (currency == null) throw new UnsupportedOperationException("");
        return CoinsEngineAPI.addBalance(player.getUniqueId(), currency, money);
    }

    @Override
    public boolean takeMoney(OfflinePlayer player, double money) {
        if (currency == null) throw new UnsupportedOperationException("");
        return CoinsEngineAPI.removeBalance(player.getUniqueId(), currency, money);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CoinsEngineEconomy)) return false;
        CoinsEngineEconomy that = (CoinsEngineEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }
}
