package top.mrxiaom.sweet.playermarket.api;

import top.mrxiaom.pluginbase.api.WithPriority;

public interface HookHandler<T> extends WithPriority {
    void invoke(T hook);
}
