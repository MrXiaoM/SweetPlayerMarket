package top.mrxiaom.sweet.playermarket.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.jspecify.annotations.NonNull;

import static java.util.Objects.requireNonNull;

public class PlainSerializer implements ComponentSerializer<Component, TextComponent, String> {
    public static final PlainSerializer INSTANCE = new PlainSerializer();
    private final ComponentFlattener flattener = ComponentFlattener.basic();

    @Override
    public @NonNull TextComponent deserialize(@NonNull String input) {
        return Component.text(input);
    }

    @Override
    public @NonNull String serialize(@NonNull Component component) {
        final StringBuilder sb = new StringBuilder();
        this.flattener.flatten(requireNonNull(component, "component"), sb::append);
        return sb.toString();
    }
}
