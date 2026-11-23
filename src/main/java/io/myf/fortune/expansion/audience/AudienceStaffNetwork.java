package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

public final class AudienceStaffNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            FortuneIds.id("staff_naming"), () -> VERSION, VERSION::equals, VERSION::equals);

    private AudienceStaffNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Inscription.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(Inscription::encode).decoder(Inscription::decode)
                .consumerMainThread(Inscription::handle).add();
    }

    public static void submit(int slot, UUID id, String name) {
        CHANNEL.sendToServer(new Inscription(slot, id, name));
    }

    private record Inscription(int slot, UUID id, String name) {
        private void encode(FriendlyByteBuf buf) {
            buf.writeVarInt(slot);
            buf.writeUUID(id);
            buf.writeUtf(name, AudienceStaffItem.MAX_NAME_LENGTH * 2);
        }

        private static Inscription decode(FriendlyByteBuf buf) {
            return new Inscription(buf.readVarInt(), buf.readUUID(), buf.readUtf(AudienceStaffItem.MAX_NAME_LENGTH * 2));
        }

        private void handle(Supplier<NetworkEvent.Context> context) {
            ServerPlayer sender = context.get().getSender();
            if (sender != null) AudienceStaffItem.inscribe(sender, slot, id, name);
        }
    }
}
