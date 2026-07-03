package me.drawn.projectz.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Zombie;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class RealisticZombieModel<T extends Zombie> extends ZombieModel<T> {

    public RealisticZombieModel(ModelPart root) {
        super(root);
    }

    private static final String key = "zombieType";

    @Override
    public void setupAnim(@NotNull T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        CompoundTag persistentData = entity.getPersistentData();
        int typeIndex;

        if (persistentData.contains(key)) {
            typeIndex = persistentData.getInt(key);
        } else {
            typeIndex = ThreadLocalRandom.current().nextInt(8);
            persistentData.putInt(key, typeIndex);
        }

        // breathing and steps frequency
        float walkSpeed = limbSwing * 0.6662F;
        float breathing = Mth.sin(ageInTicks * 0.08F) * 0.05F;

        float bodyBob = Mth.sin(walkSpeed * 2.0F) * 0.4F;
        float limpBob = Mth.cos(walkSpeed) * 1.2F;
        float dynamicYOffset = (bodyBob + limpBob) * limbSwingAmount;

        float upperY = 2.0F + dynamicYOffset;
        float upperZ = -4.0F;

        this.body.y = upperY;
        this.body.z = upperZ;
        this.body.xRot = 0.35F + Mth.sin(walkSpeed * 2.0F) * 0.05F * limbSwingAmount;
        this.body.zRot = (Mth.cos(walkSpeed) * 0.12F + 0.05F) * limbSwingAmount;

        // arms
        this.rightArm.y = upperY + 2.0F;
        this.rightArm.z = upperZ;
        this.leftArm.y = upperY + 2.0F;
        this.leftArm.z = upperZ;

        // right arm
        this.rightArm.xRot = -0.5F + Mth.sin(walkSpeed) * 0.25F * limbSwingAmount - breathing;
        this.rightArm.yRot = -0.15F + Mth.cos(walkSpeed) * 0.1F * limbSwingAmount;
        this.rightArm.zRot = 0.1F + breathing * -0.3F;

        // left arm
        this.leftArm.xRot = 0.15F + Mth.cos(walkSpeed) * 0.35F * limbSwingAmount + breathing;
        this.leftArm.yRot = 0.15F;
        this.leftArm.zRot = -0.12F + breathing * 0.3F;

        // head
        float headXRot = (headPitch * ((float)Math.PI / 180F)) - 0.1F + breathing * 0.3F;
        float headYRot = (netHeadYaw * ((float)Math.PI / 180F));

        // head swing
        headXRot += Mth.cos(walkSpeed * 2.0F) * 0.05F * limbSwingAmount;
        headYRot += Mth.sin(walkSpeed) * 0.12F * limbSwingAmount;

        final float finalHeadX = headXRot;
        final float finalHeadY = headYRot;

        this.heads().forEach(part -> {
            part.y = upperY;
            part.z = upperZ;
            part.xRot = finalHeadX;
            part.yRot = finalHeadY;
            part.zRot = 0.0F;
        });

        // idle
        float typeZRot = breathing * 0.4F;
        if (typeIndex == 0 || typeIndex == 1) {
            this.heads().forEach(part -> part.zRot += typeZRot);
            this.body.yRot = 0.15F;
        } else if (typeIndex == 2 || typeIndex == 3) {
            this.heads().forEach(part -> part.zRot += typeZRot);
            this.body.yRot = -0.15F;
        } else if (typeIndex == 4 || typeIndex == 5) {
            this.heads().forEach(part -> part.zRot += typeZRot);
            this.body.yRot = 0.0F;
        } else if (typeIndex == 6) {
            this.heads().forEach(part -> part.zRot += typeZRot * 0.8F);
            this.body.yRot = 0.05F;
        } else if (typeIndex == 7) {
            this.heads().forEach(part -> part.zRot -= typeZRot * 0.8F);
            this.body.yRot = -0.05F;
        }

        // walking swing
        float rightLegLift = 0.0F;
        if (Mth.cos(walkSpeed) > 0.0F) {
            // negative values to move the leg up a little bit
            rightLegLift = Mth.sin(walkSpeed) * -2.2F * limbSwingAmount;
        }
        this.rightLeg.y = 12.0F + rightLegLift;
        this.rightLeg.xRot = Mth.cos(walkSpeed) * 1.1F * limbSwingAmount;
        this.rightLeg.yRot = 0.0F;
        this.rightLeg.zRot = 0.02F;

        // left leg, more grounded and splayed
        float leftLegLift = 0.0F;
        if (Mth.cos(walkSpeed + Mth.PI) > 0.0F) {
            leftLegLift = Mth.sin(walkSpeed + Mth.PI) * -0.4F * limbSwingAmount; // minimum lift
        }
        this.leftLeg.y = 12.0F + leftLegLift;
        // short steps and more frontwards
        this.leftLeg.xRot = Mth.cos(walkSpeed + Mth.PI) * 0.45F * limbSwingAmount + (0.08F * limbSwingAmount);
        this.leftLeg.yRot = 0.22F * limbSwingAmount; // off
        this.leftLeg.zRot = 0.06F * limbSwingAmount; // side-angled

        // chasing
        if (entity.isAggressive()) {
            // body goes even more to the front
            this.body.xRot += 0.25F;

            // shaking
            float franticShake = Mth.sin(ageInTicks * 0.4F) * 0.12F;

            this.rightArm.xRot = -1.3F + franticShake + Mth.sin(walkSpeed * 1.4F) * 0.15F * limbSwingAmount;
            this.rightArm.yRot = -0.2F + Mth.cos(ageInTicks * 0.1F) * 0.05F;
            this.rightArm.zRot = 0.05F;

            this.leftArm.xRot = -1.1F - franticShake + Mth.cos(walkSpeed * 1.4F) * 0.15F * limbSwingAmount;
            this.leftArm.yRot = 0.2F - Mth.sin(ageInTicks * 0.1F) * 0.05F;
            this.leftArm.zRot = -0.05F;

            // body lateral swing
            this.body.zRot += Mth.cos(walkSpeed) * 0.18F * limbSwingAmount;
            this.heads().forEach(part -> part.xRot += 0.1F);
        }
    }

    protected Iterable<ModelPart> upperParts() {
        return ImmutableList.of(this.body, this.rightArm, this.leftArm, this.hat, this.head);
    }

    protected Iterable<ModelPart> arms() {
        return ImmutableList.of(this.rightArm, this.leftArm);
    }

    protected Iterable<ModelPart> heads() {
        return ImmutableList.of(this.head, this.hat);
    }
}