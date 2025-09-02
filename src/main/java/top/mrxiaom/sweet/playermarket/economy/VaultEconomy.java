package top.mrxiaom.sweet.playermarket.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.IEconomyResolver;
import top.mrxiaom.sweet.playermarket.data.DisplayNames;

import java.util.Objects;

public class VaultEconomy implements IEconomy {
    public static class Resolver implements IEconomyResolver {
        private final SweetPlayerMarket plugin;
        public Resolver(SweetPlayerMarket plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable IEconomy parse(@NotNull String str) {
            return str.equals("Vault") ? plugin.getVault() : null;
        }

        @Override
        public @Nullable String parseName(String str) {
            if (str.equals("Vault")) {
                return DisplayNames.inst().getCurrencyNameVault();
            }
            return null;
        }

        @Override
        public @Nullable String getName(IEconomy economy) {
            if (economy instanceof VaultEconomy) {
                return DisplayNames.inst().getCurrencyNameVault();
            }
            return null;
        }
    }
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
