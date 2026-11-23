package io.myf.fortune.network;

import io.myf.fortune.cardgame.CardGameManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SCardPick(int sessionId, int cardEntityId) {
    void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(cardEntityId);
    }

    static C2SCardPick decode(FriendlyByteBuf buf) {
        return new C2SCardPick(buf.readVarInt(), buf.readVarInt());
    }

    static void handle(C2SCardPick msg, Supplier<NetworkEvent.Context> context) {
        CardGameManager.onPick(context.get().getSender(), msg.sessionId(), msg.cardEntityId());
    }
}
