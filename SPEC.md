# SPEC — Work Item 3: Surface Palettes for Vanilla Biomes

## Scope
Create Terra `PALETTE` configs and per-biome `palette:` Y-level mappings that replicate vanilla Minecraft's surface block layers. Terra's `NOISE_3D` chunk generator bypasses vanilla `buildSurface`, so every biome must explicitly define its surface layers (grass/dirt, sand, snow, etc.) via Terra's palette system.

## Background: Terra Palette System

Terra uses three interlocking config types:

1. **Palette definitions** (`palettes/*.yml`, `type: PALETTE`) — Define block layers (`materials` + `layers` depth).
2. **Biome palette mappings** (`palette:` in biome `.yml`) — List of `PALETTE_ID: Y_LEVEL` entries. Each entry means "use this palette from this Y level down to the next lower entry."
3. **Meta merge keys** (`<< meta.yml:palette-bottom`) — YAML merge anchors that append deepslate/bedrock layers to every biome.

### Example Palette Definition
```yaml
id: GRASS_AND_DIRT
type: PALETTE
layers:
  - materials:
      - minecraft:grass_block: 1
    layers: 1
  - materials:
      - minecraft:dirt: 1
    layers: 3
  - materials:
      - minecraft:stone: 1
    layers: 1
```

### Example Biome Palette Mapping
```yaml
palette:
  - GRASS_AND_DIRT: 319      # from Y=319 down to Y=64
  - SAND: 64                 # from Y=64 down to Y=61
  - << meta.yml:palette-bottom  # deepslate at Y=7, bedrock at Y=-60
```

## Design Goals
1. **Replicate vanilla surface layers** for all 32 implemented biomes (9 oceans + 20 land + 3 cave).
2. **Share common palettes** — Don't duplicate; use reusable palette IDs (`GRASS_AND_DIRT`, `SAND`, `SNOW`, etc.).
3. **Handle ocean biomes** — Set `ocean.level: 63` and `ocean.palette` (water + seafloor) for all aquatic biomes.
4. **Add slant palettes** for steep terrain (mountains, cliffs) where exposed stone/gravel should show.
5. **Fix biome YAML structure** — Move `palette` from incorrectly nested `terrain.palette` to root-level `palette:`.

## Palette Catalog

### Shared Land Palettes
| Palette ID | Layers | Used By |
|---|---|---|
| `GRASS` | grass_block → dirt → stone | PLAINS, FOREST, MEADOW, WINDSWEPT_HILLS |
| `GRASS_PODZOL` | grass_block/podzol mix → dirt → stone | JUNGLE, OLD_GROWTH_SPRUCE_TAIGA |
| `DIRT_COARSE` | grass_block/coarse_dirt → dirt → stone | SAVANNA |
| `SAND` | sand → sandstone → stone | DESERT, BADLANDS (beach layer) |
| `SNOW` | snow_block → dirt → stone | SNOWY_TUNDRA, SNOWY_TAIGA |
| `SNOW_ICE` | snow_block → packed_ice → stone | ICE_SPIKES, FROZEN_PEAKS |
| `STONE` | stone → deepslate | JAGGED_PEAKS, STONY_PEAKS |
| `STONE_CALCITE` | calcite → stone → deepslate | STONY_PEAKS |
| `TERRACOTTA` | terracotta → red_sand → stone | BADLANDS |
| `CLAY` | grass_block → clay → dirt → stone | SWAMP |
| `GRAVEL` | gravel → stone | WINDSWEPT_GRAVELLY_HILLS, ocean floors |
| `SAND_GRAVEL` | sand → gravel → stone | Normal ocean floors |
| `GRAVEL_SAND` | gravel → sand → stone | Deep ocean floors |

### Ocean Palettes
| Palette ID | Layers | Used By |
|---|---|---|
| `OCEAN_WATER` | water (with sand floor below Y=61) | All oceans as `ocean.palette` |

### Cave Palettes
| Palette ID | Layers | Used By |
|---|---|---|
| `DEEPSLATE` | deepslate → bedrock | DEEP_DARK |
| `DRIPSTONE` | dripstone_block → deepslate | DRIPSTONE_CAVES |
| `MOSS_CLAY` | moss_block → clay → deepslate | LUSH_CAVES |

## Ocean Configuration

All ocean biomes need:
```yaml
ocean:
  level: 63
  palette:
    - OCEAN_WATER: 319
```

