package com.bioxx.tfc2.handlers.client;

import java.awt.Color;
import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.chunk.Chunk;

import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import org.lwjgl.opengl.GL11;

import com.bioxx.jmapgen.IslandMap;
import com.bioxx.jmapgen.IslandParameters.Feature;
import com.bioxx.jmapgen.Point;
import com.bioxx.jmapgen.attributes.Attribute;
import com.bioxx.jmapgen.attributes.CaveAttribute;
import com.bioxx.jmapgen.attributes.OreAttribute;
import com.bioxx.jmapgen.attributes.RiverAttribute;
import com.bioxx.jmapgen.graph.Center;
import com.bioxx.jmapgen.processing.CaveAttrNode;
import com.bioxx.jmapgen.processing.OreAttrNode;
import com.bioxx.tfc2.Core;
import com.bioxx.tfc2.Reference;
import com.bioxx.tfc2.api.types.Moisture;
import com.bioxx.tfc2.core.FoodStatsTFC;
import com.bioxx.tfc2.core.PlayerInfo;
import com.bioxx.tfc2.core.PlayerManagerTFC;
import com.bioxx.tfc2.core.Timekeeper;
import com.bioxx.tfc2.world.WeatherManager;
import com.bioxx.tfc2.world.WorldGen;

public class RenderOverlayHandler
{
	public static ResourceLocation tfcicons = new ResourceLocation(Reference.ModID, Reference.AssetPathGui + "icons.png");
	private FontRenderer fontrenderer = null;

	public int recordTimer;
	private final Field _recordPlayingUpFor = ReflectionHelper.findField(GuiIngame.class, "recordPlayingUpFor", "field_73845_h");
	private final Field _recordPlaying = ReflectionHelper.findField(GuiIngame.class, "recordPlaying", "field_73838_g");

