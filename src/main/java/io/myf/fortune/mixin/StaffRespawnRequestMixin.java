package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.StaffResurrection;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class StaffRespawnRequestMixin {
    @Inject(method = {"handleClientCommand", "m_6272_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void fortuneSpells$awaitSeal(ServerboundClientCommandPacket packet, CallbackInfo callback) {
        ServerPlayer player = ((ServerGamePacketListenerImpl)(Object)this).player;
        if (player.server.isSameThread() && packet.getAction() == ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
                && !player.wonGame && StaffResurrection.awaitingRespawn(player)) callback.cancel();
    }
}
