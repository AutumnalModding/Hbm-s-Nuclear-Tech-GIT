package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.MissilePronter;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRocket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderLaunchPadRocket extends TileEntitySpecialRenderer {

	@Override
	public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float f) {
		GL11.glPushMatrix();
		{

			GL11.glTranslated(x + 0.5D, y, z + 0.5D);
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_CULL_FACE);

			switch(tileEntity.getBlockMetadata() - BlockDummyable.offset) {
			case 2: GL11.glRotatef(90, 0F, 1F, 0F); break;
			case 4: GL11.glRotatef(180, 0F, 1F, 0F); break;
			case 3: GL11.glRotatef(270, 0F, 1F, 0F); break;
			case 5: GL11.glRotatef(0, 0F, 1F, 0F); break;
			}

			TileEntityLaunchPadRocket pad = (TileEntityLaunchPadRocket) tileEntity;

			bindTexture(ResourceManager.rocket_pad_tex);
			ResourceManager.rocket_pad.renderPart("Base");

			if(pad.canSeeSky && pad.height >= 8) {
				GL11.glPushMatrix();
				{

					bindTexture(ResourceManager.rocket_pad_support_tex);
					ResourceManager.rocket_pad.renderPart("Tower_Base");
	
					for(int oy = 8; oy < pad.height - 2; oy += 3) {
						ResourceManager.rocket_pad.renderPart("Tower_Segment");
						GL11.glTranslatef(0, 3, 0);
					}

					GL11.glTranslatef(0, -3, 0);

					for(int oy = 0; oy < (pad.height - 2) % 3; oy++) {
						ResourceManager.rocket_pad.renderPart("Tower_Segment_Small");
						GL11.glTranslatef(0, 1, 0);
					}

					GL11.glTranslatef(0, -1, 0);
					ResourceManager.rocket_pad.renderPart("Tower_Cap");

				}
				GL11.glPopMatrix();
			}

			if(pad.rocket != null) {
				GL11.glTranslatef(0.0F, 3.0F, 0.0F);
				MissilePronter.prontRocket(pad.rocket, Minecraft.getMinecraft().getTextureManager());
			}

		}
		GL11.glPopMatrix();
	}
	
}
