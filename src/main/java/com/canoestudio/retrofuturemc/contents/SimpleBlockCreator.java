package com.canoestudio.retrofuturemc.contents;

import com.canoestudio.retrofuturemc.contents.blocks.ModBlocks;
import com.canoestudio.retrofuturemc.retrofuturemc.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;

import static com.canoestudio.retrofuturemc.contents.tab.CreativeTab.CREATIVE_TABS;

public class SimpleBlockCreator extends Block {
    public SimpleBlockCreator(String name, Material material, SoundType soundType, float hardness, float resistance, String toolClass, int harvestLevel) {
        super(material, material.getMaterialMapColor());
        setTranslationKey(Tags.MOD_ID + "." + name.toLowerCase());
        setRegistryName(name.toLowerCase());
        setHardness(hardness);
        setResistance(resistance);
        setSoundType(soundType);
        setCreativeTab(CREATIVE_TABS);

        if (toolClass != null) {
            setHarvestLevel(toolClass, harvestLevel);
        }

        ModBlocks.BLOCKS.add(this);
        ModBlocks.BLOCKITEMS.add(new ItemBlock(this).setRegistryName(name.toLowerCase()));
    }
}
