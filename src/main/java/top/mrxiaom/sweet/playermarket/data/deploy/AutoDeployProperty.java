package top.mrxiaom.sweet.playermarket.data.deploy;

import com.ezylang.evalex.Expression;
import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.event.MarketItemAutoCreatedEvent;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;
import java.util.function.Function;

import static top.mrxiaom.pluginbase.func.AbstractPluginHolder.t;

public class AutoDeployProperty {
    public class Data {
        private long lastRound;
        private int successRoundCount;
        Data(ConfigurationSection data) {
            // 加载数据
            lastRound = data.getLong(id + ".last-round", -1);
            successRoundCount = data.getInt(id + ".success-round-count", 0);
        }

        public long getLastRound() {
            return lastRound;
        }

        public void setLastRound(long lastRound) {
            this.lastRound = lastRound;
        }

        public int getSuccessRoundCount() {
            return successRoundCount;
        }

        public void setSuccessRoundCount(int successRoundCount) {
            this.successRoundCount = successRoundCount;
        }

        public void save(ConfigurationSection data) {
            // 保存配置
            data.set(id + ".last-round", lastRound);
            data.set(id + ".success-round-count", successRoundCount);
        }
    }
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<DayOfWeek> ALL_WEEKS = Lists.newArrayList(DayOfWeek.values());
    private static final List<Integer> ALL_MONTHS = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    protected final @NotNull SweetPlayerMarket plugin;
    private final @NotNull String id;
    // schedule-time
    private final @NotNull LocalDateTime startTime, endTime;
    private final long startTimeTimestamp;
    private final @NotNull Duration periodDuration;
    private final @NotNull Duration givingUpGap;
    private final int successRunRound;
    // conditions
    private final @NotNull String conditionEval;
    private final double conditionRate;
    private final @NotNull List<DayOfWeek> conditionWeeks;
    private final @NotNull List<Integer> conditionMonths;
    // market-items
    protected final @NotNull EnumMarketType marketType;
    protected final @NotNull IEconomy marketCurrency;
    protected final @NotNull String marketPriceRaw;
    protected final @NotNull String marketAmountRaw;
    protected final @NotNull String marketTagRaw;
    protected final @NotNull String marketCustomOwnerNameRaw;
    protected final @NotNull String marketItemSourceRaw;
    protected final @NotNull String marketOutdateTimeRaw;
    // others
    public final @NotNull List<IAction> successCommands;

    private final Data data;

    public AutoDeployProperty(@NotNull SweetPlayerMarket plugin, @NotNull String id, @NotNull ConfigurationSection config, ConfigurationSection dataConfig) {
        this.plugin = plugin;
        this.id = id;
        // schedule-time
        String startTimeStr = config.getString("schedule-time.start-time", "2026-01-01 00:00:00");
        String endTimeStr = config.getString("schedule-time.end-time", "2099-12-31 23:59:59");
        String periodDurationStr = config.getString("schedule-time.period-duration", "24h");
        String givingUpGapStr = config.getString("schedule-time.giving-up-gap", "5m");

        this.startTime = LocalDateTime.parse(startTimeStr, DATE_TIME_FORMAT);
        this.endTime = LocalDateTime.parse(endTimeStr, DATE_TIME_FORMAT);
        this.periodDuration = Duration.parse(periodDurationStr);
        this.givingUpGap = Duration.parse(givingUpGapStr);
        this.successRunRound = config.getInt("schedule-time.success-run-round", -1);
        this.startTimeTimestamp = this.startTime.toEpochSecond(ZoneOffset.UTC);

        // conditions
        this.conditionEval = config.getString("conditions.eval", "");
        Double conditionRate = ConfigUtils.getPercentAsDouble(config, "conditions.rate", 0.0);
        if (conditionRate <= 0) {
            throw new IllegalArgumentException("rate 的值无效");
        }
        this.conditionRate = conditionRate;

        this.conditionWeeks = parseCondList(config.getStringList("conditions.weeks"), ALL_WEEKS, str -> DayOfWeek.valueOf(str.toUpperCase()));
        this.conditionMonths = parseCondList(config.getStringList("conditions.months"), ALL_MONTHS, Integer::parseInt);

        // market-item
        EnumMarketType marketType = Util.valueOrNull(EnumMarketType.class, config.getString("market-item.type"));
        if (marketType == null) {
            throw new IllegalArgumentException("market-item.type 的值无效");
        }
        this.marketType = marketType;
        String currencyTypeStr = config.getString("market-item.currency.type");
        IEconomy marketCurrency = plugin.parseEconomy(currencyTypeStr);
        if (marketCurrency == null) {
            throw new IllegalArgumentException("无法找到 market-item.currency.type 指定的货币类型 '" + currencyTypeStr + "'");
        }
        this.marketCurrency = marketCurrency;
        String priceStr = config.getString("market-item.currency.price");
        if (priceStr == null) {
            throw new IllegalArgumentException("未指定商品单价 price");
        }
        this.marketPriceRaw = priceStr;
        String amountStr = config.getString("market-item.amount");
        if (amountStr == null) {
            throw new IllegalArgumentException("未指定商品总份数 amount");
        }
        this.marketAmountRaw = amountStr;
        this.marketTagRaw = config.getString("market-item.tag", "");
        this.marketCustomOwnerNameRaw = config.getString("market-item.custom-owner-name", "");
        String itemSourceStr = config.getString("market-item.item-source");
        if (itemSourceStr == null) {
            throw new IllegalArgumentException("未指定物品来源 item-source");
        }
        this.marketItemSourceRaw = itemSourceStr;
        this.marketOutdateTimeRaw = config.getString("market-item.outdate-time", "24h");

        // others
        this.successCommands = ActionProviders.loadActions(config, "success-commands");

        this.data = new Data(dataConfig);
    }

