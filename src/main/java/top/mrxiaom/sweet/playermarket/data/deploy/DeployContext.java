package top.mrxiaom.sweet.playermarket.data.deploy;

import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

public class DeployContext {
    protected EnumMarketType type;
    protected IEconomy currencyType;
    protected double price;
    protected int amount;
    protected ItemStack item;
    protected LocalDateTime outdateTime;
    protected String tag;
    protected String serverCustomName;

    protected DeployContext(AutoDeployProperty property, LocalDateTime now) {
        this.type = property.marketType;
        this.currencyType = property.marketCurrency;
        String priceStr = PAPI.setPlaceholders(null, property.marketPriceRaw);
        this.price = parseDouble(priceStr).orElseThrow(
                () -> new IllegalArgumentException("无法解析物品单价 " + priceStr));

        String amountStr = PAPI.setPlaceholders(null, property.marketAmountRaw);
        this.amount = parseInt(amountStr).orElseThrow(
                () -> new IllegalArgumentException("无法解析商品总份数"));

        String itemSourceStr = PAPI.setPlaceholders(null, property.marketItemSourceRaw);
        ItemStack item = property.plugin.getItem(itemSourceStr);
        if (item == null) {
            throw new IllegalArgumentException("无法解析物品来源 '" + itemSourceStr + "'");
        }
        this.item = item;

        String outdateTimeStr = PAPI.setPlaceholders(null, property.marketOutdateTimeRaw);
        Duration outdate = Duration.parse(outdateTimeStr);
        this.outdateTime = outdate.addFrom(now);
        this.tag = PAPI.setPlaceholders(null, property.marketTagRaw);
        this.serverCustomName = PAPI.setPlaceholders(null, property.marketCustomOwnerNameRaw);
    }

    public EnumMarketType getType() {
        return type;
    }

    public IEconomy getCurrencyType() {
        return currencyType;
    }

    public double getPrice() {
        return price;
    }

    public int getAmount() {
        return amount;
    }

    public ItemStack getItem() {
        return item;
    }

    public LocalDateTime getOutdateTime() {
        return outdateTime;
    }

    public String getTag() {
        return tag;
    }

    public String getServerCustomName() {
        return serverCustomName;
    }

    private static Optional<Integer> parseInt(String str) {
        if (str.contains(" to ")) {
            String[] split = str.split(" to ", 2);
            Integer min = Util.parseInt(split[0]).orElse(null);
            Integer max = Util.parseInt(split[1]).orElse(null);
            if (min == null || max == null) return Optional.empty();
            int v = new Random().nextInt(max - min + 1);
            return Optional.of(min + v);
        }
        return Util.parseInt(str);
    }

    private static Optional<Double> parseDouble(String str) {
        if (str.contains(" to ")) {
            String[] split = str.split(" to ", 2);
            Double min = Util.parseDouble(split[0]).orElse(null);
            Double max = Util.parseDouble(split[1]).orElse(null);
            if (min == null || max == null) return Optional.empty();
            double v = new Random().nextInt(114514) / 114515.0;
            double value = min + ((max - min) * v);
            String format = String.format("%.2f", value);
            return Util.parseDouble(format);
        }
        return Util.parseDouble(str);
    }

}