	@SubscribeEvent
	public void render(RenderGameOverlayEvent.Pre event)
	{
		GuiIngameForge.renderFood = false;

		// We check for crosshairs just because it's always drawn and is before air bar
		if(event.getType() != ElementType.CROSSHAIRS)
			return;

		// This is for air to be drawn above our bars
		GuiIngameForge.right_height += 10;

		ScaledResolution sr = event.getResolution();
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer player = mc.thePlayer;
		fontrenderer = mc.fontRendererObj;

		int healthRowHeight = sr.getScaledHeight() - 40;
		int armorRowHeight = healthRowHeight - 10;
		int mid = sr.getScaledWidth() / 2;

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Core.bindTexture(tfcicons);

		PlayerInfo playerclient = PlayerManagerTFC.getInstance().getClientPlayer();
		if(mc.playerController.gameIsSurvivalOrAdventure())
		{
			//Draw Health
			this.drawTexturedModalRect(mid-91, healthRowHeight, 0, 0, 90, 10);
			float maxHealth = player.getMaxHealth();
			float percentHealth = Math.min(player.getHealth() / maxHealth, 1.0f);
			this.drawTexturedModalRect(mid-91, healthRowHeight, 0, 10, (int) (90*percentHealth), 10);

			//Draw Food and Water
			FoodStatsTFC foodstats = Core.getPlayerFoodStats(player);
			float foodLevel = foodstats.getFoodLevel();
			//float preFoodLevel = foodstats.getPrevFoodLevel();

			float waterLevel = foodstats.waterLevel;

			float percentFood = Math.min(foodLevel / foodstats.getMaxStomach(player), 1);
			float percentWater = Math.min(waterLevel / foodstats.getMaxWater(player), 1);

			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.drawTexturedModalRect(mid+1, healthRowHeight, 0, 20, 90, 5);
			//Removed during port
			/*if(playerclient != null && playerclient.guishowFoodRestoreAmount)
			{
				float percentFood2 = Math.min(percentFood + playerclient.guiFoodRestoreAmount / foodstats.getMaxStomach(player), 1);
				GL11.glColor4f(0.0F, 0.6F, 0.0F, 0.3F);
				this.drawTexturedModalRect(mid+1, healthRowHeight, 0, 25, (int) (90*(percentFood2)), 5);
			}*/
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

			this.drawTexturedModalRect(mid+1, healthRowHeight, 0, 25, (int) (90*percentFood), 5);

			this.drawTexturedModalRect(mid+1, healthRowHeight+5, 90, 20, 90, 5);
			this.drawTexturedModalRect(mid+1, healthRowHeight+5, 90, 25, (int) (90*percentWater), 5);

			//Draw Notifications
			String healthString = (int) Math.min(player.getHealth(), maxHealth) + "/" + (int) maxHealth;
			fontrenderer.drawString(healthString, mid-45-(fontrenderer.getStringWidth(healthString)/2), healthRowHeight+2, Color.white.getRGB());
			//Removed during port
			//if (player.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getModifier(TFCAttributes.OVERBURDENED_UUID) != null)
			//	mc.fontrenderer.drawString(TFC_Core.translate("gui.overburdened"), mid-(mc.fontrenderer.getStringWidth(TFC_Core.translate("gui.overburdened"))/2), healthRowHeight-20, Color.red.getRGB());

			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			Core.bindTexture(new ResourceLocation("minecraft:textures/gui/icons.png"));

			//Draw experience bar when not riding anything, riding a non-living entity such as a boat/minecart, or riding a pig.
			if (!(player.getRidingEntity() instanceof EntityLiving))
			{
				int cap = 0;
				cap = player.xpBarCap();
				int left = mid - 91;

				if (cap > 0)
				{
					short barWidth = 182;
					int filled = (int) (player.experience * (barWidth + 1));
					int top = sr.getScaledHeight() - 29;
					drawTexturedModalRect(left, top, 0, 64, barWidth, 5);
					if (filled > 0)
						drawTexturedModalRect(left, top, 0, 69, filled, 5);
				}

				if (player.experienceLevel > 0)
				{
					boolean flag1 = false;
					int color = flag1 ? 16777215 : 8453920;
					String text = Integer.toString(player.experienceLevel);
					int x = (sr.getScaledWidth() - fontrenderer.getStringWidth(text)) / 2;
					int y = sr.getScaledHeight() - 30;
					fontrenderer.drawString(text, x + 1, y, 0);
					fontrenderer.drawString(text, x - 1, y, 0);
					fontrenderer.drawString(text, x, y + 1, 0);
					fontrenderer.drawString(text, x, y - 1, 0);
					fontrenderer.drawString(text, x, y, color);
				}

				// We have to reset the color back to white
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			}

			// Don't show the dismount message if it was triggered by the "mounting" from opening a horse inventory.
			/*if (mc.currentScreen instanceof GuiScreenHorseInventoryTFC) //Removed during port
			{
				recordTimer = 0;
				try
				{
					_recordPlayingUpFor.setInt(mc.ingameGUI, 0);
				} catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}*/

			// Draw mount's health bar
			if (player.getRidingEntity() instanceof EntityLivingBase)
			{
				GuiIngameForge.renderHealthMount = false;
				Core.bindTexture(tfcicons);
				EntityLivingBase mount = ((EntityLivingBase) player.getRidingEntity());
				this.drawTexturedModalRect(mid+1, armorRowHeight, 90, 0, 90, 10);
				double mountMaxHealth = mount.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue();
				double mountCurrentHealth = mount.getHealth();
				float mountPercentHealth = (float)Math.min(mountCurrentHealth/mountMaxHealth, 1.0f);
				this.drawTexturedModalRect(mid+1, armorRowHeight, 90, 10, (int) (90*mountPercentHealth), 10);

				String mountHealthString = (int) Math.min(mountCurrentHealth, mountMaxHealth) + "/" + (int) mountMaxHealth;
				fontrenderer.drawString(mountHealthString, mid + 47 - (fontrenderer.getStringWidth(mountHealthString) / 2), armorRowHeight + 2, Color.white.getRGB());
				//renderDismountOverlay(mc, mid, sr.getScaledHeight(), event.partialTicks);//Removed during port
			}

			Core.bindTexture(new ResourceLocation("minecraft:textures/gui/icons.png"));
		}
	}

