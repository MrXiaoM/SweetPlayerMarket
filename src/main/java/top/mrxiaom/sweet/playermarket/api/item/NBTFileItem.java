package top.mrxiaom.sweet.playermarket.api.item;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.playermarket.SweetPlayerMarket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class NBTFileItem implements ItemProvider {
    public static final NBTFileItem INSTANCE = new NBTFileItem();
    private NBTFileItem() {}

    @Override
    public int priority() {
        return 990;
    }

    @Override
    public @Nullable ItemStack get(String inputText) {
        if (inputText.startsWith("file:")) {
            File file = SweetPlayerMarket.getInstance().resolve(inputText.substring(5));
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ReadWriteNBT nbt = NBT.readNBT(fis);
                    return NBT.itemStackFromNBT(nbt);
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
