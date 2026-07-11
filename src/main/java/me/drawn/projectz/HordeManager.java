package me.drawn.projectz;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = ProjectZ.MODID)
public class HordeManager {

    private static boolean hordeTriggered = false;
    private static final float zombieHealth = 18.0F;

    public static boolean isUndead(LivingEntity entity) {
        return entity.getType().is(EntityTypeTags.UNDEAD);
    }

    // optimizations
    @SubscribeEvent
    public static void onZombieJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Zombie zombie) {
            //zombie.setCanPickUpLoot(false);
            zombie.setBaby(false);

            // letting HordeManager handle it
            zombie.goalSelector.removeAllGoals(g -> true);
            zombie.targetSelector.removeAllGoals(g -> true);

            zombie.setSpeed(1.0F);

            if (!zombie.getPersistentData().contains("projectZ")) {
                zombie.getPersistentData().putBoolean("projectZ", true);

                double scaleVariance = (-0.5 + 1.5 * Math.random()) / 10;
                zombie.getAttribute(Attributes.SCALE).setBaseValue(zombie.getAttributeBaseValue(Attributes.SCALE) + scaleVariance);

                zombie.setHealth(zombieHealth);
                zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(zombieHealth);

                zombie.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(0);

                zombie.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.25D);
            }
        }
    }

    @SubscribeEvent
    public static void onZombieDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getEntity() instanceof Zombie zombie) {
            zombie.noPhysics = true;
        }
    }

    @SubscribeEvent
    public static void sound(PlayLevelSoundEvent.AtPosition e) {
        if (e.getLevel().isClientSide()) return;
        if (e.getSound() == null) return;

        if(sounds().contains(e.getSource())) {
            double radius = e.getOriginalVolume()*8.0;
            final String name = e.getSound().value().getLocation().toString();

            if(name.contains("pressure"))
                return;
            if(name.contains("alarm"))
                radius = 64;
            if(name.contains("headshot"))
                radius = (radius > 4) ? 4.0 : radius;

            // suppressors & silencers
            if(name.contains("suppre") || name.contains("silen") || name.contains("_s"))
                radius = (radius > 6) ? 6.0 : radius;

            if(name.contains("eat"))
                radius = (radius > 6) ? 6.0 : radius;

            //System.out.println("atPosition: "+name+" - "+radius+" - "+e.getSource().getName());
            broadcastNoise((ServerLevel) e.getLevel(),
                    new BlockPos((int)e.getPosition().x, (int)e.getPosition().y, (int)e.getPosition().z), radius, radius >= 16);
        }
    }
    protected static List<SoundSource> sounds() {
        return ImmutableList.of(SoundSource.PLAYERS, SoundSource.BLOCKS);
    }

    // hearing system, listens to block-related sounds
    private static final double blockEventRadius = 6.0;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()) {
            broadcastNoise((ServerLevel) event.getLevel(), event.getPos(), blockEventRadius, false);
        }
    }
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Player) {
            broadcastNoise((ServerLevel) event.getLevel(), event.getPos(), blockEventRadius, false);
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamageEvent(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide())
            return;
        if(e.getSource().getEntity() == null)
            return;

        if(e.getSource().getEntity() instanceof Player p
        && e.getEntity() instanceof Zombie zombie) {
            broadcastNoise((ServerLevel) zombie.level(), zombie.blockPosition(), 8.0, true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide()) {
            broadcastNoise((ServerLevel) event.getLevel(), BlockPos.containing(event.getExplosion().center()), 64.0, true);
        }
    }

    private static void broadcastNoise(ServerLevel level, BlockPos pos, double radius, boolean global) {
        int zombies = 0;
        List<Zombie> nearby = level.getEntitiesOfClass(Zombie.class, new net.minecraft.world.phys.AABB(pos).inflate(radius));
        for (Zombie z : nearby) {
            if (z.getTarget() == null) { // only cares about sound if not chasing anyone
                if(!global && zombies >= (radius/2))
                    return;
                zombies++;

                CompoundTag data = z.getPersistentData();
                data.putDouble("InvX", pos.getX());
                data.putDouble("InvY", pos.getY());
                data.putDouble("InvZ", pos.getZ());
                data.putBoolean("HasInv", true);
            }
        }
    }

    public static boolean isBreakable(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) return false;

        VoxelShape collisionShape = state.getCollisionShape(level, pos);

        return collisionShape != Shapes.empty() || !collisionShape.isEmpty();
    }

    // horde brain
    @SubscribeEvent
    public static void onGlobalZombieTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Zombie zombie && !zombie.level().isClientSide()) {
            ServerLevel level = (ServerLevel) zombie.level();
            CompoundTag data = zombie.getPersistentData();
            LivingEntity target = zombie.getTarget();

            if(zombie.isOnFire())
                zombie.extinguishFire();

            zombie.getNavigation().stop(); // stopping vanilla pathfinding

            // radar each 10 ticks
            if (zombie.tickCount % 10 == 0) {
                // clear invalid targets
                if (target instanceof Player p && (p.isCreative() || p.isSpectator() || !p.isAlive())) {
                    zombie.setTarget(null);
                    target = null;
                }

                // search for new target
                if (target == null) {
                    // nearest in 16 block radius
                    Player nearest = level.getNearestPlayer(zombie, 16.0);

                    if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && nearest.isAlive()) {
                        double distSqr = zombie.distanceToSqr(nearest);

                        // 1. light factor
                        // 15 = 1.0 (100% visible)
                        int lightLevel = level.getMaxLocalRawBrightness(nearest.blockPosition());
                        double lightMultiplier = 0.3 + (0.7 * (lightLevel / 15.0));

                        // 2. stealth factor (crouching, crawling, etc.)
                        double stealthPenalty = nearest.isShiftKeyDown() ? 0.5 : 1.0;

                        if(nearest.isVisuallyCrawling() || nearest.isSwimming() || nearest.isVisuallySwimming())
                            stealthPenalty = 0.1;

                        // 3. max visibility distance in these conditions
                        double baseVisionRange = 12.0; // base distance, almost making this guy blind lmao
                        double maxAllowedDist = baseVisionRange * lightMultiplier * stealthPenalty;
                        double maxAllowedDistSqr = maxAllowedDist * maxAllowedDist;

                        // discard off radius target
                        if (distSqr <= maxAllowedDistSqr) {
                            boolean canSee = false;

                            // fov
                            if (distSqr < 4.0) {
                                // ignores fov because the zombie can sense players too near
                                canSee = true;
                            } else {
                                // fov math using dot product
                                Vec3 lookVec = zombie.getViewVector(1.0F).normalize();
                                Vec3 toPlayerVec = nearest.getEyePosition().subtract(zombie.getEyePosition()).normalize();

                                double dotProduct = lookVec.dot(toPlayerVec);

                                // dotProduct goes from -1 (back) to 1 (front)
                                // > 0.5 makes 120 degrees FOV
                                if (dotProduct > 0.5) {
                                    canSee = true;
                                }
                            }

                            if(level.getBlockState(nearest.blockPosition()).is(BlockTags.TALL_FLOWERS))
                                canSee = false;

                            // line of sight
                            if (canSee && zombie.getSensing().hasLineOfSight(nearest)) {
                                level.playSound(null, zombie.blockPosition(),
                                        SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2, 1);

                                zombie.setTarget(nearest);
                                target = nearest;

                                // HIVEMIND: broadcasts the target to other zombies
                                List<Zombie> colmeia = level.getEntitiesOfClass(Zombie.class, zombie.getBoundingBox().inflate(24.0));
                                for (Zombie z : colmeia) {
                                    if (z.getTarget() == null) z.setTarget(nearest);
                                }
                            }
                        }
                    }
                }

                // logic of getting rid of the target
                // this way players can run, get lost and the zombie will stop chasing them
                if (target != null) {
                    if (zombie.getSensing().hasLineOfSight(target)) {
                        data.putInt("LostSight", 0);
                    } else {
                        int lost = data.getInt("LostSight") + 10;
                        data.putInt("LostSight", lost);
                        // if out of sight for 10 seconds, zombie will go investigate and stop chasing
                        if (lost > 200) {
                            data.putDouble("InvX", target.getX());
                            data.putDouble("InvY", target.getY());
                            data.putDouble("InvZ", target.getZ());
                            data.putBoolean("HasInv", true);
                            zombie.setTarget(null);
                            target = null;
                        }
                    }
                }
            }

            // movement, runs every tick
            double destX = 0, destY = 0, destZ = 0;
            double speed = 0;
            boolean isChasing = false;
            boolean isMoving = false;

            if (target != null) {
                // chasing mode
                destX = target.getX(); destY = target.getY(); destZ = target.getZ();

                final double variance = Math.random() / 5;
                speed = 1 + variance; //1.45D;

                isChasing = true;
                isMoving = true;
                if (zombie.distanceToSqr(target) < 1.75 && zombie.getSensing().hasLineOfSight(target)
                && target.onGround()) {
                    zombie.doHurtTarget(target);
                    zombie.swing(InteractionHand.MAIN_HAND);
                    zombie.swing(InteractionHand.OFF_HAND);
                }
            } else if (data.getBoolean("HasInv")) {
                // investigation mode, will look after sounds
                destX = data.getDouble("InvX"); destY = data.getDouble("InvY"); destZ = data.getDouble("InvZ");
                speed = 0.9;
                isMoving = true;
                if (zombie.distanceToSqr(destX, destY, destZ) < 6.0D) {
                    data.putBoolean("HasInv", false); // got into the location
                }
            } else {
                // wandering (idle), every 40 ticks
                if (zombie.tickCount % 40 == 0 && level.random.nextInt(3) == 0) {
                    destX = zombie.getX() + (level.random.nextDouble() - 0.5) * 16;
                    destY = zombie.getY();
                    destZ = zombie.getZ() + (level.random.nextDouble() - 0.5) * 16;
                    data.putDouble("InvX", destX);
                    data.putDouble("InvY", destY);
                    data.putDouble("InvZ", destZ);
                    data.putBoolean("HasInv", true);
                }
            }

            // applies the movement
            if (isMoving) {
                zombie.getLookControl().setLookAt(destX, destY + 1.5, destZ, 30.0F, 30.0F); // looks to the target loc
                zombie.getMoveControl().setWantedPosition(destX, destY, destZ, speed); // go to target loc
            }

            // block destruction & stacking logic
            if (zombie.horizontalCollision && isMoving
            && isChasing) {
                // if hitting a wall, starts stacking over other zombies, temporarily removed
                /*if (!zombie.onGround()) {
                    Vec3 delta = zombie.getDeltaMovement();
                    zombie.setDeltaMovement(delta.x, 0.45, delta.z);
                }*/

                // front block
                final Vec3 dir = new Vec3(destX - zombie.getX(), 0, destZ - zombie.getZ()).normalize();

                final double checkDistance = 0.65;

                BlockPos frontPos = BlockPos.containing(
                        zombie.getX() + dir.x * checkDistance,
                        zombie.getY() + 1.0, // chest height
                        zombie.getZ() + dir.z * checkDistance
                );
                BlockState state = level.getBlockState(frontPos);

                if (!isBreakable(state, level, frontPos) || state.getDestroySpeed(level, frontPos) < 0) {
                    frontPos = BlockPos.containing(
                            zombie.getX() + dir.x * checkDistance,
                            zombie.getY(), // legs height
                            zombie.getZ() + dir.z * checkDistance
                    );
                    state = level.getBlockState(frontPos);
                }

                float hardness = state.getDestroySpeed(level, frontPos);

                // if breakable and hardness <= 2.5
                if (hardness >= 0 && hardness <= 2.5F && isBreakable(state, level, frontPos)) {
                    int breakX = data.getInt("BreakX");
                    int breakY = data.getInt("BreakY");
                    int breakZ = data.getInt("BreakZ");
                    BlockPos lastPos = new BlockPos(breakX, breakY, breakZ);

                    // if the block changes, resets timer
                    if (!frontPos.equals(lastPos)) {
                        data.putInt("BreakTicks", 0);
                        data.putInt("BreakX", frontPos.getX());
                        data.putInt("BreakY", frontPos.getY());
                        data.putInt("BreakZ", frontPos.getZ());
                        zombie.setSpeed(1.0F);
                        level.destroyBlockProgress(zombie.getId(), lastPos, -1);
                    }

                    int breakTicks = data.getInt("BreakTicks") + 1;
                    data.putInt("BreakTicks", breakTicks);

                    final int slowMultiplier = 40;
                    int requiredTicks = (int) Math.max(10, hardness * slowMultiplier);

                    // breaking progress animation
                    int progress = (int) ((breakTicks / (float) requiredTicks) * 10.0F);
                    level.destroyBlockProgress(zombie.getId(), frontPos, progress);

                    zombie.setSpeed(0.05F);

                    level.playSound(null, zombie.blockPosition(),
                            SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 1, 1);

                    zombie.swing(InteractionHand.MAIN_HAND);
                    zombie.swing(InteractionHand.OFF_HAND);

                    if (breakTicks >= requiredTicks) {
                        level.destroyBlock(frontPos, true);
                        data.putInt("BreakTicks", 0);
                        zombie.setSpeed(1.0F);
                        level.destroyBlockProgress(zombie.getId(), frontPos, -1);

                        level.playSound(null, zombie.blockPosition(),
                                SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 0.5f, 1);
                    }
                }
            } else {
                // stopped colliding, stop destroying block animations
                if (data.getInt("BreakTicks") > 0) {
                    BlockPos lastPos = new BlockPos(data.getInt("BreakX"), data.getInt("BreakY"), data.getInt("BreakZ"));
                    level.destroyBlockProgress(zombie.getId(), lastPos, -1);
                    zombie.setSpeed(1.0F);
                    data.putInt("BreakTicks", 0);
                }
            }
        }
    }

    // horde triggering
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            long dayTime = serverLevel.getDayTime();
            long day = dayTime / 24000L;

            if (day > 0 && day % ProjectZ.HORDE_DAY_INTERVAL == 0) {
                if (dayTime % 24000L > 13000 && !hordeTriggered) {
                    for (Player p : serverLevel.players()) {
                        p.displayClientMessage(Component.literal("§4A HORDA OUVIU VOCÊ!"), true);
                        serverLevel.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 0.5f);
                        spawnHordeAsync(serverLevel, p);
                    }
                    hordeTriggered = true;
                }
            }
            if (dayTime % 24000L < 1000) hordeTriggered = false;
        }
    }

    private static void spawnHordeAsync(ServerLevel level, Player target) {
        CompletableFuture.runAsync(() -> {
            List<BlockPos> spawns = new ArrayList<>();
            BlockPos center = target.blockPosition();

            for (int i = 0; i < ProjectZ.HORDE_CAP; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 24.0 + (Math.random() * 30.0);

                int x = (int) (center.getX() + Math.cos(angle) * dist);
                int z = (int) (center.getZ() + Math.sin(angle) * dist);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos.below()).isSolid()) {
                    spawns.add(pos);
                }
            }

            level.getServer().execute(() -> {
                for (BlockPos pos : spawns) {
                    Zombie zombie = EntityType.ZOMBIE.create(level);
                    if (zombie != null) {
                        zombie.moveTo(pos.getX(), pos.getY(), pos.getZ());
                        zombie.setTarget(target);
                        level.addFreshEntity(zombie);
                    }
                }
            });
        });
    }
}