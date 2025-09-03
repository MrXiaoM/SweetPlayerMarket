package top.mrxiaom.sweet.playermarket.actions;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.ItemStackUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.database.MarketplaceDatabase;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;
import top.mrxiaom.sweet.playermarket.utils.Utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActionClaim extends AbstractActionWithMarketItem {
    public static final ActionClaim INSTANCE = new ActionClaim();
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.equals("[claim]") || s.equals("claim")) return INSTANCE;
        return null;
    };
    private ActionClaim() {}

    @Override
    public void run(@NotNull Player player, @NotNull MarketItem item, @NotNull List<Pair<String, Object>> replacements) {
        IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
        if (gui instanceof AbstractGuiSearch.SearchGui) {
            AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
            SweetPlayerMarket plugin = gm.plugin;
            if (item.noticeFlag() == 0) return;

            Runnable successAction = null;
            try (Connection conn = plugin.getConnection()) {
                MarketplaceDatabase db = plugin.getMarketplace();
                MarketItem marketItem = db.getItem(conn, item.shopId());
                if (marketItem == null || !marketItem.playerId().equals(plugin.getKey(player))) {
                    Object i = Utils.get(replacements, "__internal__index");
                    if (i instanceof Integer) {
                        gm.setItem((int) i, item.toBuilder().amount(0).build());
                    }
                    Messages.Gui.common__item_not_found.tm(player);
                    return;
                }
                String currencyName = plugin.displayNames().getCurrencyName(marketItem.currencyName());
                ConfigurationSection params = marketItem.params();
                if (marketItem.type().equals(EnumMarketType.SELL)) {
                    IEconomy currency = marketItem.currency();
                    if (currency == null) {
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
                        return;
                    }
                    params.set("buy.received-items", null);
                    ItemStack _item = sampleItem;
                    int _total = totalCount;
                    successAction = () -> {
                        ItemStackUtil.giveItemToPlayer(player, itemList);
                        MiniMessage miniMessage = AdventureItemStack.wrapHoverEvent(_item).build();
                        Messages.Gui.me__claim__buy__success.tm(miniMessage, player,
                                Pair.of("%item%", plugin.displayNames().getDisplayName(_item, player)),
                                Pair.of("%total_count%", _total));
                    };
                }
                if (successAction == null) {
                    Messages.Gui.me__claim__plugin_too_old.tm(player);
                    return;
                }
                // 提交更改到数据库
                if (!db.modifyItem(conn, marketItem.toBuilder()
                        .noticeFlag(0)
                        .params(params)
                        .build()
                )) {
                    Messages.Gui.me__claim__submit_failed.tm(player);
                    return;
                }
            } catch (SQLException e) {
                plugin.warn("玩家 " + player.getName() + " 在领取自己的商品 " + item.shopId() + " 时出现异常", e);
                player.closeInventory();
                Messages.Gui.me__claim__exception.tm(player);
                return;
            }
            successAction.run();
            gm.doSearch();
            gm.open();
        }
    }
}
