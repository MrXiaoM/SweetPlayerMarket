package top.mrxiaom.sweet.playermarket.economy;

import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IEconomyResolver;
import top.mrxiaom.sweet.playermarket.data.DisplayNames;

import java.util.Objects;

public class PlayerPointsEconomy implements IEconomy {
    public static class Resolver implements IEconomyResolver {
        private final SweetPlayerMarket plugin;
        public Resolver(SweetPlayerMarket plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            return str.equals("PlayerPoints") ? plugin.getPlayerPoints() : null;
        }

        @Override
        public @Nullable String parseName(String str) {
            if (str.equals("PlayerPoints")) {
                return DisplayNames.inst().getCurrencyNamePlayerPoints();
            }
            return null;
        }

        @Override
        public @Nullable String getName(IEconomy economy) {
            if (economy instanceof PlayerPointsEconomy) {
                return DisplayNames.inst().getCurrencyNamePlayerPoints();
            }
            return null;
        }
    }
    private final PlayerPointsAPI api;
    public PlayerPointsEconomy(PlayerPointsAPI api) {
        this.api = api;
    }

    @Override
    public String id() {
        return "PlayerPoints";
    }

    @Override
    public String getName() {
        return "PlayerPoints";
    }

    @Override
    public double get(OfflinePlayer player) {
        return api.look(player.getUniqueId());
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return get(player) >= money;
    }

    @Override
    public boolean giveMoney(OfflinePlayer player, double money) {
        return api.give(player.getUniqueId(), (int) money);
    }

    @Override
    public boolean takeMoney(OfflinePlayer player, double money) {
        return api.take(player.getUniqueId(), (int) money);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayerPointsEconomy)) return false;
        PlayerPointsEconomy that = (PlayerPointsEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }
}
