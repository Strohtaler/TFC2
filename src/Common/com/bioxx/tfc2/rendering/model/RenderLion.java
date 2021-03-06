package com.bioxx.tfc2.rendering.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import com.bioxx.tfc2.Reference;
import com.bioxx.tfc2.api.types.Gender;
import com.bioxx.tfc2.entity.EntityLion;

public class RenderLion extends RenderLiving<EntityLion>
{
	static ResourceLocation texLion = new ResourceLocation(Reference.ModID+":"+"textures/mob/lion.png");
	static ResourceLocation texLioness = new ResourceLocation(Reference.ModID+":"+"textures/mob/lioness.png");

	public RenderLion(RenderManager renderManager) 
	{
		super(renderManager, new ModelLion(), 1.0f);
		this.shadowSize = 0.8f;
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityLion entity) 
	{
		if(entity.getGender() == Gender.Male)
			return texLion;
		return texLioness;
	}
}
