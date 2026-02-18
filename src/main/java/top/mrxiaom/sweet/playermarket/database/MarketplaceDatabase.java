package top.mrxiaom.sweet.playermarket.database;

import com.zaxxer.hikari.HikariConfig;
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

import java.io.*;
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
    private String TABLE_SEARCH_INDEX;
    private Integer totalCount = null;
    private final Map<String, Integer> tagCountCache = new HashMap<>();
    private boolean enableKeywordSearch;
    private SQLiteLibSimple libSimple;
    public MarketplaceDatabase(SweetPlayerMarket plugin) {
        super(plugin, true);
        registerBungee();
        plugin.getScheduler().runTaskTimerAsync(this::fetchAllCountCache, 15 * 20L, 15 * 20L);
    }

    @Override
    public void beforeReload(HikariConfig hikariConfig, YamlConfiguration config) {
        if (plugin.options.database().isSQLite()) {
            // 为 SQLite 开启加载扩展支持
            Properties sqliteProps = new Properties();
            sqliteProps.put("enable_load_extension", "true");
            hikariConfig.setDataSourceProperties(sqliteProps);
        }
    }

    @Override
    public void reload(Connection conn, String tablePrefix) throws SQLException {
        TABLE_MARKETPLACE = tablePrefix + "marketplace";
        TABLE_SEARCH_INDEX = tablePrefix + "search_index";
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
        boolean createIndexTable;
        try (ResultSet result = conn.getMetaData().getTables(
                null, null, TABLE_SEARCH_INDEX, new String[] { "TABLE" }
        )) { createIndexTable = !result.next(); }

        boolean enableKeywordSearch = true;
        if (createIndexTable) {
            if (plugin.options.database().isMySQL()) {
                // 创建索引表
                try (PreparedStatement ps = conn.prepareStatement(
                        "CREATE TABLE `" + TABLE_SEARCH_INDEX + "`(" +
                            "`shop_id` VARCHAR(48) PRIMARY KEY," + // 商品 ID
                            "`content` TEXT," +                    // 物品名称、Lore
                            "FULLTEXT(`content`) WITH PARSER ngram" +
                        ");"
                )) {
                    ps.execute();
                }
            }
            if (plugin.options.database().isSQLite()) {
                File sqliteFolder = new File(plugin.getDataFolder(), "sqlite");
                try (Statement stat = conn.createStatement()) {
                    // 加载扩展
                    libSimple = SQLiteLibSimple.init(sqliteFolder, stat);
                    // 创建索引表
                    stat.execute("CREATE VIRTUAL TABLE `" + TABLE_SEARCH_INDEX + "` USING FTS5(`shop_id`,`content`,tokenize='simple')");
                } catch (Exception e) {
                    enableKeywordSearch = false;
                    if (!sqliteFolder.exists()) {
                        Util.mkdirs(sqliteFolder);
                    }
                    onSqliteLibSimpleInitFail(e);
                }
            }
            this.enableKeywordSearch = enableKeywordSearch;
            if (enableKeywordSearch) {
                recalculateIndex(conn);
            }
        } else {
            if (plugin.options.database().isSQLite()) {
                File sqliteFolder = new File(plugin.getDataFolder(), "sqlite");
                try (Statement stat = conn.createStatement()) {
                    // 加载扩展
                    libSimple = SQLiteLibSimple.init(sqliteFolder, stat);
                } catch (Exception e) {
                    enableKeywordSearch = false;
                    if (!sqliteFolder.exists()) {
                        Util.mkdirs(sqliteFolder);
                    }
                    onSqliteLibSimpleInitFail(e);
                }
            }
            this.enableKeywordSearch = enableKeywordSearch;
        }
        fetchAllCountCache(conn);
    }

    private void onSqliteLibSimpleInitFail(Exception e) {
        warn("初始化索引失败: " + e.getMessage());
        warn("当前 SQLite 环境未安装 simple tokenizer，请先按你的系统类型进行安装。");
        String pluginName = plugin.getDescription().getName();
        String libName = System.getProperty("os.name").toLowerCase().contains("win")
                ? "simple.dll"
                : "libsimple.*";
        warn("从以下链接下载，确保解压后 plugins/" + pluginName + "/sqlite/" + libName + " 文件存在。");
        warn("https://github.com/wangfenjin/simple/releases/latest");
        warn("如果你不需要搜索商品功能，或者打算使用 MySQL，可以忽略这个警告。");
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
            String keyword = searching.keyword();
            int startIndex = (page - 1) * size;
            int parameterIndex;
            if (keyword != null && enableKeywordSearch) {
                parameterIndex = 2;
                String conditions = searching.generateConditions("m.");
                String order = searching.generateOrder("m.");
                if (plugin.options.database().isMySQL()) {
                    sql = "SELECT m.* FROM " +
                            "`" + TABLE_MARKETPLACE + "` m " +
                            "INNER JOIN `" + TABLE_SEARCH_INDEX + "` si ON m.`shop_id` = si.`shop_id` " +
                            "WHERE MATCH(si.`content`) AGAINST(? IN NATURAL LANGUAGE MODE) " +
                            "AND " + conditions + order +
                            "LIMIT " + startIndex + ", " + size + ";";
                } else {
                    sql = "SELECT m.* FROM " +
                            "`" + TABLE_MARKETPLACE + "` m " +
                            "INNER JOIN `" + TABLE_SEARCH_INDEX + "` si ON m.`shop_id` = si.`shop_id` " +
                            "WHERE si.`content` MATCH ? " +
                            "AND " + conditions + order +
                            "LIMIT " + startIndex + ", " + size + ";";
                    try (Statement stat = conn.createStatement()) {
                        libSimple.apply(stat);
                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            return getItems(conn, ps, parameterIndex, searching, keyword);
                        }
                    }
                }
            } else {
                parameterIndex = 1;
                String conditions = searching.generateConditions();
                String order = searching.generateOrder();
                sql = "SELECT * FROM `" + TABLE_MARKETPLACE + "` "
                        + "WHERE " + conditions + order
                        + "LIMIT " + startIndex + ", " + size + ";";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                return getItems(conn, ps, parameterIndex, searching, keyword);
            }
        } catch (SQLException e) {
            if (e instanceof SQLSyntaxErrorException) {
                warn("在运行数据库语句 { " + sql + " } 时出现异常", e);
            } else {
                warn(e);
            }
            return new ListX<>();
        }
    }

    private ListX<MarketItem> getItems(Connection conn, PreparedStatement ps, int parameterIndex, Searching searching, String keyword) throws SQLException {
        if (parameterIndex == 2) {
            ps.setString(1, keyword);
        }
        searching.setValues(ps, parameterIndex);
        ListX<MarketItem> list = new ListX<>(queryAndLoadItems(ps));
        list.setTotalCount(getTotalCount(conn, searching, keyword));
        return list;
    }

    public int getTotalCount(Connection conn, @NotNull Searching searching) throws SQLException {
        return getTotalCount(conn, searching, null);
    }

    private int getTotalCount(Connection conn, @NotNull Searching searching, @Nullable String keyword) throws SQLException {
        String sql;
        int parameterIndex;
        if (keyword != null && enableKeywordSearch) {
            parameterIndex = 2;
            if (plugin.options.database().isMySQL()) {
                sql = "SELECT count(DISTINCT m.`shop_id`) AS total_count FROM " +
                        "`" + TABLE_MARKETPLACE + "` m " +
                        "INNER JOIN `" + TABLE_SEARCH_INDEX + "` si ON m.`shop_id` = si.`shop_id` " +
                        "WHERE MATCH(si.`content`) AGAINST(? IN NATURAL LANGUAGE MODE) " +
                        "AND " + searching.generateConditions("m.") + ";";
            } else {
                sql = "SELECT count(DISTINCT m.`shop_id`) AS total_count FROM " +
                        "`" + TABLE_MARKETPLACE + "` m " +
                        "INNER JOIN `" + TABLE_SEARCH_INDEX + "` si ON m.`shop_id` = si.`shop_id` " +
                        "WHERE si.`content` MATCH simple_query(?) " +
                        "AND " + searching.generateConditions("m.") + ";";
            }
        } else {
            parameterIndex = 1;
            sql = "SELECT count(DISTINCT `shop_id`) AS total_count FROM " +
                    "`" + TABLE_MARKETPLACE + "` " +
                    "WHERE " + searching.generateConditions() + ";";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parameterIndex == 2) {
                ps.setString(1, keyword);
            }
            searching.setValues(ps, parameterIndex);
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
            return getTotalCount(conn, searching, searching.keyword());
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
        String now = Searching.format(LocalDateTime.now());
        String conditions = "`tag` = ? AND `amount` > 0 AND (`outdate_time` IS NULL OR `outdate_time` >= '" + now + "')";
        String sql = "SELECT count(*) FROM `" + TABLE_MARKETPLACE + "` WHERE " + conditions + ";";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
            String sql = "SELECT * FROM `" + TABLE_MARKETPLACE + "` WHERE `amount`>0;";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet result = ps.executeQuery()
            ) {
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

    public int recalculateIndex() {
        if (!enableKeywordSearch) return -1;
        try (Connection conn = plugin.getConnection()) {
            return recalculateIndex(conn);
        } catch (SQLException e) {
            warn(e);
            return -1;
        }
    }

    public int recalculateIndex(Connection conn) throws SQLException {
        if (!enableKeywordSearch) return -1;
        List<MarketItem> items;
        String sql = "SELECT * FROM `" + TABLE_MARKETPLACE + "` WHERE `amount`>0;";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet result = ps.executeQuery()
        ) {
            items = loadItemsFromResult(result);
        }
        return putIndex(conn, items);
    }

    @Nullable
    public String createNewId(Connection conn) {
        return createNewId(conn, 0);
    }

    @Nullable
    public String createNewId(Connection conn, int times) {
        String sql = "SELECT * FROM `" + TABLE_MARKETPLACE + "` WHERE `shop_id`=? LIMIT 1;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO `" + TABLE_SEARCH_INDEX + "` "
                + "(`shop_id`,`content`) "
                + "VALUES(?,?);"
        )) {
            ps.setString(1, item.shopId());
            ps.setString(2, item.searchContent(plugin));
            ps.execute();
        }
        putIndex(conn, Collections.singletonList(item));
        sendRemoveCache(item.tag());
    }

    private int putIndex(Connection conn, @NotNull List<MarketItem> items) throws SQLException {
        if (!enableKeywordSearch) return 0;
        String sentence;
        if (plugin.options.database().isMySQL()) {
            sentence = "INSERT INTO `" + TABLE_SEARCH_INDEX + "` "
                    + "(`shop_id`,`content`) "
                    + "VALUES(?,?) "
                    + "ON DUPLICATE KEY UPDATE `content`= VALUES(`content`);";
        } else {
            try (Statement stat = conn.createStatement()) {
                libSimple.apply(stat);
                // 在 SQLite 下的 FTS5 虚拟表，INSERT OR REPLACE 失效了，要手动删除再增加
                if (!items.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM `" + TABLE_SEARCH_INDEX + "` WHERE `shop_id`=?;")) {
                        if (items.size() == 1) {
                            ps.setString(1, items.get(0).shopId());
                            ps.execute();
                        } else {
                            for (MarketItem item : items) {
                                ps.setString(1, item.shopId());
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }
                }
                sentence = "INSERT INTO `" + TABLE_SEARCH_INDEX + "` "
                        + "(`shop_id`,`content`) "
                        + "VALUES(?,?);";
                try (PreparedStatement ps = conn.prepareStatement(sentence)) {
                    return putIndex(ps, items);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(sentence)) {
            return putIndex(ps, items);
        }
    }

    private int putIndex(PreparedStatement ps, List<MarketItem> items) throws SQLException {
        if (items.size() == 1) {
            MarketItem item = items.get(0);
            ps.setString(1, item.shopId());
            ps.setString(2, item.searchContent(plugin));
            ps.execute();
            return 1;
        }
        int count = 0;
        for (MarketItem item : items) {
            ps.setString(1, item.shopId());
            ps.setString(2, item.searchContent(plugin));
            ps.addBatch();
            count++;
        }
        if (count > 0) {
            ps.executeBatch();
        }
        return count;
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
