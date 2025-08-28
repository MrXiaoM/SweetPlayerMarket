package top.mrxiaom.sweet.playermarket.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.data.MarketItem;

@FunctionalInterface
public interface ItemTagResolver {
    @Nullable String resolve(@NotNull MarketItem item);
}
