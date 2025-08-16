package top.mrxiaom.sweet.playermarket.data;

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
    private @Nullable String orderColumn = "create_time";
    private @Nullable EnumSort orderType = EnumSort.DESC;

    private Searching(boolean outdated) {
        this.outdated = outdated;
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

    public Searching orderBy(String column, EnumSort sort) {
        orderColumn = column;
        orderType = sort;
        return this;
    }

    public String generateConditions() {
        StringBuilder sb = new StringBuilder();
        if (outdated) {
            sb.append("(`outdate_time` IS NULL OR `outdate_time` >= ?) ");
        } else {
            sb.append("(`outdate_time` IS NOT NULL AND `outdate_time` < ?) ");
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