The `OCEAN_WATER` palette uses a noise sampler to place ice in frozen oceans:
```yaml
id: OCEAN_WATER
type: PALETTE
layers:
  - materials:
      - minecraft:water: 1
      - minecraft:ice: 6   # weighted for frozen oceans
    layers: 1
    sampler:  # noise-driven ice patches for frozen variants
  - materials: minecraft:water
    layers: 1
```

## Biome Palette Mappings

### Aquatic Biomes
| Biome | Palette Mapping |
|---|---|
| `FROZEN_OCEAN` | `FROZEN_OCEAN: 319` (ice/water mix), `SAND_GRAVEL: 61`, bottom |
| `COLD_OCEAN` | `SAND_GRAVEL: 319`, bottom |
| `OCEAN` | `SAND: 319`, bottom |
| `LUKEWARM_OCEAN` | `SAND: 319`, bottom |
| `WARM_OCEAN` | `SAND: 319`, bottom |
| `DEEP_FROZEN_OCEAN` | `GRAVEL: 319`, bottom |
| `DEEP_COLD_OCEAN` | `GRAVEL: 319`, bottom |
| `DEEP_OCEAN` | `GRAVEL: 319`, bottom |
| `DEEP_LUKEWARM_OCEAN` | `GRAVEL: 319`, bottom |

### Land Biomes
| Biome | Palette Mapping |
|---|---|
| `PLAINS` | `GRASS: 319`, `SAND: 64`, bottom |
| `FOREST` | `GRASS: 319`, `SAND: 64`, bottom |
| `BIRCH_FOREST` | `GRASS: 319`, `SAND: 64`, bottom |
| `DARK_FOREST` | `GRASS: 319`, `SAND: 64`, bottom |
| `JUNGLE` | `GRASS_PODZOL: 319`, `SAND: 64`, bottom |
| `SAVANNA` | `DIRT_COARSE: 319`, `SAND: 64`, bottom |
| `DESERT` | `SAND: 319`, bottom |
| `BADLANDS` | `TERRACOTTA: 319`, bottom |
| `SWAMP` | `CLAY: 319`, bottom |
| `SNOWY_TUNDRA` | `SNOW: 319`, bottom |
| `SNOWY_TAIGA` | `SNOW: 319`, bottom |
| `TAIGA` | `GRASS: 319`, `SAND: 64`, bottom |
| `OLD_GROWTH_SPRUCE_TAIGA` | `GRASS_PODZOL: 319`, `SAND: 64`, bottom |
| `ICE_SPIKES` | `SNOW_ICE: 319`, bottom |
| `MEADOW` | `GRASS: 319`, `SAND: 64`, bottom |
| `WINDSWEPT_HILLS` | `GRASS: 319`, `GRAVEL: 64`, bottom |
| `WINDSWEPT_GRAVELLY_HILLS` | `GRAVEL: 319`, bottom |
| `JAGGED_PEAKS` | `STONE: 319`, bottom |
| `STONY_PEAKS` | `STONE_CALCITE: 319`, bottom |
| `FROZEN_PEAKS` | `SNOW_ICE: 319`, bottom |

### Cave Biomes
| Biome | Palette Mapping |
|---|---|
| `DEEP_DARK` | `DEEPSLATE: 319`, bedrock bottom |
| `DRIPSTONE_CAVES` | `DRIPSTONE: 319`, bottom |
| `LUSH_CAVES` | `MOSS_CLAY: 319`, bottom |

## Slant Configuration

Mountain and cliff biomes need slant palettes so steep faces show stone/gravel instead of grass/snow:

```yaml
slant:
  - threshold: 4
    palette:
      - BLOCK:minecraft:stone: 319
      - << meta.yml:palette-bottom
```

Biomes requiring slant:
- `WINDSWEPT_HILLS`, `WINDSWEPT_GRAVELLY_HILLS`, `JAGGED_PEAKS`, `STONY_PEAKS`, `FROZEN_PEAKS`

## Meta.yml Additions

```yaml
strata:
  deepslate:
    top: 7
    bottom: -7
  bedrock:
    top: -60
    bottom: -64

palette-bottom:
  - DEEPSLATE_STRATA: $meta.yml:strata.deepslate.top
  - BEDROCK_STRATA: $meta.yml:strata.bedrock.top
  - BLOCK:minecraft:bedrock: $meta.yml:strata.bedrock.bottom
```

## Files to Create / Modify

