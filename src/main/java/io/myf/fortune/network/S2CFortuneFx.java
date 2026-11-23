package io.myf.fortune.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CFortuneFx(byte kind, int entityId, float amount) {
    void encode(FriendlyByteBuf buf) {
        buf.writeByte(kind);
        buf.writeVarInt(entityId);
        buf.writeFloat(amount);
    }

    static S2CFortuneFx decode(FriendlyByteBuf buf) {
        return new S2CFortuneFx(buf.readByte(), buf.readVarInt(), buf.readFloat());
    }

    static void handle(S2CFortuneFx msg, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> io.myf.fortune.client.ClientPacketHandler.onFortuneFx(msg));
    }
}
