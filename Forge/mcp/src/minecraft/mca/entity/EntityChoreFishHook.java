/*******************************************************************************
 * EntityChoreFishHook.java
 * Copyright (c) 2013 WildBamaBoy.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/

package mca.entity;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Replaces the standard EntityFishHook used by Minecraft.
 */
public class EntityChoreFishHook extends EntityFishHook
{
	private int xTile;
	private int yTile;
	private int zTile;
	private int inTile;
	private int ticksInGround;
	private int ticksInAir;
	private int ticksCatchable;
	private int fishPosRotationIncrements;
	private boolean inGround;
	private double fishX;
	private double fishY;
	private double fishZ;
	private double fishYaw;
	private double fishPitch;

	/** An instance of the person holding the fishing rod. */
	public AbstractEntity angler;

	/** An instance of the bobber at the end of the fishing line. */
	public Entity bobber;

	@SideOnly(Side.CLIENT)
	private double velocityX;
	@SideOnly(Side.CLIENT)
	private double velocityY;
	@SideOnly(Side.CLIENT)
	private double velocityZ;

	/**
	 * Constructor
	 * 
	 * @param 	world	The world the entity should be spawned in.
	 */
	public EntityChoreFishHook(World world)
	{
		super(world);
	}

	/**
	 * Constructor
	 * 
	 * @param 	world	The world the entity should be spawned in.
	 * @param 	owner	The owner of this fish hook entity.
	 */
	public EntityChoreFishHook(World world, AbstractEntity owner)
	{
		super(world);

		angler = owner;
		angler.fishingChore.fishEntity = this;
		setSize(0.25F, 0.25F);
		setLocationAndAngles(angler.posX, (angler.posY + 1.62D) - (double)angler.yOffset, angler.posZ, angler.rotationYaw, angler.rotationPitch);

		posX -= MathHelper.cos((rotationYaw / 180F) * (float)Math.PI) * 0.16F;
		posY -= 0.1D;
		posZ -= MathHelper.sin((rotationYaw / 180F) * (float)Math.PI) * 0.16F;
		yOffset = 0.0F;
		setPosition(posX, posY, posZ);

		float f = 0.4F;
		motionX = -MathHelper.sin((rotationYaw / 180F) * (float)Math.PI) * MathHelper.cos((rotationPitch / 180F) * (float)Math.PI) * f;
		motionZ = MathHelper.cos((rotationYaw / 180F) * (float)Math.PI) * MathHelper.cos((rotationPitch / 180F) * (float)Math.PI) * f;
		motionY = -MathHelper.sin((rotationPitch / 180F) * (float)Math.PI) * f;

		calculateVelocity(motionX, motionY, motionZ, 1.5F, 1.0F);
	}

