package com.canoestudio.retrofuturemc.utils.proxy;

import com.canoestudio.retrofuturemc.contents.world.gen.RetroFutureCaveWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {


    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerWorldGenerator(new RetroFutureCaveWorldGenerator(), 20);

    }

    public void init(FMLInitializationEvent event) {

    }

    public void postInit(FMLPostInitializationEvent event) {

    }
}
