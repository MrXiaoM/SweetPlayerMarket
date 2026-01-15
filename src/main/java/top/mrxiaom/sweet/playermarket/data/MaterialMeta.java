package top.mrxiaom.sweet.playermarket.data;

import org.bukkit.Material;

@SuppressWarnings({"deprecation"})
public enum MaterialMeta {
    IS_BLOCK(Material::isBlock),
    IS_AIR(Material::isAir),
    IS_BURNABLE(Material::isBurnable),
    IS_EDITABLE(Material::isEdible),
    IS_FLAMMABLE(Material::isFlammable),
    IS_FUEL(Material::isFuel),
    IS_INTERACTABLE(Material::isInteractable),
    IS_ITEM(Material::isItem),
    IS_OCCLUDING(Material::isOccluding),
    IS_RECORD(Material::isRecord),
    IS_SOLID(Material::isSolid),
    IS_TRANSPARENT(Material::isTransparent),

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
