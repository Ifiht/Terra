package com.dfsek.terra.fabric.world.handles.world;

import com.dfsek.terra.api.generic.Entity;
import com.dfsek.terra.api.generic.Tree;
import com.dfsek.terra.api.generic.generator.ChunkGenerator;
import com.dfsek.terra.api.generic.world.Chunk;
import com.dfsek.terra.api.generic.world.World;
import com.dfsek.terra.api.generic.world.block.Block;
import com.dfsek.terra.api.generic.world.vector.Location;
import com.dfsek.terra.fabric.world.block.FabricBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;

import java.io.File;
import java.util.UUID;
import java.util.function.Consumer;

public class FabricWorldAccess implements World {
    private final WorldAccess delegate;

    public FabricWorldAccess(WorldAccess delegate) {
        this.delegate = delegate;
    }

    @Override
    public long getSeed() {
        return ((StructureWorldAccess) delegate).getSeed();
    }

    @Override
    public int getMaxHeight() {
        return delegate.getDimensionHeight();
    }

    @Override
    public ChunkGenerator getGenerator() {
        return null;
    }

    @Override
    public String getName() {
        return delegate.toString();
    }

    @Override
    public UUID getUID() {
        return null;
    }

    @Override
    public boolean isChunkGenerated(int x, int z) {
        return false;
    }

    @Override
    public Chunk getChunkAt(int x, int z) {
        return null;
    }

    @Override
    public File getWorldFolder() {
        return null;
    }

    @Override
    public Block getBlockAt(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return new FabricBlock(pos, delegate);
    }

    @Override
    public Block getBlockAt(Location l) {
        return getBlockAt(l.getBlockX(), l.getBlockY(), l.getBlockZ());
    }

    @Override
    public boolean generateTree(Location l, Tree vanillaTreeType) {
        return false;
    }

    @Override
    public void spawn(Location location, Class<Entity> entity, Consumer<Entity> consumer) {

    }

    @Override
    public WorldAccess getHandle() {
        return delegate;
    }
}
