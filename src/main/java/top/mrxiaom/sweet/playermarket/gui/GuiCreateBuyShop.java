package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
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

    public static Impl create(Player player) {
        GuiCreateBuyShop self = inst();
        return self.new Impl(player);
    }

    public class Impl extends DeployGui {
        protected Impl(Player player) {
            super(player, EnumMarketType.BUY);
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
            CreateArguments.doDeployMarketItem(
                    plugin, player,
                    sampleItem, sampleItem.getAmount(),
                    amount, type,
                    price, currency,
                    this::callback
            );
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
