package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.gui.GuiTagList;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.util.List;

public class ActionSearchTag implements IAction {
    public static final IActionProvider PROVIDER = (input) -> {
        if (input instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) input;
            if ("search-tag".equals(section.getString("type"))) {
                String tag = section.getString("tag");
                if (tag != null) {
                    return new ActionSearchTag(tag);
                }
            }
        } else {
            String s = String.valueOf(input);
            if (s.startsWith("[search:tag]")) {
                String type = s.substring(12);
                return new ActionSearchTag(type);
            }
            if (s.startsWith("search:tag:")) {
                String type = s.substring(11);
                return new ActionSearchTag(type);
            }
        }
        return null;
    };
    private final String tag;
    public ActionSearchTag(String tag) {
        this.tag = tag;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof AbstractGuiSearch.SearchGui) {
                AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
                if (tag.equals("::list::")) {
                    GuiTagList.create(player, gm).open();
                } else {
                    gm.searching().tag(tag.isEmpty() ? null : tag);
                    gm.resetPage();
                    gm.refreshGui();
                }
            }
            if (gui instanceof GuiTagList.Impl) {
                if (tag.equals("::list::")) {
                    return;
                }
                ((GuiTagList.Impl) gui).setTag(tag.isEmpty() ? null : tag);
            }
        }
    }
}
