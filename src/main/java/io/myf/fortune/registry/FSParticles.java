package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FSParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTER = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FortuneIds.MODID);
    public static final RegistryObject<SimpleParticleType> FATE_SPARK = particle(FortuneIds.PARTICLE_FATE_SPARK);
    public static final RegistryObject<SimpleParticleType> CARD_SHARD = particle(FortuneIds.PARTICLE_CARD_SHARD);
    public static final RegistryObject<SimpleParticleType> FORSAKEN_MOTE = particle(FortuneIds.PARTICLE_FORSAKEN_MOTE);

    private FSParticles() {}

    private static RegistryObject<SimpleParticleType> particle(ResourceLocation id) {
        return REGISTER.register(id.getPath(), () -> new SimpleParticleType(false));
    }
}
