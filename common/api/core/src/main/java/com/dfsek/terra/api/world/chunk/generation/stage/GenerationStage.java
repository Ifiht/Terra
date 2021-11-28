/*
 * Copyright (c) 2020-2021 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.world.chunk.generation.stage;

import com.dfsek.terra.api.world.chunk.Chunk;
import com.dfsek.terra.api.world.access.World;


public interface GenerationStage {
    void populate(World world, Chunk chunk);
}