	@SubscribeEvent
	public void renderText(RenderGameOverlayEvent.Text event)
	{
		Minecraft mc = Minecraft.getMinecraft();
		if(mc.theWorld.provider.getDimension() == 0 && WorldGen.getInstance() != null)
		{
			int xM = ((int)(mc.thePlayer.posX) >> 12);
			int zM = ((int)(mc.thePlayer.posZ) >> 12);
			IslandMap map = WorldGen.getInstance().getIslandMap(xM, zM);
			Point islandCoord = new Point((int)(mc.thePlayer.posX), (int)(mc.thePlayer.posZ)).toIslandCoord();
			BlockPos pos = new BlockPos((int)(mc.thePlayer.posX), 0, (int)(mc.thePlayer.posZ));
			Center hex = map.getClosestCenter(islandCoord);
			event.getLeft().add(""+mc.theWorld.getWorldTime());
			event.getLeft().add("Rain: "+WeatherManager.getInstance().getPreciptitation((int)mc.thePlayer.posX, (int)mc.thePlayer.posZ) +
					" / "  + " / " + mc.theWorld.isRaining());
			event.getLeft().add("Temp: " + WeatherManager.getInstance().getTemperature((int)mc.thePlayer.posX, (int)mc.thePlayer.posY, (int)mc.thePlayer.posZ)+"C");
			event.getLeft().add("Date: " + Timekeeper.getInstance().getSeasonalPeriod() + " | Time: " + Timekeeper.getInstance().getClockTime());
			event.getLeft().add(TextFormatting.BOLD+""+TextFormatting.YELLOW+"--------Hex--------");
			event.getLeft().add("Index: "+hex.index);
			event.getLeft().add("Elevation: "+hex.getElevation()+" ("+map.convertHeightToMC(hex.getElevation())+")");
			Chunk c = mc.theWorld.getChunkFromBlockCoords(pos);
			int b = mc.theWorld.getChunkFromBlockCoords(pos).getBiomeArray()[(pos.getZ() & 0xF) << 4 | (pos.getX() & 0xF)] & 0xFF;
			event.getLeft().add("Moisture: "+Moisture.fromVal(hex.getMoistureRaw()) + " | " + hex.getMoistureRaw() + " | " + b + " | " + (float)b / 255F);
			event.getLeft().add("Island Coord: "+islandCoord.getX() + "," + islandCoord.getY());	
			if(hex.hasAttribute(Attribute.Lake))
				event.getLeft().add("IsLake");	
			RiverAttribute attrib = (RiverAttribute)hex.getAttribute(Attribute.River);
			if(attrib != null)
			{
				event.getLeft().add(TextFormatting.BOLD+""+TextFormatting.YELLOW+"-------River-------");
				event.getLeft().add("River: " + attrib.getRiver() + " | " + (attrib.upriver != null ?  attrib.upriver.size() : 0));	
				if(attrib.upriver != null && attrib.getDownRiver() != null)
					event.getLeft().add("Up :" + hex.getDirection(attrib.upriver.get(0)).toString() + " | Dn :" + hex.getDirection(attrib.getDownRiver()).toString());
			}

			CaveAttribute cattrib = (CaveAttribute)hex.getAttribute(Attribute.Cave);
			if(cattrib != null)
			{
				if(cattrib.nodes.size() > 0)
				{
					event.getLeft().add(TextFormatting.BOLD+""+TextFormatting.YELLOW+"-------Cave-------");
					event.getLeft().add("Cave: "+cattrib.nodes.size());	
					for(CaveAttrNode n : cattrib.nodes)
					{
						//event.getLeft().add("  *"+n.getOffset());	
					}
				}
			}

			OreAttribute oattrib = (OreAttribute)hex.getAttribute(Attribute.Ore);
			if(oattrib != null)
			{
				if(oattrib.nodes.size() > 0)
				{
					event.getLeft().add(TextFormatting.BOLD+""+TextFormatting.YELLOW+"-------Ore-------");
					for(OreAttrNode n : oattrib.nodes)
					{
						event.getLeft().add(n.getOreType());	
					}
				}
			}

			event.getRight().add(TextFormatting.BOLD+""+TextFormatting.YELLOW+"--Island Parmaters--");
			event.getRight().add("*Moisture: "+map.getParams().getIslandMoisture());
			event.getRight().add("*Temperature: "+map.getParams().getIslandTemp());
			event.getRight().add("*Seed: "+map.seed);

			event.getRight().add(TextFormatting.BOLD+""+TextFormatting.YELLOW+"---Island Features--");
			for(Feature f : Feature.values())
			{
				if(map.getParams().hasFeature(f))
				{
					event.getRight().add("*"+f.toString());
				}
			}
		}
	}

	public void drawTexturedModalRect(float xCoord, float yCoord, int minU, int minV, int maxU, int maxV)
	{
		float f = 0.00390625F;
		float f1 = 0.00390625F;
		Tessellator tessellator = Tessellator.getInstance();
		VertexBuffer vb = tessellator.getBuffer();
		vb.begin(7, DefaultVertexFormats.POSITION_TEX);
		vb.pos(xCoord + 0.0F, yCoord + maxV, 0).tex((minU + 0) * f, (minV + maxV) * f1).endVertex();
		vb.pos(xCoord + maxU, yCoord + maxV, 0).tex((minU + maxU) * f, (minV + maxV) * f1).endVertex();
		vb.pos(xCoord + maxU, yCoord + 0.0F, 0).tex((minU + maxU) * f, (minV + 0) * f1).endVertex();
		vb.pos(xCoord + 0.0F, yCoord + 0.0F, 0).tex((minU + 0) * f, (minV + 0) * f1).endVertex();
		tessellator.draw();
	}
}
