package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.data.NoticeFlag;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

import java.util.List;

public class ActionSearchNotice implements IAction {
    public static final ActionSearchNotice INSTANCE = new ActionSearchNotice();
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.equals("[search:notice]") || s.equals("search:notice")) return INSTANCE;
        return null;
    };
    private ActionSearchNotice() {}
    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof AbstractGuiSearch.SearchGui) {
                AbstractGuiSearch.SearchGui gm = (AbstractGuiSearch.SearchGui) gui;
                Integer old = gm.searching().notice();
                if (old == null) {
                    gm.searching().noticeFlag(NoticeFlag.CAN_CLAIM_ITEMS);
                } else {
                    gm.searching().noticeFlag(null);
                }
                gm.refreshGui();
            }
        }
    }
}
