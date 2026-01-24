package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiCanGoBack;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

@AutoRegister
public class GuiTagList extends AbstractGuiCanGoBack {
    public GuiTagList(SweetPlayerMarket plugin) {
        super(plugin, "tag-list.yml");
    }

    @Override
    protected void loadMainIcon(ConfigurationSection section, String id, LoadedIcon icon) {

    }

    public static GuiTagList inst() {
        return instanceOf(GuiTagList.class);
    }

    public static Impl create(Player player, @Nullable AbstractGuiSearch.SearchGui parent) {
        GuiTagList self = inst();
        return self.new Impl(player, parent);
    }

    public class Impl extends CanGoBackGui<AbstractGuiSearch.SearchGui> {
        private @Nullable String tag = null;
        protected Impl(Player player, @Nullable AbstractGuiSearch.SearchGui parent) {
            super(player, parent);
            if (parent != null) {
                tag = parent.searching().tag();
            }
        }

        public void setTag(@Nullable String tag) {
            this.tag = tag;
            if (parent != null) {
                parent.searching().tag(tag);
                parent.resetPage();
            }
        }

        @Override
        public void goBack() {
            if (parent != null) {
                parent.doSearch();
                parent.open();
            } else {
                GuiMarketplace
                        .create(player, Searching.of(false).tag(tag))
                        .open();
            }
        }
    }
}
