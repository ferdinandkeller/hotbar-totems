package dev.ferdinandkeller.hotbartotems.mixin;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {
    @Accessor("HOTBAR_SIZE")
    public static int getHotbarSize() {
        throw new AssertionError();
    }
}
