package dev.ferdinandkeller.hotbartotems.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HotbarTotemsMixin extends Entity {
    public HotbarTotemsMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow public abstract void setHealth(float health);

    @Inject(method = "checkTotemDeathProtection", at = @At("TAIL"), cancellable = true)
    private void hotbartotems$checkHotbarTotemDeathProtection(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // vanilla already consumed a totem from a hand
        if (cir.getReturnValueZ()) return;

        // only players have a hotbar, and only the server should consume items
        if (!((Object) this instanceof ServerPlayer player)) return;

        Inventory inventory = player.getInventory();
        ItemStack consumed = null;
        DeathProtection protection = null;

        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            DeathProtection component = stack.get(DataComponents.DEATH_PROTECTION);
            if (component == null) continue;

            protection = component;
            consumed = stack.copy();
            stack.shrink(1);
            break;
        }

        if (consumed == null) return;

        LivingEntity self = (LivingEntity) (Object) this;

        player.awardStat(Stats.ITEM_USED.get(consumed.getItem()));
        CriteriaTriggers.USED_TOTEM.trigger(player, consumed);
        consumed.causeUseVibration(self, GameEvent.ITEM_INTERACT_FINISH);

        this.setHealth(1.0F);
        protection.applyEffects(consumed, self);
        this.level().broadcastEntityEvent(this, (byte) 35);

        cir.setReturnValue(true);
    }
}