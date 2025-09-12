package dev.ferdinandkeller.hotbartotems.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HotbarTotemsMixin {
    @Shadow public abstract void setHealth(float health);

    @Inject(method = "tryUseDeathProtector", at = @At("TAIL"), cancellable = true)
    private void tryUseDeathProtector(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return; // if already success, do nothing

        // if not user, can't look for hotbar
        if (!((LivingEntity)(Object) this instanceof ServerPlayerEntity serverPlayerEntity)) return;

        PlayerInventory inv = serverPlayerEntity.getInventory();
        ItemStack itemStack = null;
        DeathProtectionComponent deathProtectionComponent = null;

        // iterate hotbar
        for (int i = 0; i < PlayerInventory.HOTBAR_SIZE; i++) {
            ItemStack itemStack2 = inv.getStack(i);
            deathProtectionComponent = itemStack2.get(DataComponentTypes.DEATH_PROTECTION);

            // if no death protection component, ignore
            if (deathProtectionComponent == null) continue;

            itemStack = itemStack2.copy();
            itemStack2.decrement(1);
            break;
        }

        // didn't find a totem
        if (deathProtectionComponent == null) return;

        // stat stuff
        serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(itemStack.getItem()));
        Criteria.USED_TOTEM.trigger(serverPlayerEntity, itemStack);
        ((Entity)(Object)this).emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);

        // make sure you survive (health, clear effects, regen, ...)
        this.setHealth(1.0F);
        deathProtectionComponent.applyDeathEffects(itemStack, (LivingEntity)(Object)this);
        // plays UI animation
        ((Entity)(Object)this).getWorld().sendEntityStatus((Entity)(Object)this, EntityStatuses.USE_TOTEM_OF_UNDYING);

        cir.setReturnValue(true);
    }
}