package top.mrxiaom.sweet.playermarket.economy;

import com.google.common.collect.Lists;
import me.yic.mpoints.MPointsAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IEconomyResolver;
import top.mrxiaom.sweet.playermarket.data.DisplayNames;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MPointsEconomy implements IEconomyWithSign, IEconomy {
    public static class Resolver implements IEconomyResolver {
        private final SweetPlayerMarket plugin;
        public Resolver(SweetPlayerMarket plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            if (str.startsWith("MPoints:") && str.length() > 8) {
                IEconomyWithSign withSign = plugin.getMPoints();
                if (withSign != null) {
                    return withSign.of(str.substring(8));
                }
            }
            return null;
        }

        @Override
        public @Nullable String parseName(String str) {
            if (str.startsWith("MPoints:") && str.length() > 8) {
                String sign = str.substring(8);
                return DisplayNames.inst().getCurrencyNameMPoints(sign);
            }
            return null;
        }

        @Override
        public @Nullable String getName(IEconomy economy) {
            if (economy instanceof MPointsEconomy) {
                String sign = ((MPointsEconomy) economy).sign();
                return DisplayNames.inst().getCurrencyNameMPoints(sign);
            }
            return null;
        }
    }
    private static final Map<String, IEconomy> caches = new HashMap<>();
    private final MPointsAPI api;
    private final String sign;

    public MPointsEconomy(MPointsAPI api, String sign) {
        this.api = api;
        this.sign = sign;
    }

    public String sign() {
        return sign;
    }

    @Override
    public List<String> getSigns() {
        return Lists.newArrayList(api.getpointslist());
    }

    @Override
    public IEconomy of(String sign) {
        IEconomy cache = caches.get(sign);
        if (cache != null) return cache;
        IEconomy economy = new MPointsEconomy(api, sign);
        caches.put(sign, economy);
        return economy;
    }

    @Override
    public String id() {
        return "MPoints:" + sign;
    }

    @Override
    public String getName() {
        return sign == null ? "MPoints" : ("MPoints{" + sign + "}");
    }

    @Override
    public double get(OfflinePlayer player) {
        if (sign == null) throw new UnsupportedOperationException("");
        return api.getbalance(sign, player.getUniqueId()).doubleValue();
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return get(player) >= money;
    }

    @Override
    public void giveMoney(OfflinePlayer player, double money) {
        if (sign == null) throw new UnsupportedOperationException("");
        api.changebalance(sign, player.getUniqueId(), player.getName(), BigDecimal.valueOf(money), true);
    }

    @Override
    public void takeMoney(OfflinePlayer player, double money) {
        if (sign == null) throw new UnsupportedOperationException("");
        api.changebalance(sign, player.getUniqueId(), player.getName(), BigDecimal.valueOf(money), false);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MPointsEconomy)) return false;
        MPointsEconomy that = (MPointsEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }
}
