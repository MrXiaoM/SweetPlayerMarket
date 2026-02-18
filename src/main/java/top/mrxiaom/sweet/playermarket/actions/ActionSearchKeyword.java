package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.api.IScheduler;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;
import top.mrxiaom.sweet.playermarket.utils.Prompter;

import java.util.List;

public class ActionSearchKeyword implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[search:keyword]")) {
            String keyword = s.substring(16);
            return new ActionSearchKeyword(keyword);
        }
        if (s.startsWith("search:keyword:")) {
            String keyword = s.substring(15);
            return new ActionSearchKeyword(keyword);
        }
        if (s.equals("search:keyword")) {
            return new ActionSearchKeyword("");
        }
        return null;
    };
    private final String keyword;
    public ActionSearchKeyword(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof AbstractGuiSearch.SearchGui) {
                AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
                IScheduler scheduler = gm.plugin.getScheduler();
                if (keyword.isEmpty()) {
                    player.closeInventory();
                    String cancel = Messages.Gui.keyword__prompt_cancel.str();
                    Messages.Gui.keyword__prompt_message.tm(player, Pair.of("%cancel%", cancel));
                    Prompter.chat(player, cancel, (input) -> {
                        gm.searching().keyword(input);
                        gm.resetPage();
                        gm.doSearch();
                        scheduler.runTask(gm::open);
                    }, () -> scheduler.runTask(gm::open));
                } else {
                    gm.searching().keyword(keyword.equals("reset") ? null : keyword);
                    gm.resetPage();
                    gm.refreshGui();
                }
            }
        }
    }
}
