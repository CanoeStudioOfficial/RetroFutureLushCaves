package com.canoestudio.retrofuturemc.contents.mobs.axolotl;

import com.canoestudio.retrofuturemc.retrofuturemc.Tags;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class AxolotlEvents {
    @SubscribeEvent
    public static void entities(RegistryEvent.Register<EntityEntry> event) {
        IForgeRegistry<EntityEntry> registry = event.getRegistry();

        registry.register(
                EntityEntryBuilder.create()
                        .entity(EntityAxolotl.class)
                        .egg(0xfbc1e3, 0xa62d74)
                        .tracker(80, 3, true)
                        .id(new ResourceLocation(Tags.MOD_ID, "axolotl"), 1)
                        .name("axolotl")
                        .build()
        );
    }
}