	/**
	 * Called to update the entity's position/logic.
	 */
	public void onUpdate()
	{
		this.onEntityUpdate();

		if (this.fishPosRotationIncrements > 0)
		{
			double var21 = this.posX + (this.fishX - this.posX) / (double)this.fishPosRotationIncrements;
			double var22 = this.posY + (this.fishY - this.posY) / (double)this.fishPosRotationIncrements;
			double var23 = this.posZ + (this.fishZ - this.posZ) / (double)this.fishPosRotationIncrements;
			double var7 = MathHelper.wrapAngleTo180_double(this.fishYaw - (double)this.rotationYaw);
			this.rotationYaw = (float)((double)this.rotationYaw + var7 / (double)this.fishPosRotationIncrements);
			this.rotationPitch = (float)((double)this.rotationPitch + (this.fishPitch - (double)this.rotationPitch) / (double)this.fishPosRotationIncrements);
			--this.fishPosRotationIncrements;
			this.setPosition(var21, var22, var23);
			this.setRotation(this.rotationYaw, this.rotationPitch);
		}

		else
		{
			if (!this.worldObj.isRemote)
			{
				if (angler != null)
				{
					ItemStack itemStack = this.angler.getHeldItem();

					if (this.angler.isDead || !this.angler.isEntityAlive() || itemStack == null || itemStack.getItem() != Item.fishingRod || this.getDistanceSqToEntity(this.angler) > 1024.0D)
					{
						this.setDead();
						this.angler.fishingChore.fishEntity = null;
						return;
					}
				}

				else
				{
					this.setDead();
				}

				if (this.bobber != null)
				{
					if (!this.bobber.isDead)
					{
						this.posX = this.bobber.posX;
						this.posY = this.bobber.boundingBox.minY + (double)this.bobber.height * 0.8D;
						this.posZ = this.bobber.posZ;
						return;
					}

					this.bobber = null;
				}

				if (this != null)
				{
					if (this.isDead && this.bobber != null)
					{
						this.bobber.setDead();
					}
				}
			}

			if (this.worldObj.isRemote)
			{
				if (this.angler != null)
				{
					if (this.angler.fishingChore != null)
					{
						if (this.angler.fishingChore.fishEntity != this)
						{
							this.setDead();

							if (this.bobber != null)
							{
								this.bobber.setDead();
							}
						}
					}
				}
			}

			if (this.shake > 0)
			{
				--this.shake;
			}

			if (this.inGround)
			{
				int blockId = this.worldObj.getBlockId(this.xTile, this.yTile, this.zTile);

				if (blockId == this.inTile)
				{
					++this.ticksInGround;

					if (this.ticksInGround == 1200)
					{
						this.setDead();
					}

					return;
				}

				this.inGround = false;
				this.motionX *= (double)(this.rand.nextFloat() * 0.2F);
				this.motionY *= (double)(this.rand.nextFloat() * 0.2F);
				this.motionZ *= (double)(this.rand.nextFloat() * 0.2F);
				this.ticksInGround = 0;
				this.ticksInAir = 0;
			}
			else
			{
				++this.ticksInAir;
			}

			Vec3 vector = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX, this.posY, this.posZ);
			Vec3 motionVector = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
			MovingObjectPosition movingObjectPosition = this.worldObj.clip(vector, motionVector);
			vector = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX, this.posY, this.posZ);
			motionVector = this.worldObj.getWorldVec3Pool().getVecFromPool(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

			if (movingObjectPosition != null)
			{
				motionVector = this.worldObj.getWorldVec3Pool().getVecFromPool(movingObjectPosition.hitVec.xCoord, movingObjectPosition.hitVec.yCoord, movingObjectPosition.hitVec.zCoord);
			}

			Entity entity = null;
			List entityList = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
			double zeroDistance = 0.0D;
			double vectorDistance;

			for (int i = 0; i < entityList.size(); ++i)
			{
				Entity entityInList = (Entity)entityList.get(i);

				if (entityInList.canBeCollidedWith() && (entityInList != this.angler || this.ticksInAir >= 5))
				{
					float boundingBoxExpansion = 0.3F;
					AxisAlignedBB boundingBox = entityInList.boundingBox.expand((double)boundingBoxExpansion, (double)boundingBoxExpansion, (double)boundingBoxExpansion);
					MovingObjectPosition interceptPosition = boundingBox.calculateIntercept(vector, motionVector);

					if (interceptPosition != null)
					{
						vectorDistance = vector.distanceTo(interceptPosition.hitVec);

						if (vectorDistance < zeroDistance || zeroDistance == 0.0D)
						{
							entity = entityInList;
							zeroDistance = vectorDistance;
						}
					}
				}
			}

			if (entity != null)
			{
				movingObjectPosition = new MovingObjectPosition(entity);
			}

			if (movingObjectPosition != null)
			{
				if (movingObjectPosition.entityHit != null)
				{
					if (movingObjectPosition.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.angler), 0))
					{
						this.bobber = movingObjectPosition.entityHit;
					}
				}
				else
				{
					this.inGround = true;
				}
			}

			if (!this.inGround)
			{
				this.moveEntity(this.motionX, this.motionY, this.motionZ);
				float motion = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
				this.rotationYaw = (float)(Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);

				for (this.rotationPitch = (float)(Math.atan2(this.motionY, (double)motion) * 180.0D / Math.PI); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F)
				{
					;
				}

				while (this.rotationPitch - this.prevRotationPitch >= 180.0F)
				{
					this.prevRotationPitch += 360.0F;
				}

				while (this.rotationYaw - this.prevRotationYaw < -180.0F)
				{
					this.prevRotationYaw -= 360.0F;
				}

				while (this.rotationYaw - this.prevRotationYaw >= 180.0F)
				{
					this.prevRotationYaw += 360.0F;
				}

				this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
				this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
				float motionModifier = 0.92F;

				if (this.onGround || this.isCollidedHorizontally)
				{
					motionModifier = 0.5F;
				}

				byte endByte = 5;
				double yMotion = 0.0D;

				for (int i = 0; i < endByte; ++i)
				{
					double boundingBoxMinY = this.boundingBox.minY + (this.boundingBox.maxY - this.boundingBox.minY) * (double)(i + 0) / (double)endByte - 0.125D + 0.125D;
					double boundingBoxMaxY = this.boundingBox.minY + (this.boundingBox.maxY - this.boundingBox.minY) * (double)(i + 1) / (double)endByte - 0.125D + 0.125D;
					AxisAlignedBB boundingBox = AxisAlignedBB.getAABBPool().getAABB(this.boundingBox.minX, boundingBoxMinY, this.boundingBox.minZ, this.boundingBox.maxX, boundingBoxMaxY, this.boundingBox.maxZ);

					if (this.worldObj.isAABBInMaterial(boundingBox, Material.water))
					{
						yMotion += 1.0D / (double)endByte;
					}
				}

				if (yMotion > 0.0D)
				{
					if (this.ticksCatchable > 0)
					{
						--this.ticksCatchable;
					}
					else
					{
						short fishCatchChance = 500;

						if (this.worldObj.canLightningStrikeAt(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY) + 1, MathHelper.floor_double(this.posZ)))
						{
							fishCatchChance = 300;
						}

						if (this.rand.nextInt(fishCatchChance) == 0)
						{
							this.ticksCatchable = this.rand.nextInt(30) + 10;
							this.motionY -= 0.20000000298023224D;
							this.playSound("random.splash", 0.25F, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);

							float floorMinY = (float)MathHelper.floor_double(this.boundingBox.minY);
							int i;
							float randomFloat1;
							float randomFloat2;

							for (i = 0; (float)i < 1.0F + this.width * 20.0F; ++i)
							{
								randomFloat1 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
								randomFloat2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
								this.worldObj.spawnParticle("bubble", this.posX + (double)randomFloat1, (double)(floorMinY + 1.0F), this.posZ + (double)randomFloat2, this.motionX, this.motionY - (double)(this.rand.nextFloat() * 0.2F), this.motionZ);
							}

							for (i = 0; (float)i < 1.0F + this.width * 20.0F; ++i)
							{
								randomFloat1 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
								randomFloat2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
								this.worldObj.spawnParticle("splash", this.posX + (double)randomFloat1, (double)(floorMinY + 1.0F), this.posZ + (double)randomFloat2, this.motionX, this.motionY, this.motionZ);
							}
						}
					}
				}

				if (this.ticksCatchable > 0)
				{
					this.motionY -= (double)(this.rand.nextFloat() * this.rand.nextFloat() * this.rand.nextFloat()) * 0.2D;
				}

				vectorDistance = yMotion * 2.0D - 1.0D;
				this.motionY += 0.03999999910593033D * vectorDistance;

				if (yMotion > 0.0D)
				{
					motionModifier = (float)((double)motionModifier * 0.9D);
					this.motionY *= 0.8D;
				}

				this.motionX *= (double)motionModifier;
				this.motionY *= (double)motionModifier;
				this.motionZ *= (double)motionModifier;
				this.setPosition(this.posX, this.posY, this.posZ);
			}
		}

		try
		{
			if (this.angler.fishingChore.hasEnded)
			{
				this.setDead();
			}
		}

		catch (NullPointerException e)
		{
			return;
		}
	}
}