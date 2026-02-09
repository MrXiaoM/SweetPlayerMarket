package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.api.IScheduler;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.func.language.Message;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.gui.api.IGuiDeploy;
import top.mrxiaom.sweet.playermarket.utils.Prompter;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ActionDeployPrice implements IAction {
    public static final IActionProvider PROVIDER = (s) -> {
        if (s.startsWith("[price]")) {
            String params = s.substring(7);
            if (params.equals("input")) {
                return new Input(
                        Messages.Gui.deploy__price__prompt_message,
                        Messages.Gui.deploy__price__prompt_cancel,
                        Messages.Gui.deploy__price__success,
                        Messages.Gui.deploy__price__not_number,
                        (gui, value) -> gui.modifyPrice(IGuiDeploy.NumberOperation.SET, value));
            }
            return parse(params, (operation, value) -> (gui) -> gui.modifyPrice(operation, value));
        }
        return null;
    };

    private static IAction parse(String params, BiFunction<IGuiDeploy.NumberOperation, Double, Consumer<IGuiDeploy>> func) {
        if (params.startsWith("-")) {
            String s = params.substring(1);
            return Util.parseDouble(s)
                    .map(i -> new ActionDeployPrice(func.apply(IGuiDeploy.NumberOperation.MINUS, i)))
                    .orElse(null);
        }
        if (params.startsWith("+")) {
            String s = params.substring(1);
            return Util.parseDouble(s)
                    .map(i -> new ActionDeployPrice(func.apply(IGuiDeploy.NumberOperation.ADD, i)))
                    .orElse(null);
        }
        Double i = Util.parseDouble(params).orElse(null);
        if (i != null) {
            return new ActionDeployPrice(func.apply(IGuiDeploy.NumberOperation.SET, i));
        }
        return null;
    }
    private final Consumer<IGuiDeploy> impl;
    public ActionDeployPrice(Consumer<IGuiDeploy> impl) {
        this.impl = impl;
    }

    @Override
    public void run(Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
            if (gui instanceof IGuiDeploy) {
                impl.accept((IGuiDeploy) gui);
            }
        }
    }

    public static class Input implements IAction {
        private final Message messagePrompt, messageCancel, messageSuccess, messageNotNumber;
        private final BiConsumer<IGuiDeploy, Double> impl;
        public Input(Message messagePrompt, Message messageCancel, Message messageSuccess, Message messageNotNumber, BiConsumer<IGuiDeploy, Double> impl) {
            this.messagePrompt = messagePrompt;
            this.messageCancel = messageCancel;
            this.messageSuccess = messageSuccess;
            this.messageNotNumber = messageNotNumber;
            this.impl = impl;
        }

        @Override
        public void run(@Nullable Player player, @Nullable List<Pair<String, Object>> list) {
            if (player != null) {
                GuiManager manager = GuiManager.inst();
                IScheduler scheduler = manager.plugin.getScheduler();
                IGuiHolder gui = manager.getOpeningGui(player);
                if (gui instanceof IGuiDeploy) {
                    IGuiDeploy deploy = (IGuiDeploy) gui;
                    player.closeInventory();
                    String cancel = messageCancel.str();
                    messagePrompt.tm(player, Pair.of("%cancel%", cancel));
                    Prompter.chat(player, cancel, str -> {
                        double v = Util.parseDouble(str).orElse(0.0);
                        if (v > 0) {
                            messageSuccess.tm(player, Pair.of("%money%", v));
                            impl.accept(deploy, v);
                        } else {
                            messageNotNumber.tm(player);
                        }
                        scheduler.runTask(gui::open);
                    }, () -> scheduler.runTask(gui::open));
                }
            }
        }
    }
}
