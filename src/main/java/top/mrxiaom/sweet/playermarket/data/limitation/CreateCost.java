package top.mrxiaom.sweet.playermarket.data.limitation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CreateCost {
    private final @Nullable IEconomy currency;
    private final @NotNull Function<Double, Double> money;
    private final @NotNull List<CreateCost> more;

    public CreateCost(@Nullable IEconomy currency, @NotNull Function<Double, Double> money) {
        this(currency, money, Collections.emptyList());
    }
    public CreateCost(@Nullable IEconomy currency, @NotNull Function<Double, Double> money, @NotNull List<CreateCost> more) {
        this.currency = currency;
        this.money = money;
        this.more = Collections.unmodifiableList(more);
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

    public void collectCosts(Map<IEconomy, Double> map, IEconomy createCurrency, double totalMoney) {
        IEconomy currency = currency(createCurrency);
        double oldMoney = map.getOrDefault(currency, 0.0);
        double money = oldMoney + money(totalMoney);
        map.put(currency, money);
        for (CreateCost createCost : more) {
            createCost.collectCosts(map, createCurrency, totalMoney);
        }
    }

    public @NotNull List<CreateCost> more() {
        return more;
    }
}
