package top.mrxiaom.sweet.playermarket.economy;

import org.bukkit.permissions.Permissible;

public interface IEconomy extends top.mrxiaom.pluginbase.economy.IEconomy {
    String PERM_PREFIX = "sweet.playermarket.create.currency.";
    String id();
    boolean hasPermission(Permissible p);
}
