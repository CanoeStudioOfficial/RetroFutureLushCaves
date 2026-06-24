package com.canoestudio.retrofuturemc.utils.registry;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.contents.items.ModItems;
import com.canoestudio.retrofuturemc.contents.world.biome.ModCaveBiomes;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

import static com.canoestudio.retrofuturemc.contents.blocks.ModBlocks.BLOCKITEMS;
import static com.canoestudio.retrofuturemc.retrofuturemc.Tags.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class ContentRegister {
    private static final BiomeDictionary.Type CAVE = BiomeDictionary.Type.getType("CAVE");
    private static final BiomeDictionary.Type UNDERGROUND = BiomeDictionary.Type.getType("UNDERGROUND");

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (Block block : ModBlocks.BLOCKS) {
            event.getRegistry().register(block);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : ModItems.ITEMS) {
            event.getRegistry().register(item);
        }
        for (Item blockitem : BLOCKITEMS) {
            event.getRegistry().register(blockitem);
        }
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        for (Biome biome : ModCaveBiomes.BIOMES) {
            event.getRegistry().register(biome);
        }

        BiomeDictionary.addTypes(ModCaveBiomes.LUSH_CAVES, CAVE, UNDERGROUND, BiomeDictionary.Type.LUSH, BiomeDictionary.Type.WET, BiomeDictionary.Type.DENSE);
        BiomeDictionary.addTypes(ModCaveBiomes.DRIPSTONE_CAVES, CAVE, UNDERGROUND, BiomeDictionary.Type.DRY, BiomeDictionary.Type.SPARSE);
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels() {
        for (Item item : ModItems.ITEMS) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
        }
        for (Item blockitem : BLOCKITEMS) {
            ModelLoader.setCustomModelResourceLocation(blockitem, 0, new ModelResourceLocation(Objects.requireNonNull(blockitem.getRegistryName()), "inventory"));
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerModels();
    }
}
