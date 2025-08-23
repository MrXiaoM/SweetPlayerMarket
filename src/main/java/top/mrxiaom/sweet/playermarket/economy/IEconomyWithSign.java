package top.mrxiaom.sweet.playermarket.economy;

import java.util.List;

public interface IEconomyWithSign {
    String getName();
    List<String> getSigns();
    IEconomy of(String sign);
}