| File | Action | Purpose |
|---|---|---|
| `meta.yml` | Modify | Add `strata:` and `palette-bottom:` merge anchors |
| `palettes/land/grass.yml` | Create | Standard grass/dirt/stone |
| `palettes/land/grass_podzol.yml` | Create | Podzol mix for jungles/taigas |
| `palettes/land/dirt_coarse.yml` | Create | Coarse dirt for savanna |
| `palettes/land/sand.yml` | Create | Sand/sandstone/stone |
| `palettes/land/terracotta.yml` | Create | Terracotta/red_sand/stone |
| `palettes/land/snow.yml` | Create | Snow_block/dirt/stone |
| `palettes/land/snow_ice.yml` | Create | Snow/packed_ice/stone |
| `palettes/land/stone.yml` | Create | Stone/deepslate |
| `palettes/land/stone_calcite.yml` | Create | Calcite/stone/deepslate |
| `palettes/land/clay.yml` | Create | Grass/clay/dirt/stone for swamp |
| `palettes/land/gravel.yml` | Create | Gravel/stone |
| `palettes/aquatic/sand_gravel.yml` | Create | Sand/gravel/stone (ocean floor) |
| `palettes/aquatic/gravel_sand.yml` | Create | Gravel/sand/stone (deep ocean) |
| `palettes/aquatic/ocean_water.yml` | Create | Water/ice noise sampler for ocean fill |
| `palettes/cave/deepslate.yml` | Create | Deepslate/bedrock |
| `palettes/cave/dripstone.yml` | Create | Dripstone_block/deepslate |
| `palettes/cave/moss_clay.yml` | Create | Moss_block/clay/deepslate |
| `palettes/strata/deepslate.yml` | Create | Deepslate strata transition |
| `palettes/strata/bedrock.yml` | Create | Bedrock strata transition |
| `biomes/aquatic/*.yml` | Modify | Add `ocean:` and fix `palette` placement |
| `biomes/aquatic/deep_frozen_ocean.yml` | Create | Deep frozen ocean biome |
| `biomes/aquatic/deep_cold_ocean.yml` | Create | Deep cold ocean biome |
| `biomes/aquatic/deep_ocean.yml` | Create | Deep ocean biome |
| `biomes/aquatic/deep_lukewarm_ocean.yml` | Create | Deep lukewarm ocean biome |
| `biomes/land/*.yml` | Modify | Fix `palette` placement, add `slant` where needed |
| `biomes/cave/*.yml` | Modify | Fix `palette` placement |

## Known Limitations

1. **No beach blending** — Terra palettes switch abruptly at Y-level thresholds. There is no "beach" transition palette that blends sand into grass over a few blocks. The `SAND: 64` layer in land biomes approximates a beach below sea level, but above-water beaches will look sharp.

2. **No vanilla surface builders** — Terra's chunk generator calls its own surface placement code, not vanilla's `SurfaceBuilder` classes. Features like podzol patches, mossy stone, or coarse dirt blobs must be approximated via noise samplers in palette layers or deferred to Terra feature configs.

3. **Slant threshold is global** — The `slant.threshold: 4` value applies to all slanted terrain in the biome. Fine-grained control (e.g., gentle slopes keep grass, only cliffs show stone) requires a custom slant palette with multiple thresholds, which is supported but more verbose.

4. **River banks** — Vanilla uses `SurfaceBuilder` to carve river banks and replace exposed dirt with grass. Terra's palette system has no equivalent; river-shaped terrain will show whatever palette is mapped at that Y level.

5. **Palette layer count is static** — Each palette layer has a fixed `layers` count. Unlike vanilla's variable-depth dirt layers (sometimes 1 deep, sometimes 3), Terra always places exactly the configured depth. Variation requires adding a noise sampler to the layer.

## Verification Plan

1. **Visual test**: Load world, verify grass blocks on top of dirt in plains/forest.
2. **Visual test**: Verify sand layers in desert, terracotta in badlands.
3. **Visual test**: Verify snow layers in snowy biomes, ice spikes in ice_spikes biome.
4. **Visual test**: Descend below Y=0, verify deepslate replaces stone at Y=7, bedrock at Y=-60.
5. **Visual test**: Fly over ocean, verify sand/gravel seafloor with water above.
6. **3D test**: Check cave biomes — dripstone blocks in dripstone caves, moss in lush caves.
7. **Slant test**: Find steep cliff in windswept_hills/jagged_peaks, verify stone shows through grass/snow.