    public @NotNull String getId() {
        return id;
    }

    public int getSuccessRunRound() {
        return successRunRound;
    }

    public @NotNull Duration getGivingUpGap() {
        return givingUpGap;
    }

    public boolean isNotInTime(LocalDateTime now) {
        return now.isBefore(startTime) || now.isAfter(endTime);
    }

    /**
     * 获取定时器已经运行多少轮了
     * @param now 当前时间
     */
    public long getCurrentPassRound(LocalDateTime now) {
        // 获取经过开始时间有多少秒了
        long passTimeSeconds = now.toEpochSecond(ZoneOffset.UTC) - startTimeTimestamp;
        // 已经过去了多少轮
        return passTimeSeconds / periodDuration.getTotalSeconds();
    }

    /**
     * 获取定时器运行的这一轮已经过去多少秒
     * @param now 当前时间
     */
    public long getCurrentRoundSeconds(LocalDateTime now) {
        // 获取经过开始时间有多少秒了
        long passTimeSeconds = now.toEpochSecond(ZoneOffset.UTC) - startTimeTimestamp;
        // 这一轮已经过去多少秒
        return passTimeSeconds % periodDuration.getTotalSeconds();
    }

    /**
     * 获取定时器下一轮到来的时间
     * @param now 当前时间
     */
    public LocalDateTime getNextRoundTime(LocalDateTime now) {
        long currentPassRound = getCurrentPassRound(now);
        return startTime.plusSeconds((currentPassRound + 1) * periodDuration.getTotalSeconds());
    }

    public boolean doConditionCheck(LocalDateTime now) {
        return doConditionCheckWithReason(now) == 0;
    }

    public int doConditionCheckWithReason(LocalDateTime now) {
        if (!conditionEval.isEmpty()) {
            // eval
            String expression = PAPI.setPlaceholders(null, conditionEval);
            try {
                if (new Expression(expression).evaluate().getBooleanValue() != Boolean.TRUE) {
                    return 1;
                }
            } catch (Throwable t) {
                plugin.warn("执行 " + id + " 定时器的表达式条件 '" + expression + "' 时出现异常", t);
                return 1;
            }
        }
        if (conditionRate < 1.0) {
            // rate
            double rand = new Random().nextInt(1919810) / 1919811.0;
            if (rand > conditionRate) {
                return 2;
            }
        }
        if (!conditionWeeks.contains(now.getDayOfWeek())) {
            // weeks
            return 3;
        }
        if (!conditionMonths.contains(now.getMonth().getValue())) {
            // months
            return 4;
        }

        return 0;
    }

