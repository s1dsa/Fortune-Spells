package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.BossSpell;
import io.myf.fortune.expansion.audience.AudienceSpell;
import io.myf.fortune.expansion.audience.CataclysmSpell;
import io.myf.fortune.spell.CardSelectionSpell;
import io.myf.fortune.spell.BlindGambleSpell;
import io.myf.fortune.spell.AllInSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class FSSpells {
    public static final DeferredRegister<AbstractSpell> REGISTER = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, FortuneIds.MODID);
    public static final RegistryObject<AbstractSpell> CARD_SELECTION = REGISTER.register(
            FortuneIds.SPELL_CARD_SELECTION.getPath(), CardSelectionSpell::new);
    public static final RegistryObject<AbstractSpell> BLIND_GAMBLE = REGISTER.register(
            FortuneIds.SPELL_BLIND_GAMBLE.getPath(), BlindGambleSpell::new);
    public static final RegistryObject<AbstractSpell> ALL_IN = REGISTER.register(
            FortuneIds.SPELL_ALL_IN.getPath(), AllInSpell::new);
    public static final RegistryObject<AbstractSpell> CATACLYSM = REGISTER.register("cataclysm", CataclysmSpell::new);

    static {
        REGISTER.register("audience", AudienceSpell::new);
        for (BossSpell.Kind kind : BossSpell.Kind.values()) {
            REGISTER.register(kind.path, () -> new BossSpell(kind));
        }
    }

    private FSSpells() {}
}
