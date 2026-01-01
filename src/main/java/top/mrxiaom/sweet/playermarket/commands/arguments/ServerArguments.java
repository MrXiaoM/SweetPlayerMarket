package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.data.Searching;
import top.mrxiaom.sweet.playermarket.gui.GuiMyItems;

public class ServerArguments extends AbstractArguments<CommandSender> {
    protected ServerArguments(CommandArguments arguments) {
        super(arguments);
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        Player player = getPlayerOrSelf(sender, "sweet.playermarket.server.other");
        if (player == null) {
            return true;
        }
        GuiMyItems.create(player, Searching.of(false)
                .playerId("#server#")
        ).open();
        return true;
    }

    public static ServerArguments of(CommandArguments args) {
        return new ServerArguments(args);
    }
}
