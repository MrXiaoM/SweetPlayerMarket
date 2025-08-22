package top.mrxiaom.sweet.playermarket.data.limitation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.util.function.Function;

public class CreateCost {
    private final @Nullable IEconomy currency;
    private final @NotNull Function<Double, Double> money;

    public CreateCost(@Nullable IEconomy currency, @NotNull Function<Double, Double> money) {
        this.currency = currency;
        this.money = money;
    }

    public @NotNull IEconomy currency(@NotNull IEconomy inherit) {
        return currency == null ? inherit : currency;
    }

    public boolean isTheSameCurrency(@NotNull IEconomy currency) {
        return this.currency == null || this.currency.equals(currency);
    }

    public double money(double totalMoney) {
        return money.apply(totalMoney);
    }
}
