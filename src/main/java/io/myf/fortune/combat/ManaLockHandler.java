package io.myf.fortune.combat;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.cardgame.CardGameManager;
import io.myf.fortune.registry.FSEffects;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class ManaLockHandler {
    private ManaLockHandler() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void lockMana(ChangeManaEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasEffect(FSEffects.CURSED_BY_FATE.get())) {
            event.setNewMana(0);
        }
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance().getEffect() == FSEffects.CURSED_BY_FATE.get()) {
            clearMana(player);
        }
    }

    @SubscribeEvent
    public static void copyCardEffects(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original)
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        // Leaving the End can replace the player before the next card-game tick.
        CardGameManager.cancel(original);
        copyEffect(original, player, FSEffects.CURSED_BY_FATE.get());
        if (!event.isWasDeath()) {
            copyEffect(original, player, FSEffects.BLESSED_BY_FORTUNE.get());
        }
    }

    private static void copyEffect(ServerPlayer original, ServerPlayer player, MobEffect type) {
        MobEffectInstance effect = original.getEffect(type);
        if (effect != null) player.addEffect(new MobEffectInstance(effect));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void syncAfterRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Clone runs before the vanilla respawn packet replaces the client-side player.
            if (event.isEndConquered()) {
                MobEffectInstance blessing = player.getEffect(FSEffects.BLESSED_BY_FORTUNE.get());
                if (blessing != null) {
                    player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), blessing));
                }
            }
            MobEffectInstance curse = player.getEffect(FSEffects.CURSED_BY_FATE.get());
            if (curse != null) {
                player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), curse));
                clearMana(player);
            }
        }
    }

    private static void clearMana(ServerPlayer player) {
        MagicData data = MagicData.getPlayerMagicData(player);
        data.setMana(0);
        if (player.connection != null) {
            PacketDistributor.sendToPlayer(player, new SyncManaPacket(data));
        }
    }
}