    public DeployContext createContext(LocalDateTime now) {
        return new DeployContext(this, now);
    }

    public void doDeployItem(Connection conn, LocalDateTime now) throws SQLException {
        MarketplaceDatabase marketplace = plugin.getMarketplace();
        DeployContext ctx;
        try {
            ctx = createContext(now);
        } catch (Throwable t) {
            plugin.warn("尝试执行自动上架商品配置 " + id + " 时出现一个异常: " + t.getMessage());
            return;
        }
        ConfigurationSection params = new MemoryConfiguration();
        params.set("auto-deploy.property", id);
        if (!ctx.tag.isEmpty()) {
            params.set("auto-deploy.tag", ctx.tag);
        }
        // 上面的校验全部通过时，提交上架物品，增加成功轮数计数
        data.successRoundCount++;
        MarketItem marketItem = MarketItem.builder(ctx.serverCustomName)
                .type(ctx.type)
                .currency(ctx.currencyType)
                .price(ctx.price)
                .amount(ctx.amount)
                .item(ctx.item)
                .outdateTime(ctx.outdateTime)
                .params(params)
                .build(plugin.itemTagResolver());
        marketplace.putItem(conn, marketItem);
        plugin.info("已自动上架商品 " + id + " 到全球市场");
        plugin.getScheduler().runTask(() -> {
            if (!successCommands.isEmpty()) {
                // 执行上架成功命令
                ListPair<String, Object> r = new ListPair<>();
                AbstractGuiSearch.applyMarketItemPlaceholders(plugin, marketItem, r);
                plugin.getScheduler().runTask(() -> ActionProviders.run(plugin, null, successCommands, r));
            }
            MarketItemAutoCreatedEvent e = new MarketItemAutoCreatedEvent(marketItem, this);
            Bukkit.getPluginManager().callEvent(e);
        });
    }

    public void print(CommandSender sender) {
        StringJoiner weeks = new StringJoiner("&f, &e");
        StringJoiner months = new StringJoiner("&f, &e");
        for (DayOfWeek week : conditionWeeks) {
            weeks.add(week.name());
        }
        for (Integer month : conditionMonths) {
            months.add(String.valueOf(month));
        }
        LocalDateTime now = LocalDateTime.now();
        t(sender, "&b&l自动上架配置详情",
                "&f  ID: &e" + id,
                "&f  起始时间: &e" + startTime.format(DATE_TIME_FORMAT),
                "&f  结束时间: &e" + endTime.format(DATE_TIME_FORMAT),
                "&f  定时周期: &e" + periodDuration.getDisplay(),
                "&f  本轮已经过秒数: &e" + getCurrentRoundSeconds(now),
                "&f  放弃执行间隔: &e" + givingUpGap.getDisplay(),
                "&f  下一轮: &e" + getNextRoundTime(now).format(DATE_TIME_FORMAT),
                "&f  定时器已经过轮数: &e" + data.getLastRound() + "&f / &e" + getCurrentPassRound(now),
                "&f  成功运行轮数: &e" + data.getSuccessRoundCount() + " &f/ &e" + (successRunRound < 0 ? "无限" : String.valueOf(successRunRound)),
                "&f  条件语句: &e" + conditionEval,
                "&f  成功概率: &e" + String.format("%.2f", conditionRate * 100) + "%",
                "&f  允许星期: &f[ &e" + weeks + "&f ]",
                "&f  允许月份: &f[ &e" + months + "&f ]",
                "");
    }

    public Data getData() {
        return data;
    }

    private static <T> List<T> parseCondList(List<String> strList, List<T> fullList, Function<String, T> transformer) {
        List<T> positiveList = new ArrayList<>();
        List<T> negativeList = new ArrayList<>();
        for (String str : strList) {
            try {
                if (str.startsWith("!")) {
                    negativeList.add(transformer.apply(str.substring(1)));
                } else {
                    positiveList.add(transformer.apply(str));
                }
            } catch (Throwable t) {
                throw new IllegalArgumentException("尝试解析 '" + str + "' 时出现错误", t);
            }
        }
        List<T> list = new ArrayList<>(positiveList.isEmpty() ? fullList : positiveList);
        list.removeAll(negativeList);
        return list;
    }
}
