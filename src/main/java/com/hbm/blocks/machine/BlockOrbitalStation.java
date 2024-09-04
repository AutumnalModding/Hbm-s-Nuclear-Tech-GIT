package com.hbm.blocks.machine;

import java.util.List;
import java.util.ArrayList;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.handler.RocketStruct;
import com.hbm.handler.atmosphere.IBlockSealable;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCustomRocket;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityOrbitalStation;
import com.hbm.util.I18nUtil;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockOrbitalStation extends BlockDummyable implements IBlockSealable, ILookOverlay {

	public BlockOrbitalStation(Material mat) {
		super(mat);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityOrbitalStation();
		if(meta >= 6) return new TileEntityProxyCombo(true, false, true);
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {1, 0, 2, 2, 2, 2};
	}

	@Override
	public int getOffset() {
		return 2;
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		int[] pos = this.findCore(world, x, y, z);

		if(pos == null)
			return false;

		// If activating the side blocks, ignore, to allow placing
		if(Math.abs(pos[0] - x) >= 2 || Math.abs(pos[2] - z) >= 2)
			return false;

		if(world.isRemote) {
			return true;
		} else {
			TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);

			if(!(te instanceof TileEntityOrbitalStation))
				return false;

			TileEntityOrbitalStation station = (TileEntityOrbitalStation) te;

			if(station.hasStoredItems()) {
				station.giveStoredItems(player);
			} else if(station.hasDocked) {
				if(player.isSneaking() && player.getHeldItem() == null) {
					station.despawnRocket();
					station.giveStoredItems(player);
				} else {
					station.enterCapsule(player);
				}
			} else {
				ItemStack held = player.getHeldItem();
				if(held != null) {
					if(held.getItem() == ModItems.rocket_custom && ItemCustomRocket.hasFuel(held)) {
						station.spawnRocket(held);
						held.stackSize--;
					}
					if(held.getItem() == ModItems.rp_capsule_20 || held.getItem() == ModItems.rp_pod_20) {
						station.spawnRocket(ItemCustomRocket.build(new RocketStruct(held)));
						held.stackSize--;
					}
				}
			}
			
			return true;
		}
	}

	@Override
	public boolean isSealed(World world, int x, int y, int z) {
		return true;
	}

	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		this.makeExtra(world, x + 2, y + 1, z - 1);
		this.makeExtra(world, x + 2, y + 1, z + 0);
		this.makeExtra(world, x + 2, y + 1, z + 1);
		this.makeExtra(world, x - 2, y + 1, z - 1);
		this.makeExtra(world, x - 2, y + 1, z + 0);
		this.makeExtra(world, x - 2, y + 1, z + 1);
		this.makeExtra(world, x - 1, y + 1, z + 2);
		this.makeExtra(world, x + 0, y + 1, z + 2);
		this.makeExtra(world, x + 1, y + 1, z + 2);
		this.makeExtra(world, x - 1, y + 1, z - 2);
		this.makeExtra(world, x + 0, y + 1, z - 2);
		this.makeExtra(world, x + 1, y + 1, z - 2);
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		int[] pos = this.findCore(world, x, y, z);
		
		if(pos == null)
			return;

		if(Math.abs(pos[0] - x) >= 2 || Math.abs(pos[2] - z) >= 2)
			return;
		
		TileEntity te = world.getTileEntity(pos[0], pos[1], pos[2]);
		
		if(!(te instanceof TileEntityOrbitalStation))
			return;
		
		TileEntityOrbitalStation station = (TileEntityOrbitalStation) te;
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		List<String> text = new ArrayList<String>();

		for(int i = 0; i < station.slots.length; i++) {
			if(station.slots[i] != null) {
				text.add(EnumChatFormatting.RED + "<- " + EnumChatFormatting.RESET + station.slots[i].getDisplayName());
			}
		}

		if(!text.isEmpty()) {
			text.add("Interact to retrieve stored rockets");
		} else {
			if(station.hasDocked) {
				if(player.isSneaking()) {
					if(player.getHeldItem() == null) {
						text.add("Interact to remove docked rocket");
					}
				} else {
					text.add("Interact to enter docked rocket");
				}
			} else if(!player.isSneaking()) {
				ItemStack held = player.getHeldItem();
				if(held != null) {
					if(held.getItem() == ModItems.rocket_custom && ItemCustomRocket.hasFuel(held)) {
						text.add("Interact to place held rocket");
					}
					if(held.getItem() == ModItems.rp_capsule_20 || held.getItem() == ModItems.rp_pod_20) {
						text.add("Interact to place held capsule");
					}
				}
			}
		}

		if(text.isEmpty())
			return;
		
		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text);
	}
	
}
