package com.dfsek.terra.bukkit.nms.v1_21_3;

import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldSetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.MethodName;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

import java.util.List;


public class Reflection {
    public static final MappedRegistryProxy MAPPED_REGISTRY;
    public static final StructureManagerProxy STRUCTURE_MANAGER;

    public static final ReferenceProxy REFERENCE;


    public static final ChunkMapProxy CHUNKMAP;
    public static final HolderSetNamedProxy HOLDER_SET;
    public static final BiomeProxy BIOME;

    static {
        ReflectionRemapper reflectionRemapper = ReflectionRemapper.forReobfMappingsInPaperJar();
        ReflectionProxyFactory reflectionProxyFactory = ReflectionProxyFactory.create(reflectionRemapper,
            Reflection.class.getClassLoader());

        MAPPED_REGISTRY = reflectionProxyFactory.reflectionProxy(MappedRegistryProxy.class);
        STRUCTURE_MANAGER = reflectionProxyFactory.reflectionProxy(StructureManagerProxy.class);
        REFERENCE = reflectionProxyFactory.reflectionProxy(ReferenceProxy.class);
        CHUNKMAP = reflectionProxyFactory.reflectionProxy(ChunkMapProxy.class);
        HOLDER_SET = reflectionProxyFactory.reflectionProxy(HolderSetNamedProxy.class);
        BIOME = reflectionProxyFactory.reflectionProxy(BiomeProxy.class);
    }


    @Proxies(MappedRegistry.class)
    public interface MappedRegistryProxy {
        @FieldSetter("frozen")
        void setFrozen(MappedRegistry<?> instance, boolean frozen);
    }


    @Proxies(StructureManager.class)
    public interface StructureManagerProxy {
        @FieldGetter("level")
        LevelAccessor getLevel(StructureManager instance);
    }


    @Proxies(Holder.Reference.class)
    public interface ReferenceProxy {
        @MethodName("bindValue")
        <T> void invokeBindValue(Reference<T> instance, T value);
    }

    @Proxies(ChunkMap.class)
    public interface ChunkMapProxy {
        @FieldGetter("worldGenContext")
        WorldGenContext getWorldGenContext(ChunkMap instance);

        @FieldSetter("worldGenContext")
        void setWorldGenContext(ChunkMap instance, WorldGenContext worldGenContext);
    }

    @Proxies(HolderSet.Named.class)
    public interface HolderSetNamedProxy {
        @MethodName("contents")
        <T> List<Holder<T>> invokeContents(HolderSet.Named<T> instance);
    }

    @Proxies(Biome.class)
    public interface BiomeProxy {
        @MethodName("getGrassColorFromTexture")
        int invokeGrassColorFromTexture(Biome instance);
    }
}
