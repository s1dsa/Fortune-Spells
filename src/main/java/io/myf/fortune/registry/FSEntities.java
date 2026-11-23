package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.entity.FateCardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FSEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FortuneIds.MODID);
    public static final RegistryObject<EntityType<FateCardEntity>> FATE_CARD = REGISTER.register(FortuneIds.ENTITY_FATE_CARD.getPath(),
            () -> EntityType.Builder.<FateCardEntity>of(FateCardEntity::new, MobCategory.MISC)
                    .sized(1.25f, 1.75f).clientTrackingRange(8).updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true).noSave().build(FortuneIds.ENTITY_FATE_CARD.toString()));

    private FSEntities() {}
}
