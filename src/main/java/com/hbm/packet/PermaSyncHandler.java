package com.hbm.packet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbm.handler.ImpactWorldHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionHandler.PollutionData;
import com.hbm.handler.pollution.PollutionHandler.PollutionType;
import com.hbm.main.ModEventHandler;
import com.hbm.potion.HbmPotion;
import com.hbm.saveddata.TomSaveData;
import com.hbm.util.PlanetaryTraitUtil;
import com.hbm.util.PlanetaryTraitWorldSavedData;
import com.hbm.util.PlanetaryTraitUtil.Hospitality;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Utility for permanently synchronizing values every tick with a player in the given context of a world.
 * Uses the Byte Buffer directly instead of NBT to cut back on unnecessary data.
 * @author hbm
 */
public class PermaSyncHandler {
	
	public static HashSet<Integer> boykissers = new HashSet();
	public static float[] pollution = new float[PollutionType.values().length];

	public static void writePacket(ByteBuf buf, World world, EntityPlayerMP player) {
		
		/// TOM IMPACT DATA ///
		TomSaveData data = TomSaveData.forWorld(world);
		
		buf.writeFloat(data.fire);
		buf.writeFloat(data.dust);
		buf.writeBoolean(data.impact);
		buf.writeLong(data.time);
		/// TOM IMPACT DATA ///

		buf.writeFloat(ModEventHandler.flashd);

		buf.writeFloat(ModEventHandler.toy);
		buf.writeFloat(ModEventHandler.altitude);

		buf.writeInt(ModEventHandler.chargetime);

		/// SHITTY MEMES ///
		List<Integer> ids = new ArrayList();
		for(Object o : world.playerEntities) {
			EntityPlayer p = (EntityPlayer) o;
			if(p.isPotionActive(HbmPotion.death.id)) {
				ids.add(p.getEntityId());
			}
		}
		
        /// PLANETARY TRAITS ///
        int dimensionId = player.dimension;
        PlanetaryTraitWorldSavedData traitsData = PlanetaryTraitWorldSavedData.get(world);
        Set<PlanetaryTraitUtil.Hospitality> traits = traitsData.getTraits(dimensionId);

        buf.writeInt(dimensionId);
        buf.writeShort(traits.size());
        for (PlanetaryTraitUtil.Hospitality trait : traits) {
            buf.writeInt(trait.ordinal());
        }
        
        
        /// PLANETARY TRAITS ///
		
		buf.writeShort((short) ids.size());
		for(Integer i : ids) buf.writeInt(i);
		/// SHITTY MEMES ///

		/// POLLUTION ///
		PollutionData pollution = PollutionHandler.getPollutionData(world, (int) Math.floor(player.posX), (int) Math.floor(player.posY), (int) Math.floor(player.posZ));
		if(pollution == null) pollution = new PollutionData();
		for(int i = 0; i < PollutionType.values().length; i++) {
			buf.writeFloat(pollution.pollution[i]);
		}
		/// POLLUTION ///
		
	}
	
	public static void readPacket(ByteBuf buf, World world, EntityPlayer player) {

		/// TOM IMPACT DATA ///
		ImpactWorldHandler.lastSyncWorld = player.worldObj;
		ImpactWorldHandler.fire = buf.readFloat();
		ImpactWorldHandler.dust = buf.readFloat();
		ImpactWorldHandler.impact = buf.readBoolean();
		ImpactWorldHandler.time = buf.readLong();
		/// TOM IMPACT DATA ///

		//WAR//

		
		ImpactWorldHandler.flashd = buf.readFloat();

		ImpactWorldHandler.toy = buf.readFloat();
		ImpactWorldHandler.altitude = buf.readFloat();

		ImpactWorldHandler.chargetime = buf.readInt();

		//WAR//		
	
		
        PlanetaryTraitUtil.lastSyncWorld = player.worldObj;

		//grabs the dimension, then grabs the traits it has along with it.
     
        int dimensionId = buf.readInt();
        int traitCount = buf.readShort();

        PlanetaryTraitWorldSavedData traitsData = PlanetaryTraitWorldSavedData.get(world);
        Set<PlanetaryTraitUtil.Hospitality> traits = traitsData.getTraits(dimensionId);
        //then it writes all the traits in both the static class and the save file into one file.
        //obviously once thats done the game then reads the traits from the file
        for (int i = 0; i < traitCount; i++) {
            int traitOrdinal = buf.readInt();
            PlanetaryTraitUtil.Hospitality trait = PlanetaryTraitUtil.Hospitality.values()[traitOrdinal];
            traits.add(trait);
        }

        //i genuinley dont know what the fuck this is for, it must have been when i was hammering the class down trying to get it to work on servers
        // i will leave it in because im not going through that shit again.
        // Convert the set to an NBTTagCompound
        NBTTagCompound tag = new NBTTagCompound();
        for (PlanetaryTraitUtil.Hospitality trait : traits) {
            if (traits.contains(trait)) {
                tag.setBoolean(trait.name(), true);
            }
        }

        // Assign it to the static field for client-side access
        PlanetaryTraitUtil.tag = tag;
        
		/// SHITTY MEMES ///
		boykissers.clear();
		int ids = buf.readShort();
		for(int i = 0; i < ids; i++) boykissers.add(buf.readInt());
		/// SHITTY MEMES ///

		/// POLLUTION ///
		for(int i = 0; i < PollutionType.values().length; i++) {
			pollution[i] = buf.readFloat();
		}
		/// POLLUTION ///
	}
}
