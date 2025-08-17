package top.mrxiaom.sweet.playermarket.economy;

public interface IEconomyWithSign {
    String getName();
    IEconomy of(String sign);
}
