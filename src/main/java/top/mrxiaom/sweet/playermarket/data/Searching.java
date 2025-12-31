package top.mrxiaom.sweet.playermarket.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

@SuppressWarnings("UnusedReturnValue")
public class Searching {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private boolean outdated;
    private @Nullable String playerId;
    private @Nullable EnumMarketType type;
    private @Nullable String currency;
    private @Nullable String tag;
    private @NotNull String orderColumn = "create_time";
    private @NotNull EnumSort orderType = EnumSort.DESC;
    private boolean onlyOutOfStock = false;
    private @Nullable Integer notice;
    private boolean noticeReversed = false;

    private Searching(boolean outdated) {
        this.outdated = outdated;
    }

    public boolean outdated() {
        return outdated;
    }

    public @Nullable String playerId() {
        return playerId;
    }

    public @Nullable EnumMarketType type() {
        return type;
    }

    public @Nullable String currency() {
        return currency;
    }

    public @Nullable String tag() {
        return tag;
    }

    public @NotNull String orderColumn() {
        return orderColumn;
    }

    public @NotNull EnumSort orderType() {
        return orderType;
    }

    public boolean onlyOutOfStock() {
        return onlyOutOfStock;
    }

    public @Nullable Integer notice() {
        return notice;
    }

    public Searching outdated(boolean outdated) {
        this.outdated = outdated;
        return this;
    }

    public Searching playerId(@Nullable String playerId) {
        this.playerId = playerId;
        return this;
    }

    public Searching type(@Nullable EnumMarketType type) {
        this.type = type;
        return this;
    }

    public Searching currency(@Nullable String currency) {
        this.currency = currency;
        return this;
    }

    public Searching tag(@Nullable String tag) {
        this.tag = tag;
        return this;
    }

    public Searching orderColumn(@NotNull String column) {
        orderColumn = column;
        return this;
    }

    public Searching orderType(@NotNull EnumSort sort) {
        orderType = sort;
        return this;
    }

    public Searching orderBy(@NotNull String column, @NotNull EnumSort sort) {
        orderColumn = column;
        orderType = sort;
        return this;
    }

    public Searching onlyOutOfStock(boolean onlyOutOfStock) {
        this.onlyOutOfStock = onlyOutOfStock;
        return this;
    }

    public Searching noticeFlag(@Nullable NoticeFlag flag) {
        return noticeFlag(flag, false);
    }

    public Searching noticeFlag(@Nullable NoticeFlag flag, boolean noticeReversed) {
        return notice(flag == null ? null : flag.getIntValue(), noticeReversed);
    }

    public Searching notice(@Nullable Integer notice) {
        return notice(notice, false);
    }

    public Searching notice(@Nullable Integer notice, boolean noticeReversed) {
        this.notice = notice;
        this.noticeReversed = noticeReversed;
        return this;
    }

    public String generateConditions() {
        StringJoiner conditions = new StringJoiner(" AND ");
        // 如果设置了查询条件 notice_flag，则忽略 outdate_time 和 onlyOutOfStock 选项
        if (notice == null) {
            String now = LocalDateTime.now().format(formatter);
            conditions.add(onlyOutOfStock ? "`amount`=0 " : "`amount`>0");
            if (outdated) {
                // 获取过时商品，应该筛选 outdate_time 不为 NULL 且 outdate_time 小于现在的商品
                conditions.add("(`outdate_time` IS NOT NULL AND `outdate_time` < '" + now + "')");
            } else {
                // 获取未过时商品，应该筛选 outdate_time 为 NULL (无期限) 或者 outdate_time 大于现在的商品
                conditions.add("(`outdate_time` IS NULL OR `outdate_time` >= '" + now + "')");
            }
        }
        if (type != null) conditions.add("`shop_type`=?");
        if (currency != null) conditions.add("`currency`=?");
        if (tag != null) conditions.add("`tag`=?");
        if (playerId != null) conditions.add("`player`=?");
        if (notice != null) {
            if (noticeReversed) {
                conditions.add("`notice_flag`!=?");
            } else {
                conditions.add("`notice_flag`=?");
            }
        }
        return conditions + " ";
    }

    public String generateOrder() {
        return "ORDER BY `" + orderColumn + "` " + orderType.value() + " ";
    }

    public void setValues(PreparedStatement ps, int parameterIndex) throws SQLException {
        int i = parameterIndex - 1;
        if (type != null) ps.setInt(++i, type.value());
        if (currency != null) ps.setString(++i, currency);
        if (tag != null) ps.setString(++i, tag);
        if (playerId != null) ps.setString(++i, playerId);
        if (notice != null) ps.setInt(++i, notice);
    }

    public static Searching of(boolean outdated) {
        return new Searching(outdated);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }

    public static LocalDateTime format(String str) {
        return str == null ? null : LocalDateTime.parse(str, formatter);
    }
}
