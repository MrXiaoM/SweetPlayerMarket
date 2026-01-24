package top.mrxiaom.sweet.playermarket.commands.arguments;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.func.GuiManager;
import top.mrxiaom.pluginbase.gui.IGuiHolder;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;
import top.mrxiaom.sweet.playermarket.api.AbstractArguments;
import top.mrxiaom.sweet.playermarket.gui.GuiTagList;
import top.mrxiaom.sweet.playermarket.gui.api.AbstractGuiSearch;

public class TagsArguments extends AbstractArguments<CommandSender> {
    protected TagsArguments(CommandArguments args) {
        super(args);
    }

    @Override
    public boolean execute(SweetPlayerMarket plugin, CommandSender sender) {
        Player player = getPlayerOrSelf(sender, "sweet.playermarket.tags.other");
        if (player == null) {
            return true;
        }
        IGuiHolder gui = GuiManager.inst().getOpeningGui(player);
        if (gui instanceof AbstractGuiSearch.SearchGui) {
            GuiTagList.create(player, (AbstractGuiSearch.SearchGui) gui).open();
        } else {
            GuiTagList.create(player, null).open();
        }
        return true;
    }

    public static TagsArguments of(CommandArguments args) {
        return new TagsArguments(args);
    }
}
