package dev.ferdinandkeller.hotbartotems.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HotbarTotemsMixin extends Entity {
    public HotbarTotemsMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow public abstract void setHealth(float health);

    @Inject(method = "tryUseDeathProtector", at = @At("TAIL"), cancellable = true)
    private void hotbartotems$tryHotbarDeathProtector(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // vanilla already consumed a totem from a hand
        if (cir.getReturnValueZ()) return;

        // only players have a hotbar, and only the server should consume items
        if (!((Object) this instanceof ServerPlayerEntity player)) return;

        PlayerInventory inventory = player.getInventory();
        ItemStack consumed = null;
        DeathProtectionComponent protection = null;

        for (int slot = 0; slot < PlayerInventory.getHotbarSize(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            DeathProtectionComponent component = stack.get(DataComponentTypes.DEATH_PROTECTION);
            if (component == null) continue;

            protection = component;
            consumed = stack.copy();
            stack.decrement(1);
            break;
        }

        if (consumed == null) return;

        player.incrementStat(Stats.USED.getOrCreateStat(consumed.getItem()));
        Criteria.USED_TOTEM.trigger(player, consumed);
        this.emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);

        this.setHealth(1.0F);
        protection.applyDeathEffects(consumed, (LivingEntity) (Object) this);
        this.getEntityWorld().sendEntityStatus(this, EntityStatuses.USE_TOTEM_OF_UNDYING);

        cir.setReturnValue(true);
    }
}