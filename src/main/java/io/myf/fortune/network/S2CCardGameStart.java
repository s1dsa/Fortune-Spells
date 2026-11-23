package io.myf.fortune.network;

import io.myf.fortune.FortuneIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CCardGameStart(int sessionId, byte mode, byte spellLevel, byte cardCount,
                               int[] cardEntityIds, byte targetFace, int pickWindowTicks) {
    public S2CCardGameStart {
        if (cardCount < 1 || cardCount > FortuneIds.Face.COUNT || cardEntityIds.length != cardCount) {
            throw new IllegalArgumentException("Invalid card count");
        }
        cardEntityIds = cardEntityIds.clone();
    }

    @Override
    public int[] cardEntityIds() {
        return cardEntityIds.clone();
    }

    void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeByte(mode);
        buf.writeByte(spellLevel);
        buf.writeByte(cardCount);
        for (int entityId : cardEntityIds) {
            buf.writeVarInt(entityId);
        }
        buf.writeByte(targetFace);
        buf.writeVarInt(pickWindowTicks);
    }

    static S2CCardGameStart decode(FriendlyByteBuf buf) {
        int sessionId = buf.readVarInt();
        byte mode = buf.readByte();
        byte spellLevel = buf.readByte();
        byte cardCount = buf.readByte();
        if (cardCount < 1 || cardCount > FortuneIds.Face.COUNT) {
            throw new IllegalArgumentException("Invalid card count");
        }
        int[] ids = new int[cardCount];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = buf.readVarInt();
        }
        return new S2CCardGameStart(sessionId, mode, spellLevel, cardCount, ids, buf.readByte(), buf.readVarInt());
    }

    static void handle(S2CCardGameStart msg, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> io.myf.fortune.client.ClientPacketHandler.onCardGameStart(msg));
    }
}
