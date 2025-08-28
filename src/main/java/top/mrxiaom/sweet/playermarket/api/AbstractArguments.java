package top.mrxiaom.sweet.playermarket.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.utils.arguments.Arguments;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.Messages;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;

import java.util.function.Function;

public abstract class AbstractArguments<Sender extends CommandSender> extends CommandArguments {
    protected AbstractArguments(CommandArguments args) {
        super(args.arguments());
        pointer(args.pointer());
    }

    protected AbstractArguments(Arguments arguments) {
        super(arguments);
    }

    public abstract boolean execute(SweetPlayerMarket plugin, Sender sender);

    public <T> T into(Function<CommandArguments, T> transformer) {
        return transformer.apply(this);
    }

    protected Player getPlayerOrSelf(CommandSender sender, String perm) {
        return nextPlayerOrSelf(sender,
                () -> Messages.player__only.tm(sender), perm,
                () -> Messages.Command.no_permission.tm(sender),
                () -> Messages.player__not_online.tm(sender));
    }
}
