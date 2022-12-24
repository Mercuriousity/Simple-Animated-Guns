package net.elidhan.anim_guns.item;

import io.netty.buffer.Unpooled;
import net.elidhan.anim_guns.AnimatedGuns;
import net.elidhan.anim_guns.AnimatedGunsClient;
import net.elidhan.anim_guns.entity.projectile.BulletEntity;
import net.elidhan.anim_guns.util.InventoryUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.SoundKeyframeEvent;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.network.GeckoLibNetwork;
import software.bernie.geckolib3.network.ISyncable;
import software.bernie.geckolib3.util.GeckoLibUtil;

import java.util.Objects;
import java.util.Random;

public abstract class GunItem
extends Item
implements FabricItem, IAnimatable, ISyncable
{
    public AnimationFactory factory = GeckoLibUtil.createFactory(this);
    public static final String controllerName = "controller";
    private final String gunID;
    private final String animationID;
    private final float gunDamage;
    private final int rateOfFire;
    private final int magSize;
    private final Item ammoType;
    private final int reloadCooldown;
    private final float bulletSpread;
    private final float gunRecoilX;
    private final float gunRecoilY;
    private final int pelletCount;
    private final int loadingType;
    private final SoundEvent reloadSoundStart;
    private final SoundEvent reloadSoundMagOut;
    private final SoundEvent reloadSoundMagIn;
    private final SoundEvent reloadSoundEnd;
    private final SoundEvent shootSound;
    private final int reloadCycles;
    private final boolean isScoped;
    private final int reloadStage1;
    private final int reloadStage2;
    private final int reloadStage3;
    private boolean keyLock;
    public GunItem(Settings settings, String gunID, String animationID,
                   float gunDamage, int rateOfFire, int magSize,
                   Item ammoType, int reloadCooldown, float bulletSpread,
                   float gunRecoilX, float gunRecoilY, int pelletCount, int loadingType,
                   SoundEvent reloadSoundStart, SoundEvent reloadSoundMagOut, SoundEvent reloadSoundMagIn, SoundEvent reloadSoundEnd,
                   SoundEvent shootSound, int reloadCycles, boolean isScoped,
                   int reloadStage1, int reloadStage2, int reloadStage3)
    {
        super(settings.maxDamage((magSize*10)+1));
        GeckoLibNetwork.registerSyncable(this);

        this.gunID = gunID;
        this.animationID = animationID;
        this.gunDamage = gunDamage;
        this.rateOfFire = rateOfFire;
        this.magSize = magSize;
        this.ammoType = ammoType;
        this.reloadCooldown = reloadCooldown;
        this.bulletSpread = bulletSpread;
        this.gunRecoilX = gunRecoilX;
        this.gunRecoilY = gunRecoilY;
        this.pelletCount = pelletCount;
        this.loadingType = loadingType;
        this.reloadSoundStart = reloadSoundStart;
        this.reloadSoundMagOut = reloadSoundMagOut;
        this.reloadSoundMagIn = reloadSoundMagIn;
        this.reloadSoundEnd = reloadSoundEnd;
        this.shootSound = shootSound;
        this.reloadCycles = reloadCycles;
        this.isScoped = isScoped;
        this.reloadStage1 = reloadStage1;
        this.reloadStage2 = reloadStage2;
        this.reloadStage3 = reloadStage3;
        this.keyLock = false;
    }
    private <P extends Item & IAnimatable> PlayState predicate(AnimationEvent<P> event)
    {
        return PlayState.CONTINUE;
    }
    @Override
    public void registerControllers(AnimationData animationData)
    {
        AnimationController<GunItem> controller = new AnimationController(this, controllerName, 1, this::predicate);
        controller.registerSoundListener(this::soundListener);
        animationData.addAnimationController(controller);
    }
    @Override
    public void onAnimationSync(int id, int state)
    {
        final AnimationController controller = GeckoLibUtil.getControllerForID(this.factory, id, controllerName);
        switch (state)
        {
            case 0 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder().addAnimation("idle", ILoopType.EDefaultLoopTypes.LOOP));
            }
            case 1 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder()
                        .addAnimation("firing", ILoopType.EDefaultLoopTypes.PLAY_ONCE)
                        .addAnimation("idle", ILoopType.EDefaultLoopTypes.LOOP));
            }
            case 2 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder().addAnimation("reload_start", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
            }
            case 3 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder().addAnimation("reload_magout", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
            }
            case 4 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder().addAnimation("reload_magin", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
            }
            case 5 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder().addAnimation("reload_end", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
            }
            case 6 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder().addAnimation("aim", ILoopType.EDefaultLoopTypes.LOOP));
            }
            case 7 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder()
                        .addAnimation("aim_firing", ILoopType.EDefaultLoopTypes.PLAY_ONCE)
                        .addAnimation("aim", ILoopType.EDefaultLoopTypes.LOOP));
            }
            case 8 ->
            {
                controller.markNeedsReload();
                controller.setAnimation(new AnimationBuilder()
                        .addAnimation("aim_reload_start", ILoopType.EDefaultLoopTypes.PLAY_ONCE));
            }
        }
    }
    protected <ENTITY extends IAnimatable> void soundListener(SoundKeyframeEvent<ENTITY> event)
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            switch (event.sound)
            {
                case "reload_start" ->
                        player.playSound(this.reloadSoundStart, SoundCategory.MASTER, 1, 1);
                case "reload_magout" ->
                        player.playSound(this.reloadSoundMagOut, SoundCategory.MASTER, 1, 1);
                case "reload_magin" ->
                        player.playSound(this.reloadSoundMagIn, SoundCategory.MASTER, 1, 1);
                case "reload_end" ->
                        player.playSound(this.reloadSoundEnd, SoundCategory.MASTER, 1, 1);
            }
        }
    }
    @Override
    public AnimationFactory getFactory()
    {
        return this.factory;
    }
    private void setDefaultNBT(NbtCompound nbtCompound)
    {
        nbtCompound.putInt("reloadTick", 0);
        nbtCompound.putInt("currentCycle", 1);
        nbtCompound.putInt("Clip", 0);

        nbtCompound.putBoolean("isScoped", this.isScoped);
        nbtCompound.putBoolean("isReloading", false);
        nbtCompound.putBoolean("isAiming", false);
    }
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected)
    {
        NbtCompound nbtCompound = stack.getOrCreateNbt();
        //Just setting the gun's default NBT tags
        //If there's a better way to do this, let me know
        if (!nbtCompound.contains("reloadTick") || !nbtCompound.contains("Clip") || !nbtCompound.contains("currentCycle") || !nbtCompound.contains("isScoped") || !nbtCompound.contains("isReloading") || !nbtCompound.contains("isAiming"))
        {
            setDefaultNBT(nbtCompound);
        }
        //This part's for the keypress
        //to reload and aim the gun
        if (world.isClient())
        {
            if (((PlayerEntity) entity).getMainHandStack() == stack
                    && AnimatedGunsClient.reloadToggle.isPressed()
                    && remainingAmmo(stack) < this.magSize
                    && reserveAmmoCount(((PlayerEntity) entity), this.ammoType) > 0
                    && !nbtCompound.getBoolean("isReloading"))
            {
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeBoolean(true);
                ClientPlayNetworking.send(new Identifier(AnimatedGuns.MOD_ID, "reload"), buf);
            }
            if (((PlayerEntity) entity).getMainHandStack() == stack && AnimatedGunsClient.aimToggle.isPressed() && !nbtCompound.getBoolean("isReloading") && !this.keyLock)
            {
                this.keyLock = true;
                PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                buf.writeBoolean(!stack.getOrCreateNbt().getBoolean("isAiming"));
                ClientPlayNetworking.send(new Identifier(AnimatedGuns.MOD_ID, "aim"), buf);
            }
            else if (!AnimatedGunsClient.aimToggle.isPressed())
            {
                this.keyLock = false;
            }
        }

        //The actual reload process/tick
        if (nbtCompound.getBoolean("isReloading"))
        {
            if((((PlayerEntity)entity).getMainHandStack() != stack
                    || (reserveAmmoCount((PlayerEntity) entity, this.ammoType) <= 0 && this.reloadCycles <= 1)
                    || (nbtCompound.getInt("reloadTick") >= this.reloadCooldown)
                    || (remainingAmmo(stack) >= this.magSize && this.reloadCycles <= 1)))
                nbtCompound.putBoolean("isReloading",false);

            doReloadTick(world, nbtCompound, (PlayerEntity)entity, stack);
        }
        else
        {
            if (nbtCompound.getInt("reloadTick") > this.reloadStage3 && nbtCompound.getInt("reloadTick") <= this.reloadCooldown)
                finishReload((PlayerEntity) entity, stack);

            nbtCompound.putInt("reloadTick", 0);
        }
    }

    private void doReloadTick(World world, NbtCompound nbtCompound, PlayerEntity player, ItemStack stack)
    {
        int rTick = nbtCompound.getInt("reloadTick");

        if (world instanceof ServerWorld)
        {
            if(nbtCompound.getInt("reloadTick") == 0)
            {
                final int id = GeckoLibUtil.guaranteeIDForStack(stack, (ServerWorld)world);
                GeckoLibNetwork.syncAnimation(player, this, id, nbtCompound.getBoolean("isAiming")?8:2);
                for (PlayerEntity otherPlayer : PlayerLookup.tracking(player))
                {
                    GeckoLibNetwork.syncAnimation(otherPlayer, this, id, nbtCompound.getBoolean("isAiming")?8:2);
                }
                nbtCompound.putBoolean("isAiming", false);
            }
            else if(nbtCompound.getInt("reloadTick") == this.reloadStage1)
            {
                final int id = GeckoLibUtil.guaranteeIDForStack(stack, (ServerWorld)world);
                GeckoLibNetwork.syncAnimation(player, this, id, 3);
                for (PlayerEntity otherPlayer : PlayerLookup.tracking(player))
                {
                    GeckoLibNetwork.syncAnimation(otherPlayer, this, id, 3);
                }
            }
            else if(nbtCompound.getInt("reloadTick") == this.reloadStage2)
            {
                final int id = GeckoLibUtil.guaranteeIDForStack(stack, (ServerWorld)world);
                GeckoLibNetwork.syncAnimation(player, this, id, 4);
                for (PlayerEntity otherPlayer : PlayerLookup.tracking(player))
                {
                    GeckoLibNetwork.syncAnimation(otherPlayer, this, id, 4);
                }
            }
            else if(nbtCompound.getInt("reloadTick") == this.reloadStage3)
            {
                final int id = GeckoLibUtil.guaranteeIDForStack(stack, (ServerWorld)world);
                GeckoLibNetwork.syncAnimation(player, this, id, 5);
                for (PlayerEntity otherPlayer : PlayerLookup.tracking(player))
                {
                    GeckoLibNetwork.syncAnimation(otherPlayer, this, id, 5);
                }
            }
        }

        nbtCompound.putInt("reloadTick", nbtCompound.getInt("reloadTick") + 1);

        switch(this.loadingType)
        {
            case 1:
                if(rTick >= this.reloadCooldown
                        && reserveAmmoCount(player, this.ammoType) > 0)
                {
                    nbtCompound.putInt("currentCycle", 1);
                    finishReload(player, stack);
                    nbtCompound.putInt("reloadTick", 0);
                }
                break;
            case 2:
                if (rTick >= this.reloadStage3
                        && nbtCompound.getInt("currentCycle") < this.reloadCycles
                        && reserveAmmoCount(player, this.ammoType) > 0)
                {
                    nbtCompound.putInt("Clip", nbtCompound.getInt("Clip")+1);
                    InventoryUtil.removeItemFromInventory(player, this.ammoType, 1);
                    if (remainingAmmo(stack) < this.magSize && reserveAmmoCount(player, this.ammoType) > 0)
                    {
                        nbtCompound.putInt("reloadTick", this.reloadStage2);
                    }
                    nbtCompound.putInt("currentCycle", nbtCompound.getInt("Clip"));
                    stack.setDamage(this.getMaxDamage()-((nbtCompound.getInt("Clip")*10)+1));
                }
                break;
        }
    }
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand)
    {
        ItemStack itemStack = user.getStackInHand(hand);

        if (hand == Hand.MAIN_HAND && !user.isSprinting() && isLoaded(itemStack))
        {
            shoot(world, user, itemStack);
        }
        return TypedActionResult.fail(itemStack);
    }
    public void shoot(World world, PlayerEntity user, ItemStack itemStack)
    {
        float v_kick = user.getPitch() - getRecoil(itemStack);
        user.getItemCooldownManager().set(this, this.rateOfFire);

        if (!world.isClient())
        {
            for(int i = 0; i < this.pelletCount; i++)
            {
                BulletEntity bullet = new BulletEntity(user, world, this.gunDamage);
                bullet.setPos(user.getX(),user.getEyeY(),user.getZ());
                bullet.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 10, this.bulletSpread);
                bullet.setAccel(bullet.getVelocity());

                world.spawnEntity(bullet);
            }
            final int id = GeckoLibUtil.guaranteeIDForStack(itemStack, (ServerWorld) world);
            GeckoLibNetwork.syncAnimation(user, this, id, !itemStack.getOrCreateNbt().getBoolean("isScoped") && itemStack.getOrCreateNbt().getBoolean("isAiming")?7:1);
            for (PlayerEntity otherPlayer : PlayerLookup.tracking(user))
            {
                GeckoLibNetwork.syncAnimation(otherPlayer, this, id, !itemStack.getOrCreateNbt().getBoolean("isScoped") && itemStack.getOrCreateNbt().getBoolean("isAiming")?7:1);
            }

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeFloat(v_kick);
            ServerPlayNetworking.send(((ServerPlayerEntity) user), AnimatedGuns.RECOIL_PACKET_ID, buf);
        }

        if(!user.getAbilities().creativeMode)
        {
            itemStack.getOrCreateNbt().putInt("Clip", itemStack.getOrCreateNbt().getInt("Clip") - 1);
            itemStack.damage(10, user, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }

        world.playSound(null,
                user.getX(),
                user.getY(),
                user.getZ(),
                shootSound,SoundCategory.MASTER,1.0f, 1.0f);

        if(this.reloadCycles > 1)
        {
            itemStack.getOrCreateNbt().putInt("currentCycle", itemStack.getOrCreateNbt().getInt("Clip"));
        }

        itemStack.getOrCreateNbt().putInt("reloadTick",0);
        itemStack.getOrCreateNbt().putBoolean("isReloading",false);
        if(itemStack.getOrCreateNbt().getBoolean("isScoped"))
            itemStack.getOrCreateNbt().putBoolean("isAiming", false);
    }
    private float getRecoil(ItemStack stack)
    {
        return stack.getOrCreateNbt().getBoolean("isAiming") ? this.gunRecoilY / 2 : this.gunRecoilY;
    }
    public static boolean isLoaded(ItemStack stack)
    {
        return remainingAmmo(stack) > 0;
    }
    public void finishReload(PlayerEntity player, ItemStack stack)
    {
        NbtCompound nbtCompound = stack.getOrCreateNbt();

        int ammoToLoad = this.magSize - nbtCompound.getInt("Clip");

        if (reserveAmmoCount(player, this.ammoType) >= ammoToLoad)
        {
            nbtCompound.putInt("Clip", nbtCompound.getInt("Clip") + ammoToLoad);
            InventoryUtil.removeItemFromInventory(player, this.ammoType, ammoToLoad);
        }
        else
        {
            nbtCompound.putInt("Clip", nbtCompound.getInt("Clip") + reserveAmmoCount(player, this.ammoType));
            InventoryUtil.removeItemFromInventory(player, this.ammoType, reserveAmmoCount(player, this.ammoType));
        }

        stack.setDamage(this.getMaxDamage()-((nbtCompound.getInt("Clip")*10)+1));
    }
    public void toggleAim(ItemStack stack, boolean aim, ServerWorld world, PlayerEntity player)
    {
        stack.getOrCreateNbt().putBoolean("isAiming", aim);

        final int id = GeckoLibUtil.guaranteeIDForStack(stack, world);
        GeckoLibNetwork.syncAnimation(player, this, id, stack.getOrCreateNbt().getBoolean("isAiming")?6:0);
        for (PlayerEntity otherPlayer : PlayerLookup.tracking(player))
        {
            GeckoLibNetwork.syncAnimation(otherPlayer, this, id, stack.getOrCreateNbt().getBoolean("isAiming")?6:0);
        }
    }
    private static int remainingAmmo(ItemStack stack)
    {
        NbtCompound nbtCompound = stack.getOrCreateNbt();
        return nbtCompound.getInt("Clip");
    }
    public static int reserveAmmoCount(PlayerEntity player, Item item)
    {
        return InventoryUtil.itemCountInInventory(player, item);
    }
    @Override
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack)
    {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return false;
    }
    public String getID()
    {
        return this.gunID;
    }
    public String getAnimationID()
    {
        return this.animationID;
    }
}
