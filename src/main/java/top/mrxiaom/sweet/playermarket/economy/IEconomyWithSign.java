package top.mrxiaom.sweet.playermarket.economy;

import top.mrxiaom.pluginbase.economy.IEconomy;

public interface IEconomyWithSign {
    String getName();
    IEconomy of(String sign);
}
