package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class AudienceRitual extends Entity {
    public static final int SLOTS = 13, ACTIVATION_TICKS = 60, FINALE_TICKS = 40;
    private static final int CENTER = 12;
    private static final String ACTIVE = "fortune_spells_audience_ritual";
    private static final String PREVIEW = "fortune_spells_audience_drawing";
    private static final String SPELL_ID = FortuneIds.id("audience").toString();
    public static final int DRAWING = -1;
    private static final Set<ResourceLocation> RELICS = Set.of(relic("phantoplasm"), relic("mossy_tooth"),
            relic("violet_bloom"), relic("fortunes_favor"));
    private static final Vec3[] OFFSETS = offsets();
    // Entity ticking at ticket distance two; time out if the entity vanishes between saves.
    private static final TicketType<UUID> TICKET = TicketType.create("fortune_spells:audience", UUID::compareTo, 60);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(AudienceRitual.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(AudienceRitual.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> STAGE_START = SynchedEntityData.defineId(AudienceRitual.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(AudienceRitual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DRAW_TICKS = SynchedEntityData.defineId(AudienceRitual.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> HEIGHTS = SynchedEntityData.defineId(AudienceRitual.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DEPOSIT_TIMES = SynchedEntityData.defineId(AudienceRitual.class, EntityDataSerializers.COMPOUND_TAG);
    private static final List<EntityDataAccessor<ItemStack>> OFFERINGS = offeringKeys();
    private long nextPayment;
    private boolean refunding;

    public AudienceRitual(EntityType<? extends AudienceRitual> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    private static ResourceLocation relic(String path) {
        return ResourceLocation.fromNamespaceAndPath("meetyourfight", path);
    }

    private static Vec3[] offsets() {
        Vec3[] points = new Vec3[SLOTS];
        for (int i = 0; i < CENTER; i++) {
            double angle = i < 8 ? i * Math.PI / 4 : (i - 8) * Math.PI / 2 + Math.PI / 4;
            double radius = i < 8 ? 24 : 11;
            points[i] = new Vec3(radius * Math.cos(angle), 0, radius * Math.sin(angle));
        }
        points[CENTER] = Vec3.ZERO;
        return points;
    }

    private static List<EntityDataAccessor<ItemStack>> offeringKeys() {
        List<EntityDataAccessor<ItemStack>> keys = new ArrayList<>(SLOTS);
        for (int i = 0; i < SLOTS; i++) keys.add(SynchedEntityData.defineId(AudienceRitual.class, EntityDataSerializers.ITEM_STACK));
        return List.copyOf(keys);
    }

    @Override protected void defineSynchedData() {
        entityData.define(OWNER, Optional.empty());
        entityData.define(START, 0L);
        entityData.define(STAGE_START, 0L);
        entityData.define(STAGE, 0);
        entityData.define(HEIGHTS, new CompoundTag());
        entityData.define(DEPOSIT_TIMES, new CompoundTag());
        entityData.define(DRAW_TICKS, 0);
        for (var key : OFFERINGS) entityData.define(key, ItemStack.EMPTY);
    }

    public static Vec3 slotOffset(int index) { return OFFSETS[index]; }
    public static float slotRadius(int index) { return index < 8 ? 2.35f : index < CENTER ? 3.45f : 4.9f; }
    public double slotY(int index) {
        int[] heights = entityData.get(HEIGHTS).getIntArray("Offsets");
        return index < heights.length ? heights[index] / 16.0 : 0;
    }
    public Vec3 slotPosition(int index) { return position().add(slotOffset(index)).add(0, slotY(index), 0); }
    public ItemStack offering(int index) { return entityData.get(OFFERINGS.get(index)); }
    public float offeringAge(int index, float ritualAge) {
        return ritualAge - entityData.get(DEPOSIT_TIMES).getLong(Integer.toString(index));
    }
    public int stage() { return entityData.get(STAGE); }
    public float stageAge(float partial) { return Math.max(0, level().getGameTime() - entityData.get(STAGE_START) + partial); }
    public float age(float partial) { return Math.max(0, level().getGameTime() - entityData.get(START) + partial); }

    private boolean belongsTo(Player player) { return entityData.get(OWNER).filter(player.getUUID()::equals).isPresent(); }
    private ServerPlayer caster(ServerLevel world) {
        Entity entity = entityData.get(OWNER).map(world::getEntity).orElse(null);
        return entity instanceof ServerPlayer player ? player : null;
    }

    public float drawingProgress(float partial) {
        return stage() == DRAWING ? Math.min(1, age(partial) / Math.max(1, entityData.get(DRAW_TICKS))) : 1;
    }

    private static AudienceRitual preview(ServerPlayer caster) {
        var tag = caster.getPersistentData();
        if (!tag.hasUUID(PREVIEW)) return null;
        return caster.serverLevel().getEntity(tag.getUUID(PREVIEW)) instanceof AudienceRitual ritual
                && ritual.stage() == DRAWING && ritual.belongsTo(caster) ? ritual : null;
    }

    static void endDrawing(ServerPlayer caster) {
        AudienceRitual drawing = preview(caster);
        caster.getPersistentData().remove(PREVIEW);
        if (drawing != null) drawing.discard();
    }

    static void beginDrawing(ServerPlayer caster, int duration) {
        endDrawing(caster);
        AudienceRitual drawing = create(caster);
        drawing.entityData.set(DRAW_TICKS, Math.max(1, duration));
        drawing.setStage(DRAWING);
        if (caster.serverLevel().addFreshEntity(drawing)) caster.getPersistentData().putUUID(PREVIEW, drawing.getUUID());
    }

    private static AudienceRitual create(ServerPlayer caster) {
        ServerLevel world = caster.serverLevel();
        AudienceRitual ritual = new AudienceRitual(AudienceContent.RITUAL.get(), world);
        Vec3 start = caster.getEyePosition();
        HitResult ground = world.clip(new ClipContext(start, new Vec3(start.x, world.getMinBuildHeight(), start.z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        ritual.setPos(ground.getType() == HitResult.Type.MISS ? caster.position() : ground.getLocation());
        ritual.measureGround(world);
        long now = world.getGameTime();
        ritual.entityData.set(OWNER, Optional.of(caster.getUUID()));
        ritual.entityData.set(START, now);
        ritual.entityData.set(STAGE_START, now);
        return ritual;
    }

    public static void summon(ServerPlayer caster) {
        ServerLevel world = caster.serverLevel();
        AudienceRitual ritual = preview(caster);
        if (ritual == null) {
            ritual = create(caster);
            if (!world.addFreshEntity(ritual)) return;
        }
        caster.getPersistentData().remove(PREVIEW);
        ritual.setStage(0);
        ritual.nextPayment = world.getGameTime() + 20;
        UUID previous = caster.getPersistentData().hasUUID(ACTIVE) ? caster.getPersistentData().getUUID(ACTIVE) : null;
        caster.getPersistentData().putUUID(ACTIVE, ritual.getUUID());
        ritual.pin(world);
        if (previous != null) {
            for (ServerLevel level : world.getServer().getAllLevels()) {
                if (level.getEntity(previous) instanceof AudienceRitual old && old.stage() != 2) old.cancel(level);
            }
        }
    }

    private void measureGround(ServerLevel world) {
        int[] offsets = new int[SLOTS];
        for (int i = 0; i < CENTER; i++) {
            Vec3 point = position().add(slotOffset(i));
            BlockPos base = BlockPos.containing(point);
            if (!world.hasChunkAt(base)) continue;
            // Pick the closest support with clear space above, including slabs and steps.
            double nearest = Double.POSITIVE_INFINITY;
            for (int dy = -7; dy <= 6; dy++) {
                BlockPos pos = base.above(dy);
                VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
                if (shape.isEmpty() || !world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty()) continue;
                double top = pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
                if (Math.abs(top - point.y) < nearest) {
                    nearest = Math.abs(top - point.y);
                    offsets[i] = (int) Math.round((top - point.y) * 16);
                }
            }
        }
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Offsets", offsets);
        entityData.set(HEIGHTS, tag);
    }

    private void setStage(int stage) {
        entityData.set(STAGE, stage);
        entityData.set(STAGE_START, level().getGameTime());
    }

    private void pin(ServerLevel world) {
        world.getChunkSource().addRegionTicket(TICKET, chunkPosition(), 2, getUUID());
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel world)) return;
        if (tickCount == 1 || tickCount % 20 == 0) pin(world);
        if (stage() == DRAWING) {
            ServerPlayer player = caster(world);
            if (player == null || !player.isAlive() || preview(player) != this) discard();
            else {
                MagicData cast = MagicData.getPlayerMagicData(player);
                if (!cast.isCasting() || !SPELL_ID.equals(cast.getCastingSpellId())) discard();
            }
            return;
        }
        if (stage() == 2) {
            if (stageAge(0) >= FINALE_TICKS) discard();
            return;
        }
        ServerPlayer owner = caster(world);
        if (refunding || owner == null || !owner.isAlive() || !isCurrent(owner)) {
            cancel(world);
            return;
        }
        MagicData mana = MagicData.getPlayerMagicData(owner);
        long now = world.getGameTime();
        if (now >= nextPayment) {
            mana.setMana(Math.max(0, mana.getMana() - 1));
            if (owner.connection != null) PacketDistributor.sendToPlayer(owner, new SyncManaPacket(mana));
            nextPayment = now + 20;
        }
        if (mana.getMana() <= 0) {
            cancel(world);
        } else if (stage() == 1) {
            if (stageAge(0) >= ACTIVATION_TICKS) finish(world);
        } else if (tickCount % 4 == 0) {
            collectOfferings(world, owner);
        }
    }

    private boolean isCurrent(Player owner) {
        return owner.getPersistentData().hasUUID(ACTIVE) && owner.getPersistentData().getUUID(ACTIVE).equals(getUUID());
    }

    private int slotAt(Vec3 point) {
        for (int i = 0; i < SLOTS; i++) {
            Vec3 d = point.subtract(slotPosition(i));
            double radius = slotRadius(i);
            if (Math.abs(d.y) <= 1.5 && d.x * d.x + d.z * d.z <= radius * radius) return i;
        }
        return -1;
    }

    private boolean accepts(int slot, ItemStack stack) {
        if (stack.isEmpty() || !offering(slot).isEmpty()) return false;
        if (slot < 8) return stack.is(ExpansionCrafting.INK.get());
        if (slot == CENTER) return stack.is(ItemRegistry.BLOOD_STAFF.get());
        if (!RELICS.contains(ForgeRegistries.ITEMS.getKey(stack.getItem()))) return false;
        for (int i = 8; i < CENTER; i++) if (offering(i).is(stack.getItem())) return false;
        return true;
    }

    private void place(int slot, ItemStack item) {
        CompoundTag times = entityData.get(DEPOSIT_TIMES).copy();
        times.putLong(Integer.toString(slot), level().getGameTime() - entityData.get(START));
        entityData.set(DEPOSIT_TIMES, times);
        entityData.set(OFFERINGS.get(slot), item);
        Vec3 p = slotPosition(slot);
        level().playSound(null, p.x, p.y, p.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .9f, .8f + slot * .04f);
        for (int i = 0; i < SLOTS; i++) if (offering(i).isEmpty()) return;
        setStage(1);
    }

    private void collectOfferings(ServerLevel world, ServerPlayer caster) {
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, new AABB(position(), position()).inflate(27, 9, 27),
                entity -> entity.isAlive() && entity.getOwner() == caster)) {
            int slot = slotAt(item.position());
            if (slot < 0 || !accepts(slot, item.getItem())) continue;
            ItemStack remainder = item.getItem().copy();
            ItemStack offering = remainder.split(1);
            if (remainder.isEmpty()) item.discard(); else item.setItem(remainder);
            place(slot, offering);
            if (stage() != 0) return;
        }
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND || player.isSpectator()) return;
        Vec3 point = event.getHitVec().getLocation();
        AABB range = new AABB(point, point).inflate(29, 9, 29);
        for (AudienceRitual ritual : event.getLevel().getEntitiesOfClass(AudienceRitual.class, range,
                candidate -> !candidate.refunding && candidate.stage() == 0 && candidate.belongsTo(player))) {
            int slot = ritual.slotAt(point);
            if (slot < 0) continue;
            if (player instanceof ServerPlayer owner) {
                if (!ritual.isCurrent(owner)) continue;
                ritual.useSlot(owner, slot);
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
            return;
        }
    }

    private void useSlot(ServerPlayer player, int slot) {
        ItemStack hand = player.getMainHandItem();
        ItemStack contained = offering(slot);
        if (!contained.isEmpty()) {
            if (hand.isEmpty()) {
                entityData.set(OFFERINGS.get(slot), ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.MAIN_HAND, contained);
            } else {
                player.displayClientMessage(Component.translatable("message.fortune_spells.audience.occupied"), true);
            }
        } else if (accepts(slot, hand)) {
            place(slot, hand.split(1));
        } else if (slot < 8 || slot == CENTER) {
            player.displayClientMessage(Component.translatable("message.fortune_spells.audience.offering",
                    (slot < 8 ? ExpansionCrafting.INK.get() : ItemRegistry.BLOOD_STAFF.get()).getDescription()), true);
        } else {
            player.displayClientMessage(Component.translatable("message.fortune_spells.audience.boss_offering"), true);
        }
    }

    private boolean drop(ServerLevel world, ItemStack stack) {
        ItemEntity item = new ItemEntity(world, getX(), getY() + .9, getZ(), stack.copy());
        item.setDeltaMovement(Vec3.ZERO);
        item.setNoPickUpDelay();
        entityData.get(OWNER).ifPresent(item::setTarget);
        return world.addFreshEntity(item);
    }

    private void finish(ServerLevel world) {
        if (!drop(world, new ItemStack(AudienceContent.STAFF.get()))) return;
        for (var key : OFFERINGS) entityData.set(key, ItemStack.EMPTY);
        setStage(2);
        world.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2, .75f);
    }

    private void cancel(ServerLevel world) {
        refunding = true;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = offering(i);
            if (stack.isEmpty()) continue;
            if (!drop(world, stack)) return;
            entityData.set(OFFERINGS.get(i), ItemStack.EMPTY);
        }
        discard();
    }

    @Override public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel world && !isRemoved()) {
            if (reason.shouldDestroy() && !refunding && stage() != 2 && stage() != DRAWING) {
                cancel(world);
                return;
            }
            if (reason.shouldDestroy()) {
                ServerPlayer player = caster(world);
                if (player != null && isCurrent(player)) player.getPersistentData().remove(ACTIVE);
            }
            world.getChunkSource().removeRegionTicket(TICKET, chunkPosition(), 2, getUUID());
        }
        super.remove(reason);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        endDrawing(player);
        if (!player.getPersistentData().hasUUID(ACTIVE)) return;
        if (player.serverLevel().getEntity(player.getPersistentData().getUUID(ACTIVE)) instanceof AudienceRitual ritual
                && ritual.stage() != 2) ritual.cancel(player.serverLevel());
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        entityData.get(OWNER).ifPresent(id -> tag.putUUID("Caster", id));
        tag.putLong("Start", entityData.get(START));
        tag.putLong("StageStart", entityData.get(STAGE_START));
        tag.putInt("Stage", stage());
        tag.putInt("DrawTicks", entityData.get(DRAW_TICKS));
        tag.putLong("NextPayment", nextPayment);
        tag.putBoolean("Refunding", refunding);
        tag.put("Heights", entityData.get(HEIGHTS).copy());
        tag.put("Deposits", entityData.get(DEPOSIT_TIMES).copy());
        ListTag slots = new ListTag();
        for (int i = 0; i < SLOTS; i++) {
            if (offering(i).isEmpty()) continue;
            CompoundTag slot = offering(i).save(new CompoundTag());
            slot.putInt("Slot", i);
            slots.add(slot);
        }
        tag.put("Offerings", slots);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Caster")) entityData.set(OWNER, Optional.of(tag.getUUID("Caster")));
        entityData.set(START, tag.getLong("Start"));
        entityData.set(STAGE_START, tag.getLong("StageStart"));
        entityData.set(STAGE, tag.getInt("Stage"));
        entityData.set(DRAW_TICKS, tag.getInt("DrawTicks"));
        entityData.set(HEIGHTS, tag.getCompound("Heights"));
        entityData.set(DEPOSIT_TIMES, tag.getCompound("Deposits"));
        nextPayment = tag.getLong("NextPayment");
        refunding = tag.getBoolean("Refunding");
        ListTag slots = tag.getList("Offerings", Tag.TAG_COMPOUND);
        for (int i = 0; i < slots.size(); i++) {
            CompoundTag saved = slots.getCompound(i);
            int index = saved.getInt("Slot");
            if (index >= 0 && index < SLOTS) entityData.set(OFFERINGS.get(index), ItemStack.of(saved));
        }
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
