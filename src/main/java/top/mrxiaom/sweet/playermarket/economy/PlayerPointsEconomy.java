package top.mrxiaom.sweet.playermarket.economy;

import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;

public class PlayerPointsEconomy implements IEconomy {
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
    public void giveMoney(OfflinePlayer player, double money) {
        api.give(player.getUniqueId(), (int) money);
    }

    @Override
    public void takeMoney(OfflinePlayer player, double money) {
        api.take(player.getUniqueId(), (int) money);
    }
}
