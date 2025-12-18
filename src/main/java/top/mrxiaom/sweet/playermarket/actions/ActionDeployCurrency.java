package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.economy.IEconomy;
import top.mrxiaom.sweet.playermarket.gui.api.IGuiDeploy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ActionDeployCurrency implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        SweetPlayerMarket plugin = SweetPlayerMarket.getInstance();
        if (s.startsWith("[currency]switch::")) {
            String[] params = s.substring(18).split(",");
            List<IEconomy> currencyList = new ArrayList<>();
            for (String param : params) {
                IEconomy currency = plugin.parseEconomy(param);
                if (currency != null) {
                    currencyList.add(currency);
                }
            }
            if (!currencyList.isEmpty()) {
                return new ActionDeployCount(gui -> gui.switchCurrency(currencyList));
            }
        }
        if (s.startsWith("[currency]set::")) {
            String param = s.substring(15);
            IEconomy currency = plugin.parseEconomy(param);
            if (currency != null) {
                return new ActionDeployCount(gui -> gui.setCurrency(currency));
            }
        }
        return null;
    };

    private final Consumer<IGuiDeploy> consumer;
    public ActionDeployCurrency(Consumer<IGuiDeploy> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof IGuiDeploy) {
                consumer.accept((IGuiDeploy) gui);
            }
        }
    }
}
