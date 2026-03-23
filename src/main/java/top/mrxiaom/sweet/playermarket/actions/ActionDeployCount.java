package top.mrxiaom.sweet.playermarket.actions;

import org.bukkit.configuration.ConfigurationSection;
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

public class ActionDeployCount implements IAction {
    public static final IActionProvider PROVIDER = (input) -> {
        if (input instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) input;
            String type = section.getString("type");
            String operation = section.getString("operation");
            if ("create-amount".equals(type) && operation != null) {
                if (operation.equals("input")) {
                    return new Input(
                            Messages.Gui.deploy__amount__prompt_message,
                            Messages.Gui.deploy__amount__prompt_cancel,
                            Messages.Gui.deploy__amount__success,
                            Messages.Gui.deploy__amount__not_number,
                            (gui, value) -> gui.modifyAmount(IGuiDeploy.NumberOperation.SET, value));
                }
                return parse(operation, (operation1, value) -> (gui) -> gui.modifyAmount(operation1, value));
            }
            if ("create-item-count".equals(type) && operation != null) {
                if (operation.equals("input")) {
                    return new Input(
                            Messages.Gui.deploy__item_count__prompt_message,
                            Messages.Gui.deploy__item_count__prompt_cancel,
                            Messages.Gui.deploy__item_count__success,
                            Messages.Gui.deploy__item_count__not_number,
                            (gui, value) -> gui.modifyItemCount(IGuiDeploy.NumberOperation.SET, value));
                }
                return parse(operation, (operation1, value) -> (gui) -> gui.modifyItemCount(operation1, value));
            }
        } else {
            String s = String.valueOf(input);
            if (s.startsWith("[count:amount]")) {
                String params = s.substring(14);
                if (params.equals("input")) {
                    return new Input(
                            Messages.Gui.deploy__amount__prompt_message,
                            Messages.Gui.deploy__amount__prompt_cancel,
                            Messages.Gui.deploy__amount__success,
                            Messages.Gui.deploy__amount__not_number,
                            (gui, value) -> gui.modifyAmount(IGuiDeploy.NumberOperation.SET, value));
                }
                return parse(params, (operation, value) -> (gui) -> gui.modifyAmount(operation, value));
            }
            if (s.startsWith("[count:item]")) {
                String params = s.substring(12);
                if (params.equals("input")) {
                    return new Input(
                            Messages.Gui.deploy__item_count__prompt_message,
                            Messages.Gui.deploy__item_count__prompt_cancel,
                            Messages.Gui.deploy__item_count__success,
                            Messages.Gui.deploy__item_count__not_number,
                            (gui, value) -> gui.modifyItemCount(IGuiDeploy.NumberOperation.SET, value));
                }
                return parse(params, (operation, value) -> (gui) -> gui.modifyItemCount(operation, value));
            }
        }
        return null;
    };

    private static IAction parse(String params, BiFunction<IGuiDeploy.NumberOperation, Integer, Consumer<IGuiDeploy>> func) {
        if (params.startsWith("-")) {
            String s = params.substring(1);
            return Util.parseInt(s)
                    .map(i -> new ActionDeployCount(func.apply(IGuiDeploy.NumberOperation.MINUS, i)))
                    .orElse(null);
        }
        if (params.startsWith("+")) {
            String s = params.substring(1);
            return Util.parseInt(s)
                    .map(i -> new ActionDeployCount(func.apply(IGuiDeploy.NumberOperation.ADD, i)))
                    .orElse(null);
        }
        Integer i = Util.parseInt(params).orElse(null);
        if (i != null) {
            return new ActionDeployCount(func.apply(IGuiDeploy.NumberOperation.SET, i));
        }
        return null;
    }
    private final Consumer<IGuiDeploy> consumer;
    public ActionDeployCount(Consumer<IGuiDeploy> consumer) {
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

    public static class Input implements IAction {
        private final Message messagePrompt, messageCancel, messageSuccess, messageNotNumber;
        private final BiConsumer<IGuiDeploy, Integer> impl;
        public Input(Message messagePrompt, Message messageCancel, Message messageSuccess, Message messageNotNumber, BiConsumer<IGuiDeploy, Integer> impl) {
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
                        int v = Util.parseInt(str).orElse(0);
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
