package top.mrxiaom.sweet.playermarket.database;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.pluginbase.utils.Bytes;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.ItemTagResolver;
import top.mrxiaom.sweet.playermarket.data.*;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.func.AbstractPluginHolder;
import top.mrxiaom.sweet.playermarket.func.ItemTagManager;
import top.mrxiaom.sweet.playermarket.utils.ListX;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MarketplaceDatabase extends AbstractPluginHolder implements IDatabase {
    public class SearchHolder {
        private final int page, size;
        private final Searching searching;
        public SearchHolder(int page, int size, boolean outdated) {
            this.page = page;
            this.size = size;
            this.searching = Searching.of(outdated);
        }

        public SearchHolder player(Player player) {
            searching.playerId(plugin.getKey(player));
            return this;
        }

        public SearchHolder player(String playerId) {
            searching.playerId(playerId);
            return this;
        }

        public SearchHolder type(EnumMarketType type) {
            searching.type(type);
            return this;
        }

        public SearchHolder currency(String currency) {
            searching.currency(currency);
            return this;
        }

        public SearchHolder orderBy(String column, EnumSort sort) {
            searching.orderBy(column, sort);
            return this;
        }

        public SearchHolder onlyOutOfStock(boolean onlyOutOfStock) {
            searching.onlyOutOfStock(onlyOutOfStock);
            return this;
        }

        public SearchHolder noticeFlag(@Nullable NoticeFlag flag) {
            searching.noticeFlag(flag);
            return this;
        }

        public SearchHolder noticeFlag(@Nullable NoticeFlag flag, boolean noticeReversed) {
            searching.noticeFlag(flag, noticeReversed);
            return this;
        }

        public SearchHolder notice(@Nullable Integer notice) {
            searching.notice(notice);
            return this;
        }

        public SearchHolder notice(@Nullable Integer notice, boolean noticeReversed) {
            searching.notice(notice, noticeReversed);
            return this;
        }

        public ListX<MarketItem> search() {
            return getItems(page, size, searching);
        }
    }
    private String TABLE_MARKETPLACE;
    private Integer totalCount = null;
    private final Map<String, Integer> tagCountCache = new HashMap<>();
    public MarketplaceDatabase(SweetPlayerMarket plugin) {
        super(plugin, true);
        registerBungee();
        plugin.getScheduler().runTaskTimerAsync(this::fetchAllCountCache, 15 * 20L, 15 * 20L);
    }

    @Override
    public void reload(Connection conn, String tablePrefix) throws SQLException {
        TABLE_MARKETPLACE = tablePrefix + "marketplace";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_MARKETPLACE + "`(" +
                        "`shop_id` VARCHAR(48) PRIMARY KEY," + // 商品 ID
                        "`player` VARCHAR(48)," +              // 商家的玩家 ID
                        "`shop_type` INT," +                   // 商品类型
                        "`create_time` DATETIME," +            // 商品上架时间
                        "`outdate_time` DATETIME NULL," +      // 商品到期时间 (NULL 代表永不过期)
                        "`currency` VARCHAR(48)," +            // 商品使用货币
                        "`price` VARCHAR(24)," +               // 商品价格
                        "`amount` INT," +                      // 商品数量
                        "`notice_flag` INT," +                 // 提醒标记
                        "`tag` VARCHAR(48)," +                 // 商品标签
                        "`data` LONGTEXT" +                    // 商品数据，包括物品以及额外参数
                ");"
        )) { ps.execute(); }
        fetchAllCountCache(conn);
    }

    public void fetchAllCountCache() {
        try (Connection conn = plugin.getConnection()) {
            fetchAllCountCache(conn);
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void fetchAllCountCache(Connection conn) throws SQLException {
        ItemTagManager manager = getOrNull(ItemTagManager.class);
        if (manager != null) {
            totalCount = getTotalCount(conn, Searching.of(false));
            for (String tag : manager.getNamedTags()) {
                getTagCount(conn, tag);
            }
        }
    }

    private List<MarketItem> queryAndLoadItems(PreparedStatement ps) throws SQLException {
        try (ResultSet resultSet = ps.executeQuery()) {
            return loadItemsFromResult(resultSet);
        }
    }

    private List<MarketItem> loadItemsFromResult(ResultSet result) throws SQLException {
        List<MarketItem> items = new ArrayList<>();
        while (result.next()) {
            MarketItem marketItem = loadItemFromResult(result);
            if (marketItem != null) {
                items.add(marketItem);
            }
        }
        return items;
    }

    private MarketItem loadItemFromResult(ResultSet result) throws SQLException {
        String shopId = result.getString("shop_id");
        String playerId = result.getString("player");
        int typeInt = result.getInt("shop_type");
        EnumMarketType type = EnumMarketType.valueOf(typeInt);
        if (type == null) {
            warn("商品 " + shopId + " 的类型ID不正确 (" + typeInt + ")，请检查插件是否已更新到最新版本");
            return null;
        }
        LocalDateTime createTime = Searching.format(result.getString("create_time"));
        LocalDateTime outdate = Searching.format(result.getString("outdate_time"));
        String currencyName = result.getString("currency");
        IEconomy currency = plugin.parseEconomy(currencyName);
        String priceStr = result.getString("price");
        Double price = Util.parseDouble(priceStr).orElse(null);
        if (price == null) {
            warn("商品 " + shopId + " 的价格不正确 (" + priceStr + ")");
            return null;
        }
        int amount = result.getInt("amount");
        int noticeFlag = result.getInt("notice_flag");
        String tag = result.getString("tag");
        Reader reader = new StringReader(result.getString("data"));
        YamlConfiguration data = YamlConfiguration.loadConfiguration(reader);
        try {
            return new MarketItem(shopId, playerId, type, createTime, outdate, currencyName, currency, price, amount, noticeFlag, tag, data);
        } catch (Throwable t) {
            warn("商品 " + shopId + " 在读取时出现错误: " + t.getMessage());
            return null;
        }
    }

    public SearchHolder queryItems(int page, int size) {
        return new SearchHolder(page, size, false);
    }

    public SearchHolder queryItemsOutdated(int page, int size) {
        return new SearchHolder(page, size, true);
    }

    /**
     * 获取所有商品
     * @param page 第几页
     * @param size 一页应该放多少个商品
     * @param searching 搜素参数
     * @return 搜索结果
     */
    public ListX<MarketItem> getItems(int page, int size, @NotNull Searching searching) {
        String sql = null;
        try (Connection conn = plugin.getConnection()) {
            String conditions = searching.generateConditions();
            String order = searching.generateOrder();
            ListX<MarketItem> list;
            int startIndex = (page - 1) * size;
            sql = "SELECT * FROM `" + TABLE_MARKETPLACE + "` "
                    + "WHERE " + conditions + order
                    + "LIMIT " + startIndex + ", " + size + ";";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                searching.setValues(ps, 1);
                list = new ListX<>(queryAndLoadItems(ps));
            }
            list.setTotalCount(getTotalCount(conn, searching));
            return list;
        } catch (SQLException e) {
            if (e instanceof SQLSyntaxErrorException) {
                warn("在运行数据库语句 { " + sql + " } 时出现异常", e);
            } else {
                warn(e);
            }
            return new ListX<>();
        }
    }

    public int getTotalCount(Connection conn, @NotNull Searching searching) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM `" + TABLE_MARKETPLACE + "` WHERE " + searching.generateConditions() + ";"
        )) {
            searching.setValues(ps, 1);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * 获取符合条件的商品数量
     * @param searching 搜索参数
     * @return 如果没有商品，或者数据库错误，将会返回 <code>0</code>
     */
    public int getTotalCount(@NotNull Searching searching) {
        try (Connection conn = plugin.getConnection()) {
            return getTotalCount(conn, searching);
        } catch (SQLException e) {
            warn(e);
            return 0;
        }
    }

    public int getTotalCountWithCache() {
        if (totalCount != null) return totalCount;
        return totalCount = getTotalCount(Searching.of(false));
    }

    public int getTagCountWithCache(String tag) {
        Integer cache = tagCountCache.get(tag);
        if (cache != null) return cache;
        return getTagCount(tag);
    }

    public int getTagCount(String tag) {
        try (Connection conn = plugin.getConnection()) {
            return getTagCount(conn, tag);
        } catch (SQLException e) {
            warn(e);
            return 0;
        }
    }

    public int getTagCount(Connection conn, String tag) throws SQLException {
        StringJoiner conditions = new StringJoiner(" AND ");
        conditions.add("`tag` = ?");
        conditions.add("`amount` > 0");
        conditions.add("(`outdate_time` IS NULL OR `outdate_time` >= '" + Searching.format(LocalDateTime.now()) + "')");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT count(*) FROM `" + TABLE_MARKETPLACE + "` WHERE " + conditions + ";"
        )) {
            ps.setString(1, tag);
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    int count = result.getInt(1);
                    tagCountCache.put(tag, count);
                    return count;
                }
            }
        }
        return 0;
    }

    /**
     * 获取指定商品信息
     * @param shopId 商品ID
     */
    @Nullable
    public MarketItem getItem(@NotNull String shopId) {
        try (Connection conn = plugin.getConnection()) {
            return getItem(conn, shopId);
        } catch (SQLException e) {
            warn(e);
        }
        return null;
    }

    @Nullable
    public MarketItem getItem(Connection conn, @NotNull String shopId) throws SQLException {
        return getItem(conn, shopId, false);
    }

    @Nullable
    public MarketItem getItem(Connection conn, @NotNull String shopId, boolean ignoreAmount) throws SQLException {
        String sql = ignoreAmount
                ? ("SELECT * FROM `" + TABLE_MARKETPLACE + "` WHERE `shop_id`=? LIMIT 1;")
                : ("SELECT * FROM `" + TABLE_MARKETPLACE + "` WHERE `shop_id`=? AND `amount`>0 LIMIT 1;");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shopId);
            try (ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return loadItemFromResult(resultSet);
                }
            }
        }
        return null;
    }

    /**
     * 提交商品的 货币、价格、数量 修改
     * @param item 商品
     * @return 修改是否提交成功
     */
    public boolean modifyItem(MarketItem item) {
        try (Connection conn = plugin.getConnection()) {
            return modifyItem(conn, item);
        } catch (SQLException e) {
            warn(e);
        }
        return false;
    }

    public boolean modifyItem(Connection conn, MarketItem item) throws SQLException {
        boolean result;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE `" + TABLE_MARKETPLACE + "` "
                        + "SET `currency`=?, `price`=?, `amount`=?, `notice_flag`=?, `data`=? "
                        + "WHERE `shop_id`=?;"
        )) {
            ps.setString(1, item.currencyName());
            ps.setString(2, String.format("%.2f", item.price()));
            ps.setInt(3, item.amount());
            ps.setInt(4, item.noticeFlag());
            ps.setString(5, item.data().saveToString());
            ps.setString(6, item.shopId());
            result = ps.executeUpdate() != 0;
        }
        if (item.amount() <= 0) {
            sendRemoveCache(item.tag());
        }
        return result;
    }

    public int recalculateItemsTag() {
        return recalculateItemsTag(plugin.itemTagResolver());
    }

    public int recalculateItemsTag(ItemTagResolver resolver) {
        try (Connection conn = plugin.getConnection()) {
            List<MarketItem> items;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `" + TABLE_MARKETPLACE + "` WHERE `amount`>0;"
            );
                ResultSet result = ps.executeQuery()) {
                items = loadItemsFromResult(result);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE `" + TABLE_MARKETPLACE + "` "
                            + "SET `tag`=? WHERE `shop_id`=?;"
            )) {
                int count = 0;
                for (MarketItem item : items) {
                    String newTagRaw = resolver.resolve(item);
                    String newTag = newTagRaw == null ? "default" : newTagRaw;
                    if (!item.tag().equals(newTag)) {
                        ps.setString(1, newTag);
                        ps.setString(2, item.shopId());
                        ps.addBatch();
                        count++;
                    }
                }
                if (count > 0) {
                    ps.executeBatch();
                }
                return count;
            }
        } catch (SQLException e) {
            warn(e);
            return -1;
        }
    }

    @Nullable
    public String createNewId(Connection conn) {
        return createNewId(conn, 0);
    }

    @Nullable
    public String createNewId(Connection conn, int times) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM `" + TABLE_MARKETPLACE + "` WHERE `shop_id`=? LIMIT 1;"
        )) {
            String shopId = UUID.randomUUID().toString();
            ps.setString(1, shopId);
            if (!ps.executeQuery().next()) {
                return shopId;
            }
        } catch (SQLException e) {
            warn(e);
            return null;
        }
        int i = times + 1;
        if (i >= 514) {
            return null;
        } else {
            return createNewId(conn, i);
        }
    }

    /**
     * 添加商品到商店中
     */
    public boolean putItem(@NotNull MarketItem item) {
        try (Connection conn = plugin.getConnection()) {
            putItem(conn, item);
            return true;
        } catch (SQLException e) {
            warn(e);
            return false;
        }
    }

    public void putItem(Connection conn, @NotNull MarketItem item) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `" + TABLE_MARKETPLACE + "` "
                + "(`shop_id`,`player`,`shop_type`,`create_time`,`outdate_time`,`currency`,`price`,`amount`,`notice_flag`,`tag`,`data`) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?);"
        )) {
            ps.setString(1, item.shopId());
            ps.setString(2, item.playerId());
            ps.setInt(3, item.type().value());
            ps.setObject(4, Searching.format(item.createTime()));
            LocalDateTime outdateTime = item.outdateTime();
            if (outdateTime == null) {
                ps.setNull(5, Types.TIMESTAMP);
            } else {
                ps.setObject(5, Searching.format(outdateTime));
            }
            ps.setString(6, item.currencyName());
            ps.setString(7, String.format("%.2f", item.price()));
            ps.setInt(8, item.amount());
            ps.setInt(9, item.noticeFlag());
            ps.setString(10, item.tag());
            ps.setString(11, item.data().saveToString());
            ps.execute();
        }
        sendRemoveCache(item.tag());
    }

    @Override
    public void receiveBungee(String subChannel, DataInputStream in) throws IOException {
        if (subChannel.equals("SPM_CountCache")) {
            String tag = in.readUTF();
            tagCountCache.remove(tag);
            totalCount = null;
        }
    }

    private void sendRemoveCache(String tag) {
        if (tag == null) return;
        tagCountCache.remove(tag);
        totalCount = null;
        Bytes.sendByWhoeverOrNot("BungeeCord", Bytes.build(out -> {
            out.writeUTF(tag);
        }, /*subChannel:*/"Forward", /*arguments:*/"ALL", "SPM_CountCache"));
    }
}
