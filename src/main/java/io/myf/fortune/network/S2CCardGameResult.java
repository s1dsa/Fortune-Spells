package io.myf.fortune.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CCardGameResult(int sessionId, int pickedCardId, int correctCardId,
                                boolean won, boolean timedOut, byte effect, int effectTicks) {
    void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(pickedCardId);
        buf.writeVarInt(correctCardId);
        buf.writeBoolean(won);
        buf.writeBoolean(timedOut);
        buf.writeByte(effect);
        buf.writeVarInt(effectTicks);
    }

    static S2CCardGameResult decode(FriendlyByteBuf buf) {
        return new S2CCardGameResult(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readByte(), buf.readVarInt());
    }

    static void handle(S2CCardGameResult msg, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> io.myf.fortune.client.ClientPacketHandler.onCardGameResult(msg));
    }
}
