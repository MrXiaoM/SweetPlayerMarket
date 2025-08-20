package top.mrxiaom.sweet.playermarket.commands.arguments;

import top.mrxiaom.pluginbase.utils.arguments.Arguments;
import top.mrxiaom.pluginbase.utils.arguments.CommandArguments;

public class OpenArguments extends CommandArguments {
    private static final Arguments.Builder builder = Arguments.builder();
    protected OpenArguments(Arguments arguments) {
        super(arguments);
    }

    public static OpenArguments of(String[] args) {
        return builder.build(OpenArguments::new, args);
    }
}
