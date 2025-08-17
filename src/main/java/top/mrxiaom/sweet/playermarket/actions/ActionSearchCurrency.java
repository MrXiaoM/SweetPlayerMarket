package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGui;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.gui.GuiMarketplace;

import java.util.List;

public class ActionSearchCurrency implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[search:currency]")) {
            String type = s.substring(17);
            return new ActionSearchCurrency(type);
        }
        if (s.startsWith("search:currency:")) {
            String type = s.substring(16);
            return new ActionSearchCurrency(type);
        }
        return null;
    };
    private final String type;
    public ActionSearchCurrency(String type) {
        this.type = type;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGui gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof GuiMarketplace.Impl) {
                GuiMarketplace.Impl gm = (GuiMarketplace.Impl) gui;
                switch (type) {
                    case "next": {
                        String currency = gm.searching().currency();
                        if ("Vault".equals(currency)) {
                            gm.searching().currency("PlayerPoints");
                        } else if ("PlayerPoints".equals(currency)) {
                            gm.searching().currency(null);
                        } else {
                            gm.searching().currency("Vault");
                        }
                        gm.refreshGui();
                        break;
                    }
                    case "Vault":
                    case "PlayerPoints": {
                        gm.searching().currency(type);
                        gm.refreshGui();
                        break;
                    }
                }
                if (type.startsWith("MPoints:")) {
                    gm.searching().currency(type);
                    gm.refreshGui();
                }
            }
        }
    }
}
