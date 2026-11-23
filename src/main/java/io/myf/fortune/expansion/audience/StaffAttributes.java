package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class StaffAttributes {
    private static final AttributeModifier MANA = new AttributeModifier(UUID.fromString("b9814d49-7071-44b8-9434-e391bc3aa653"),
            "Audience staff mana", 1000, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier POWER = new AttributeModifier(UUID.fromString("b9f58065-a793-4239-968e-913a463a20e7"),
            "Audience staff power", 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier COOLDOWN = new AttributeModifier(UUID.fromString("c41cf121-fb20-4db0-aedb-e169de558ec2"),
            "Audience staff cooldown reduction", 1, AttributeModifier.Operation.ADDITION);

    private StaffAttributes() {}

    /** In Doom the external mod returns base values. Restore only the equipped staff's contribution. */
    public static double getDoomValue(AttributeInstance attribute, double value) {
        if (attribute.getModifier(MANA.getId()) != null) return attribute.getBaseValue() + MANA.getAmount();
        if (attribute.getModifier(POWER.getId()) != null) return attribute.getBaseValue() * (1 + POWER.getAmount());
        if (attribute.getModifier(COOLDOWN.getId()) != null) return attribute.getBaseValue() + COOLDOWN.getAmount();
        return value;
    }

    public static boolean heldBy(LivingEntity player) {
        return player.getMainHandItem().getItem() instanceof AudienceStaffItem
                || player.getOffhandItem().getItem() instanceof AudienceStaffItem;
    }

    @SubscribeEvent public static void changed(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof ServerPlayer player) refresh(player);
    }

    private static void refresh(ServerPlayer player) {
        boolean held = heldBy(player);
        AttributeInstance mana = player.getAttribute(AttributeRegistry.MAX_MANA.get());
        if (mana == null) return;
        boolean removing = !held && mana.getModifier(MANA.getId()) != null;
        reconcile(mana, MANA, held);
        reconcile(player.getAttribute(AttributeRegistry.SPELL_POWER.get()), POWER, held);
        reconcile(player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION.get()), COOLDOWN, held);
        if (!held && !removing) return;
        var data = MagicData.getPlayerMagicData(player);
        if (removing && data.getMana() > mana.getValue()) {
            data.setMana((float) mana.getValue());
            if (player.connection != null) PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
        }
        var cooldowns = data.getPlayerCooldowns();
        if (held && cooldowns.hasCooldownsActive()) {
            cooldowns.clearCooldowns();
            if (player.connection != null) cooldowns.syncToPlayer(player);
        }
    }

    private static void reconcile(AttributeInstance attribute, AttributeModifier modifier, boolean held) {
        if (attribute == null || (attribute.getModifier(modifier.getId()) != null) == held) return;
        if (held) attribute.addTransientModifier(modifier);
        else attribute.removeModifier(modifier.getId());
    }

    @SubscribeEvent public static void cooldown(SpellCooldownAddedEvent.Pre event) {
        if (heldBy(event.getEntity())) event.setCanceled(true);
    }
}
