/*
 * Copyright (c) 2020-2021 Polyhedral Development
 *
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the common/api directory.
 */

package com.dfsek.terra.api.structure.buffer;

import com.dfsek.terra.api.util.vector.Vector3;
import com.dfsek.terra.api.world.access.World;

import org.jetbrains.annotations.ApiStatus.Experimental;


@Experimental
public interface BufferedItem {
    void paste(Vector3 origin, World world);
}
