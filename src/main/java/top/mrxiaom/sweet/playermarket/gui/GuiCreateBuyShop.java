package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.commands.arguments.CreateArguments;
import top.mrxiaom.sweet.playermarket.data.EnumMarketType;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiDeploy;

@AutoRegister
public class GuiCreateBuyShop extends AbstractGuiDeploy {
    public GuiCreateBuyShop(SweetPlayerMarket plugin) {
        super(plugin, "create-buy-shop.yml");
    }

    public static GuiCreateBuyShop inst() {
        return instanceOf(GuiCreateBuyShop.class);
    }

    /**
     * 创建上架菜单实例，以玩家身份上架商品
     * @param player 玩家
     */
    public static Impl create(Player player) {
        return create(player, null);
    }

    /**
     * 创建上架菜单实例
     * @param player 玩家
     * @param systemName 如果不为 <code>null</code>，则以系统身份上架商品
     */
    public static Impl create(Player player, @Nullable String systemName) {
        GuiCreateBuyShop self = inst();
        return self.new Impl(player, systemName);
    }

    public class Impl extends DeployGui {
        private final @Nullable String systemName;
        protected Impl(Player player, @Nullable String systemName) {
            super(player, EnumMarketType.BUY);
            this.systemName = systemName;
        }

        @Override
        protected void checkNeedToLockAction(char id) {
            if (id == '确') {
                actionLock = true;
            }
        }

        @Override
        protected void onClickConfirm(
                InventoryAction action, ClickType click,
                InventoryType.SlotType slotType, int slot,
                InventoryView view, InventoryClickEvent event
        ) {
            actionLock = true;
            if (sampleItem == null) {
                Messages.Command.create__no_item_selected.tm(player);
                actionLock = false;
                return;
            }
            // 上架流程与命令保持一致
            plugin.getScheduler().runTask(() -> CreateArguments.doDeployMarketItem(
                    plugin, player, systemName,
                    sampleItem, sampleItem.getAmount(),
                    amount, type,
                    price, currency,
                    this::callback
            ));
        }

        private void callback(MarketItem marketItem) {
            if (marketItem != null) {
                player.closeInventory();
            } else {
                actionLock = false;
            }
        }
    }
}
