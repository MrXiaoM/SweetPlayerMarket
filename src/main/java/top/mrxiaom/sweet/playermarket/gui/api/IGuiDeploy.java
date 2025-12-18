package top.mrxiaom.sweet.playermarket.gui.api;

import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.util.List;

public interface IGuiDeploy {
    enum NumberOperation {
        ADD, MINUS, SET
    }
    void modifyAmount(NumberOperation operation, int value);
    void modifyItemCount(NumberOperation operation, int value);
    void modifyPrice(NumberOperation operation, double value);
    void setCurrency(IEconomy currency);
    void switchCurrency(List<IEconomy> currencyList);
}
