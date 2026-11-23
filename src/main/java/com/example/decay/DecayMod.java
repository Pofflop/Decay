package com.example.decay;

import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(DecayMod.MOD_ID)
public class DecayMod {
    public static final String MOD_ID = "decay";

    // Half a heart = 1 health point. Edit these two to tune it.
    private static final int INTERVAL_TICKS = 30 * 20;
    private static final double HEARTS_AMOUNT = 1.0;

    private static final UUID MODIFIER_ID = UUID.fromString("5d1c3a52-7a43-4f0e-9b1a-3e6f0c2d8a11");

    public DecayMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /** Every 30 seconds, every player loses half a heart of max health. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        Player player = event.player;
        if (!(player instanceof ServerPlayer serverPlayer) || !player.isAlive()) return;

        if (player.tickCount > 0 && player.tickCount % INTERVAL_TICKS == 0) {
            changeHearts(serverPlayer, -HEARTS_AMOUNT);
        }
    }

    /** Killing a mob steals half a heart from it. */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof Mob)) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            changeHearts(killer, HEARTS_AMOUNT);
        }
    }

    private static void changeHearts(ServerPlayer player, double delta) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        AttributeModifier old = maxHealth.getModifier(MODIFIER_ID);
        double total = (old == null ? 0.0 : old.getAmount()) + delta;

        // Vanilla can't go below half a heart; losing more than that is death.
        if (maxHealth.getBaseValue() + total < 1.0) {
            player.kill();
            return;
        }

        if (old != null) maxHealth.removeModifier(MODIFIER_ID);
        maxHealth.addPermanentModifier(
                new AttributeModifier(MODIFIER_ID, "Decay hearts", total, AttributeModifier.Operation.ADDITION));

        if (delta > 0) {
            player.heal((float) delta);
        } else if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
