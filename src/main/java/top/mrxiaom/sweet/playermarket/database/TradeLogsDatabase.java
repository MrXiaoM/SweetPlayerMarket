package top.mrxiaom.sweet.playermarket.database;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.data.TradeLog;
import top.mrxiaom.sweet.playermarket.func.AbstractPluginHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeLogsDatabase extends AbstractPluginHolder implements IDatabase {
    private String TABLE_NAME;
    private boolean enableTradeLogs;
    public TradeLogsDatabase(SweetPlayerMarket plugin) {
        super(plugin, true);
    }

    public String getTableTradeLogs() {
        return TABLE_NAME;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        enableTradeLogs = config.getBoolean("trade-logs.enable", false);
    }

    @Override
    public void reload(Connection conn, String tablePrefix) throws SQLException {
        TABLE_NAME = tablePrefix + "trade_logs";
        String AUTO_INCREMENT = plugin.options.database().isSQLite() ? "AUTOINCREMENT" : "AUTO_INCREMENT";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_NAME + "`(" +
                        "`id` INTEGER PRIMARY KEY " + AUTO_INCREMENT + "," + // 记录ID
                        "`shop_id` VARCHAR(48)," +     // 商品ID
                        "`shop_owner` VARCHAR(48)," +  // 店主ID
                        "`trade_time` DATETIME," +     // 交易时间
                        "`player_uuid` VARCHAR(48)," + // 购买/卖出商品的玩家
                        "`player_name` VARCHAR(48)," + // 购买/卖出商品的玩家
                        "`amount` INT" +               // 购买/卖出的份数
                ");"
        )) {
            ps.execute();
        }
    }

    private List<TradeLog> collectTradeLog(ResultSet result, MarketItem provided) throws SQLException {
        return collectTradeLog(result, provided, "", "");
    }

    private List<TradeLog> collectTradeLog(ResultSet result, MarketItem provided, String prefix, String marketPrefix) throws SQLException {
        List<TradeLog> list = new ArrayList<>();
        while (result.next()) {
            int id = result.getInt(prefix + "id");
            String shopId = result.getString(prefix + "shop_id");
            String shopOwner = result.getString(prefix + "shop_owner");
            LocalDateTime tradeTime = Searching.format(result.getString(prefix + "trade_time"));
            UUID playerUuid = UUID.fromString(result.getString(prefix + "player_uuid"));
            String playerName = result.getString(prefix + "player_name");
            int amount = result.getInt(prefix + "amount");
            TradeLog log = new TradeLog(id, shopId, shopOwner, tradeTime, playerUuid, playerName, amount);
            if (provided != null) {
                log.item(provided);
            } else {
                log.item(plugin.getMarketplace().loadItemFromResult(result, marketPrefix));
            }
            list.add(log);
        }
        return list;
    }

    public List<TradeLog> getByItem(Connection conn, MarketItem item, int page, int size) throws SQLException {
        int startIndex = (page - 1) * size;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM `" + TABLE_NAME + "`" +
                        " WHERE `shop_id`=?" +
                        " ORDER BY `id` DESC" +
                        " LIMIT " + startIndex + ", " + size + ";"
        )) {
            ps.setString(1, item.shopId());
            try (ResultSet result = ps.executeQuery()) {
                return collectTradeLog(result, item);
            }
        }
    }

    private String joinMarketLogs(String conditions, int page, int size) {
        int startIndex = (page - 1) * size;
        return "SELECT * FROM (" +
                "  SELECT *" +
                "  FROM `" + TABLE_NAME + "`" +
                "  WHERE " + conditions +
                ") AS t" +
                "  JOIN `" + plugin.getMarketplace().TABLE_MARKETPLACE + "` AS m ON t.shop_id = m.shop_id" +
                "  ORDER BY `id` DESC" +
                "  LIMIT " + startIndex + ", " + size + ";";
    }

    public List<TradeLog> getByOwner(Connection conn, Player owner, int page, int size) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                joinMarketLogs("`shop_id` = ?", page, size)
        )) {
            ps.setString(1, plugin.getKey(owner));
            try (ResultSet result = ps.executeQuery()) {
                return collectTradeLog(result, null, "t.", "m.");
            }
        }
    }

    public List<TradeLog> getByPlayer(Connection conn, UUID playerId, int page, int size) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                joinMarketLogs("`player_uuid` = ?", page, size)
        )) {
            ps.setString(1, playerId.toString());
            try (ResultSet result = ps.executeQuery()) {
                return collectTradeLog(result, null, "t.", "m.");
            }
        }
    }

    public void put(Connection conn, MarketItem item, Player player, int amount, LocalDateTime time) throws SQLException {
        if (!enableTradeLogs) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `" + TABLE_NAME + "`(`shop_id`, `shop_owner`, `trade_time`, `player_uuid`, `player_name`, `amount`) VALUES(?, ?, ?, ?, ?, ?);"
        )) {
            ps.setString(1, item.shopId());
            ps.setString(2, item.playerId());
            ps.setString(3, Searching.format(time));
            ps.setString(4, player.getUniqueId().toString());
            ps.setString(5, player.getName());
            ps.setInt(6, amount);
            ps.execute();
        }
    }
}
