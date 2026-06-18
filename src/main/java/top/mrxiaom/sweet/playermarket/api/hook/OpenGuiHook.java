package top.mrxiaom.sweet.playermarket.api.hook;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.MarketItem;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.gui.GuiMarketplace;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.util.List;

public class OpenGuiHook {
    private final Player player;
    private final IContext context;
    private boolean cancelled = false;
    public OpenGuiHook(@NotNull Player player, @NotNull IContext context) {
        this.player = player;
        this.context = context;
    }

    public IContext getContext() {
        return context;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public interface IContext {}

    public abstract static class AbstractContextConfirm implements IContext {
        private GuiMarketplace.Impl parent;
        private MarketItem marketItem;
        public AbstractContextConfirm(GuiMarketplace.Impl parent, MarketItem marketItem) {
            this.parent = parent;
            this.marketItem = marketItem;
        }

        public GuiMarketplace.Impl parent() {
            return parent;
        }

        public void parent(GuiMarketplace.Impl parent) {
            this.parent = parent;
        }

        public MarketItem marketItem() {
            return marketItem;
        }

        public void marketItem(MarketItem marketItem) {
            this.marketItem = marketItem;
        }
    }

    public static class ContextConfirmBuy extends AbstractContextConfirm {
        public ContextConfirmBuy(GuiMarketplace.Impl parent, MarketItem marketItem) {
            super(parent, marketItem);
        }
    }

    public static class ContextConfirmSell extends AbstractContextConfirm {
        public ContextConfirmSell(GuiMarketplace.Impl parent, MarketItem marketItem) {
            super(parent, marketItem);
        }
    }

    public abstract static class AbstractContextDeploy implements IContext {
        private @Nullable String systemName;
        public AbstractContextDeploy(@Nullable String systemName) {
            this.systemName = systemName;
        }

        public @Nullable String systemName() {
            return systemName;
        }

        public void systemName(@Nullable String systemName) {
            this.systemName = systemName;
        }
    }

    public static class ContextCreateBuyShop extends AbstractContextDeploy {
        public ContextCreateBuyShop(@Nullable String systemName) {
            super(systemName);
        }
    }

    public static class ContextCreateSellShop extends AbstractContextDeploy {
        public ContextCreateSellShop(@Nullable String systemName) {
            super(systemName);
        }
    }

    public abstract static class AbstractContextSearch implements IContext {
        private Searching searching;
        public AbstractContextSearch(Searching searching) {
            this.searching = searching;
        }

        public Searching searching() {
            return searching;
        }

        public void searching(Searching searching) {
            this.searching = searching;
        }
    }

    public static class ContextMarketplace extends AbstractContextSearch {
        public ContextMarketplace(Searching searching) {
            super(searching);
        }
    }

    public static class ContextMyItems extends AbstractContextSearch {
        public ContextMyItems(Searching searching) {
            super(searching);
        }
    }

    public static class ContextPreview implements IContext {
        private IGuiHolder parent;
        private List<ItemStack> items;
        public ContextPreview(IGuiHolder parent, List<ItemStack> items) {
            this.parent = parent;
            this.items = items;
        }

        public IGuiHolder parent() {
            return parent;
        }

        public void parent(IGuiHolder parent) {
            this.parent = parent;
        }

        public List<ItemStack> items() {
            return items;
        }

        public void items(List<ItemStack> items) {
            this.items = items;
        }
    }

    public static class ContextTagList implements IContext {
        private AbstractGuiSearch.SearchGui parent;
        public ContextTagList(AbstractGuiSearch.SearchGui parent) {
            this.parent = parent;
        }

        public AbstractGuiSearch.SearchGui parent() {
            return parent;
        }

        public void parent(AbstractGuiSearch.SearchGui parent) {
            this.parent = parent;
        }
    }

    public static boolean test(Player player, IContext context) {
        OpenGuiHook event = new OpenGuiHook(player, context);
        ((SweetPlayerMarket.API) SweetPlayerMarket.api()).callOpenGuiHook(event);
        return !event.isCancelled();
    }
}
