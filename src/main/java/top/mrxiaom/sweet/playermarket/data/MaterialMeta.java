package top.mrxiaom.sweet.playermarket.data;

import org.bukkit.Material;

@SuppressWarnings({"deprecation", "Convert2MethodRef"})
public enum MaterialMeta {
    IS_BLOCK(it -> it.isBlock()),
    IS_AIR(it -> it.isAir()),
    IS_BURNABLE(it -> it.isBurnable()),
    IS_EDITABLE(it -> it.isEdible()),
    IS_FLAMMABLE(it -> it.isFlammable()),
    IS_FUEL(it -> it.isFuel()),
    IS_INTERACTABLE(it -> it.isInteractable()),
    IS_ITEM(it -> it.isItem()),
    IS_OCCLUDING(it -> it.isOccluding()),
    IS_RECORD(it -> it.isRecord()),
    IS_SOLID(it -> it.isSolid()),
    IS_TRANSPARENT(it -> it.isTransparent()),

    ;
    private interface Impl {
        boolean check(Material material) throws Throwable;
    }
    private final Impl impl;
    MaterialMeta(Impl impl) {
        this.impl = impl;
    }

    public boolean check(Material material) {
        try {
            return impl.check(material);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
