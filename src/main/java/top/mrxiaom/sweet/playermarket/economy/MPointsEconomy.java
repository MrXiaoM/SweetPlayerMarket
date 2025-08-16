package top.mrxiaom.sweet.playermarket.economy;

import me.yic.mpoints.MPointsAPI;
import org.bukkit.OfflinePlayer;
import top.mrxiaom.pluginbase.economy.IEconomy;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class MPointsEconomy implements IEconomyWithSign, IEconomy {
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
    public IEconomy of(String sign) {
        IEconomy cache = caches.get(sign);
        if (cache != null) return cache;
        IEconomy economy = new MPointsEconomy(api, sign);
        caches.put(sign, economy);
        return economy;
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
}
