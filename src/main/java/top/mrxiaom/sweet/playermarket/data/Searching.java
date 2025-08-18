package top.mrxiaom.sweet.playermarket.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class Searching {
    private final boolean outdated;
    private @Nullable String playerId;
    private @Nullable EnumMarketType type;
    private @Nullable String currency;
    private @NotNull String orderColumn = "create_time";
    private @NotNull EnumSort orderType = EnumSort.DESC;
    private boolean onlyOutOfStock = false;

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

    public @NotNull String orderColumn() {
        return orderColumn;
    }

    public @NotNull EnumSort orderType() {
        return orderType;
    }

    public boolean onlyOutOfStock() {
        return onlyOutOfStock;
    }

    public Searching playerId(String playerId) {
        this.playerId = playerId;
        return this;
    }

    public Searching type(EnumMarketType type) {
        this.type = type;
        return this;
    }

    public Searching currency(String currency) {
        this.currency = currency;
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

    public String generateConditions() {
        StringBuilder sb = new StringBuilder(onlyOutOfStock ? "`amount`=0 " : "`amount`>0 ");
        if (outdated) {
            sb.append("AND (`outdate_time` IS NULL OR `outdate_time` >= ?) ");
        } else {
            sb.append("AND (`outdate_time` IS NOT NULL AND `outdate_time` < ?) ");
        }
        if (type != null) sb.append("AND `type`=? ");
        if (currency != null) sb.append("AND `currency`=? ");
        if (playerId != null) sb.append("AND `player`=? ");
        return sb.toString();
    }

    public String generateOrder() {
        if (orderColumn != null && orderType != null) {
            return "ORDER BY `" + orderColumn + "` " + orderType.value() + " ";
        }
        return "";
    }

    public void setValues(PreparedStatement ps, int parameterIndex) throws SQLException {
        int i = parameterIndex;
        ps.setTimestamp(i, Timestamp.from(Instant.now()));
        if (type != null) ps.setInt(++i, type.value());
        if (currency != null) ps.setString(++i, currency);
        if (playerId != null) ps.setString(++i, playerId);
    }

    public static Searching of(boolean outdated) {
        return new Searching(outdated);
    }
}
