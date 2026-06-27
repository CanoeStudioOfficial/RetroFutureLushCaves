package com.canoestudio.retrofuturemc;

import com.canoestudio.retrofuturemc.utils.proxy.CommonProxy;
import com.canoestudio.retrofuturemc.retrofuturemc.Tags;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, dependencies = "required-after:fluidlogged_api@[3.2.0,)")
public class RetroFuturemc {
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @SidedProxy(clientSide = "com.canoestudio.retrofuturemc.utils.proxy.ClientProxy", serverSide = "com.canoestudio.retrofuturemc.utils.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) { proxy.preInit(event); }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) { proxy.init(event); }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) { proxy.postInit(event); }
}
