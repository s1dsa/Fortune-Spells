package io.myf.fortune.client;

import io.myf.fortune.client.cardgame.ClientCardGame;
import io.myf.fortune.client.fx.FortuneFx;
import io.myf.fortune.network.S2CCardGameStart;
import io.myf.fortune.network.S2CCardGamePhase;
import io.myf.fortune.network.S2CCardGameResult;
import io.myf.fortune.network.S2CFortuneFx;

/** Client entry points for the card HUD, reveal animations and combat effects. */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void onCardGameStart(S2CCardGameStart msg) {
        ClientCardGame.onStart(msg);
    }

    public static void onCardGamePhase(S2CCardGamePhase msg) {
        ClientCardGame.onPhase(msg);
    }

    public static void onCardGameResult(S2CCardGameResult msg) {
        ClientCardGame.onResult(msg);
    }

    public static void onFortuneFx(S2CFortuneFx msg) {
        FortuneFx.onFx(msg);
    }
}
