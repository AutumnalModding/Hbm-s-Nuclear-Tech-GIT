package com.hbm.tileentity.machine;

import com.hbm.dim.CelestialBody;
import com.hbm.dim.SolarSystem;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerStardar;
import com.hbm.inventory.gui.GUIMachineStardar;
import com.hbm.items.ModItems;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class TileEntityMachineStardar extends TileEntityMachineBase implements IGUIProvider, IControlReceiver {

	public String planetName;
	public int planetTier;
	public TileEntityMachineStardar() {
		super(2);
	}

	public int dimID;
	
	public SolarSystem.Body body;

	@Override
	public void updateEntity() {
		if(!worldObj.isRemote) {
			for(CelestialBody rody : CelestialBody.getLandableBodies()) {
				if(slots[1] != null && slots[1].getItem() == ModItems.hard_drive) {
					if(planetName != null && planetName == rody.name) {
						slots[1] = new ItemStack(ModItems.full_drive, 1, rody.getEnum().ordinal());
						return;
					}
				}
			}
		}
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerStardar(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineStardar(player.inventory, this);
	}

	@Override
	public String getName() {
		return "container.machineStardar";
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setString("Pname", planetName);
		nbt.setInteger("tier", planetTier);
		nbt.setInteger("id", dimID);

	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.getString("Pname");
		nbt.getInteger("tier");
		nbt.getInteger("id");

	}
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		if(worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
			return false;
		} else {
			return player.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 128;
		}
	}
	
	@Override
	public void receiveControl(NBTTagCompound data) {
		this.planetName = data.getString("Pname");
		this.planetTier = data.getInteger("tier");
		this.dimID = data.getInteger("id");

		this.markDirty();
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return Vec3.createVectorHelper(xCoord - player.posX, yCoord - player.posY, zCoord - player.posZ).lengthVector() < 20;
	}

}
