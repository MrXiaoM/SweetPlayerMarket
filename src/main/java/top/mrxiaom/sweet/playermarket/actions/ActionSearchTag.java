package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.util.List;

public class ActionSearchTag implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[search:tag]")) {
            String type = s.substring(12);
            return new ActionSearchTag(type);
        }
        if (s.startsWith("search:tag:")) {
            String type = s.substring(11);
            return new ActionSearchTag(type);
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
                gm.searching().tag(tag.isEmpty() ? null : tag);
                gm.refreshGui();
            }
        }
    }
}
