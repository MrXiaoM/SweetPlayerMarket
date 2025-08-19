package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@AutoRegister
public class GuiMyItems extends AbstractGuiSearch {
    public GuiMyItems(SweetPlayerMarket plugin) {
        super(plugin, "gui/my-items.yml");
    }

    LoadedIcon iconClaim;
    @Override
    protected void reloadMenuConfig(YamlConfiguration config) {
        super.reloadMenuConfig(config);
        iconClaim = null;
    }

    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon icon) {
        super.loadMainIcon(section, id, icon);
        if (id.equals("领")) {
            iconClaim = icon;
        }
    }

    @Override
    protected LoadedIcon decideIconByMarketItem(SearchGui instance, Player player, MarketItem item, ListPair<String, Object> r) {
        if (item.noticeFlag() == 1) {
            int amountCanTake = 0;
            if (item.type().equals(EnumMarketType.SELL)) {
                amountCanTake = item.params().getInt("sell.received-count");
            }
            if (item.type().equals(EnumMarketType.BUY)) {
                List<?> list = item.params().getList("buy.received-items");
                amountCanTake = list == null ? 0 : list.size();
            }
            if (amountCanTake > 0) {
                r.add("%amount_can_take%", amountCanTake);
                return iconClaim;
            }
        }
        return iconItem;
    }

    public static GuiMyItems inst() {
        return instanceOf(GuiMyItems.class);
    }

    public static Impl create(Player player, Searching searching) {
        GuiMyItems self = inst();
        return self.new Impl(player, searching);
    }

    public class Impl extends SearchGui {
        private final String playerId;
        protected Impl(Player player, Searching searching) {
            super(player, searching);
            this.playerId = plugin.getKey(player);
            List<String> columnList = plugin.displayNames().columnList();
            for (int i = 0; i < columnList.size(); i++) {
                if (columnList.get(i).equals("create_time")) {
                    columnIndex = i;
                    break;
                }
            }
            postInit();
        }

        @Override
        protected void onClickMarketItem(InventoryAction action, ClickType click, InventoryType.SlotType slotType, int slot, MarketItem item, int i, InventoryView view, InventoryClickEvent event) {
            if (item.amount() == 0) {
                actionLock = false;
                Messages.Gui.common__item_not_found.tm(player);
                return;
            }
            if (click.isLeftClick()) {
                String currencyName;
                Runnable successAction = null;
                try (Connection conn = plugin.getConnection()) {
                    MarketplaceDatabase db = plugin.getMarketplace();
                    MarketItem marketItem = db.getItem(conn, item.shopId());
                    if (marketItem == null || !marketItem.playerId().equals(playerId)) {
                        items.set(i, item.toBuilder().amount(0).build());
                        actionLock = false;
                        Messages.Gui.common__item_not_found.tm(player);
                        return;
                    }
                    currencyName = plugin.displayNames().getCurrencyName(marketItem.currencyName());
                    ConfigurationSection params = marketItem.params();
                    if (marketItem.type().equals(EnumMarketType.SELL)) {
                        IEconomy currency = marketItem.currency();
                        if (currency == null) {
                            actionLock = false;
                            Messages.Gui.common__currency_not_found.tm(player, Pair.of("%currency%", currencyName));
                            return;
                        }
                        double money = params.getDouble("sell.received-currency");
                        params.set("sell.received-currency", null);
                        params.set("sell.received-count", null);
                        successAction = () -> {
                            currency.giveMoney(player, money);
                            Messages.Gui.me__claim__sell__success.tm(player,
                                    Pair.of("%money%", String.format("%.2f", money).replace(".00", "")),
                                    Pair.of("%currency%", currencyName));
                        };
                    }
                    if (marketItem.type().equals(EnumMarketType.BUY)) {
                        List<ItemStack> itemList = new ArrayList<>();
                        int totalCount = 0;
                        ItemStack sampleItem = null;
                        for (Object obj : params.getList("buy.received-items", new ArrayList<>())) {
                            if (obj instanceof ItemStack) {
                                ItemStack itemStack = (ItemStack) obj;
                                totalCount += itemStack.getAmount();
                                if (sampleItem == null) {
                                    sampleItem = itemStack;
                                }
                                itemList.add(itemStack);
                            }
                        }
                        if (sampleItem == null) {
                            actionLock = false;
                            return;
                        }
                        params.set("buy.received-items", null);
                        ItemStack _item = sampleItem;
                        int _total = totalCount;
                        successAction = () -> {
                            ItemStackUtil.giveItemToPlayer(player, itemList);
                            Messages.Gui.me__claim__buy__success.tm(player,
                                    Pair.of("%item%", plugin.displayNames().getDisplayName(_item, player)),
                                    Pair.of("%total_count%", _total));
                        };
                    }
                    if (successAction == null) {
                        Messages.Gui.me__claim__plugin_too_old.tm(player);
                        actionLock = false;
                        return;
                    }
                    // 提交更改到数据库
                    if (!db.modifyItem(conn, marketItem.toBuilder()
                            .noticeFlag(0)
                            .params(params)
                            .build()
                    )) {
                        Messages.Gui.sell__submit_failed.tm(player);
                        actionLock = false;
                        return;
                    }
                } catch (SQLException e) {
                    warn("玩家 " + player.getName() + " 在领取自己的商品 " + item.shopId() + " 时出现异常", e);
                    player.closeInventory();
                    Messages.Gui.me__claim__exception.tm(player);
                    return;
                }
                successAction.run();
                doSearch(true);
                return;
            }
            actionLock = false;
        }
    }
}
