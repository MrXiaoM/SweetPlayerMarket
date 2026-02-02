package top.mrxiaom.sweet.playermarket.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.gui.LoadedIcon;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.func.ItemTagManager;
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

    @Override
    protected @Nullable ItemStack applyOtherIcon(IGuiHolder instance, Player player, char id, int index, int appearTimes, LoadedIcon icon) {
        ItemTagManager manager = ItemTagManager.inst();
        ListPair<String, Object> r = new ListPair<>();
        r.addAll(manager.getTagDisplayNames());
        if (contains(icon, "%item_count_all%")) {
            r.add("%item_count_all%", plugin.getMarketplace().getTotalCountWithCache());
        }
        for (String tag : manager.getNamedTags()) {
            String key = "%item_count_tag_" + tag + "%";
            if (contains(icon, key)) {
                r.add(key, plugin.getMarketplace().getTagCountWithCache(tag));
            }
        }
        return icon.generateIcon(player, s -> Pair.replace(s, r), l -> Pair.replace(l, r));
    }

    private static boolean contains(LoadedIcon icon, String str) {
        if (icon.display.contains(str)) {
            return true;
        }
        for (String line : icon.lore) {
            if (line.contains(str)) {
                return true;
            }
        }
        return false;
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
            actionLock = true;
            plugin.getScheduler().runTaskAsync(() -> {
                if (parent != null) {
                    parent.doSearch();
                    parent.open();
                } else {
                    GuiMarketplace
                            .create(player, Searching.of(false).tag(tag))
                            .open();
                }
            });
        }
    }
}
