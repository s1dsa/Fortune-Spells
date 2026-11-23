package io.myf.fortune;

import io.myf.fortune.network.FSNetwork;
import io.myf.fortune.expansion.audience.AudienceContent;
import io.myf.fortune.expansion.audience.AudienceStaffNetwork;
import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.myf.fortune.expansion.movement.MovementSpells;
import io.myf.fortune.expansion.offense.OffensiveSpells;
import io.myf.fortune.expansion.rose.RoseGuard;
import io.myf.fortune.registry.FSAttributes;
import io.myf.fortune.registry.BossSchools;
import io.myf.fortune.registry.FSEffects;
import io.myf.fortune.registry.FSEntities;
import io.myf.fortune.registry.FSParticles;
import io.myf.fortune.registry.FSSchools;
import io.myf.fortune.registry.FSSounds;
import io.myf.fortune.registry.FSSpells;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FortuneIds.MODID)
public final class FortuneSpells {
    public FortuneSpells() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BossSchools.values(); // enqueue all school and attribute registrations before the registry events
        FSAttributes.REGISTER.register(bus);
        FSSounds.REGISTER.register(bus);
        FSEffects.REGISTER.register(bus);
        FSEntities.REGISTER.register(bus);
        FSParticles.REGISTER.register(bus);
        FSSchools.REGISTER.register(bus);
        FSSpells.REGISTER.register(bus);
        ExpansionCrafting.register(bus);
        MovementSpells.register(bus);
        OffensiveSpells.register(bus);
        RoseGuard.register(bus);
        AudienceContent.register(bus);
        bus.addListener(this::setup);
    }

    private void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(FSNetwork::register);
        event.enqueueWork(AudienceStaffNetwork::register);
    }
}
