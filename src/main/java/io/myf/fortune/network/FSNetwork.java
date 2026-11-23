package io.myf.fortune.network;

import io.myf.fortune.FortuneIds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FSNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            FortuneIds.CHANNEL_MAIN, () -> FortuneIds.PROTOCOL_VERSION,
            FortuneIds.PROTOCOL_VERSION::equals, FortuneIds.PROTOCOL_VERSION::equals);

    private FSNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(S2CCardGameStart.class, FortuneIds.PKT_S2C_CARD_GAME_START, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CCardGameStart::encode).decoder(S2CCardGameStart::decode)
                .consumerMainThread(S2CCardGameStart::handle).add();
        CHANNEL.messageBuilder(S2CCardGamePhase.class, FortuneIds.PKT_S2C_CARD_GAME_PHASE, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CCardGamePhase::encode).decoder(S2CCardGamePhase::decode)
                .consumerMainThread(S2CCardGamePhase::handle).add();
        CHANNEL.messageBuilder(S2CCardGameResult.class, FortuneIds.PKT_S2C_CARD_GAME_RESULT, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CCardGameResult::encode).decoder(S2CCardGameResult::decode)
                .consumerMainThread(S2CCardGameResult::handle).add();
        CHANNEL.messageBuilder(C2SCardPick.class, FortuneIds.PKT_C2S_CARD_PICK, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SCardPick::encode).decoder(C2SCardPick::decode)
                .consumerMainThread(C2SCardPick::handle).add();
        CHANNEL.messageBuilder(S2CFortuneFx.class, FortuneIds.PKT_S2C_FORTUNE_FX, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CFortuneFx::encode).decoder(S2CFortuneFx::decode)
                .consumerMainThread(S2CFortuneFx::handle).add();
    }

    public static void sendTo(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendTracking(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToServer(C2SCardPick pick) {
        CHANNEL.sendToServer(pick);
    }
}
