package top.mrxiaom.sweet.playermarket.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

import java.util.Objects;

public class VaultEconomy implements IEconomy {
    private final Economy economy;
    private final String name;

    public VaultEconomy(Economy economy) {
        this.economy = economy;
        this.name = "Vault{" + economy.getName() + "}";
    }

    @Override
    public String id() {
        return "Vault";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double get(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double money) {
        return economy.has(player, money);
    }

    @Override
    public void giveMoney(OfflinePlayer player, double money) {
        economy.depositPlayer(player, money);
    }

    @Override
    public void takeMoney(OfflinePlayer player, double money) {
        economy.withdrawPlayer(player, money);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VaultEconomy)) return false;
        VaultEconomy that = (VaultEconomy) o;
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id());
    }
}
