package io.myf.fortune.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CCardGamePhase(int sessionId, byte phase, int durationTicks, byte reason) {
    void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeByte(phase);
        buf.writeVarInt(durationTicks);
        buf.writeByte(reason);
    }

    static S2CCardGamePhase decode(FriendlyByteBuf buf) {
        return new S2CCardGamePhase(buf.readVarInt(), buf.readByte(), buf.readVarInt(), buf.readByte());
    }

    static void handle(S2CCardGamePhase msg, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> io.myf.fortune.client.ClientPacketHandler.onCardGamePhase(msg));
    }
}
