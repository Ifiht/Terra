# SPEC — Work Item 1: Multi-Noise Approximated Biome Provider

## Scope
Build a **pack-level** biome provider that approximates vanilla Minecraft's multi-noise biome placement using Terra's existing `biome-provider-pipeline-v2` and `biome-provider-extrusion` addons. No new Java provider is written; we compose the approximation entirely from YAML configs, Expression-based noise samplers, and pipeline/extrusion stages.

## Design Goals
1. **Approximate**, not replicate, vanilla multi-noise. Terra's noise library (`OpenSimplex2`, `Cellular`, etc.) will not match vanilla `shifted_noise` / `flat_cache` exactly, but will produce similar continental/temperature/humidity banding.
2. **Five tunable noise axes**: `continentalness`, `temperature`, `humidity` (vegetation), `erosion`, `weirdness` (ridges). Each axis has a `floor` and `ceiling` meta-parameter so a single config change can force an entire world to be, e.g., all-cold, all-desert, or all-tropical-rainforest.
3. **Land/sea ratio** is tunable via a `land_threshold` parameter that shifts the continentalness cutoff between ocean and land biomes.
4. **Vanilla biome ID mapping**: Every biome definition references its vanilla counterpart (`vanilla: minecraft:plains`) so that when NMS `applyBiomeDecoration` runs, vanilla sees the correct biome and places the correct structures, ores, and vegetation.
5. **Terrain sampler linkage**: Each biome definition also binds to a Terra terrain sampler (defined in Work Item 3) so that shape (height, caves, surface) is decoupled from biome identity.
6. **3D biome support**: Cave biomes (`deep_dark`, `dripstone_caves`, `lush_caves`) are handled natively via the `EXTRUSION` provider layered on top of the 2D surface pipeline.

## Architecture

### 1. Top-Level Provider: EXTRUSION

The `EXTRUSION` provider is already registered by the `biome-provider-extrusion` addon in the current codebase. It wraps a 2D base provider and applies Y-level-dependent extrusion layers.

```yaml
# biome-providers/extrusion.yml
id: VANILLA_3D
type: BIOME_PROVIDER
provider: EXTRUSION
base: VANILLA_PIPELINE  # 2D surface pipeline
extrusions:
  # Deep Dark: spawns below Y=0 in areas with low cave_noise
  - type: REPLACE
    from: "*"
    to: DEEP_DARK
    noise: deep_dark_noise
    min-y: -64
    max-y: 0

  # Dripstone Caves
  - type: REPLACE
    from: "*"
    to: DRIPSTONE_CAVES
    noise: dripstone_cave_noise
    min-y: -64
    max-y: 60

  # Lush Caves
  - type: REPLACE
    from: "*"
    to: LUSH_CAVES
    noise: lush_cave_noise
    min-y: -64
    max-y: 60
```

### 2. Base Provider: PIPELINE (2D Surface Biomes)

The `PIPELINE` provider is registered by `biome-provider-pipeline-v2`. It resolves surface biomes through a sequence of stages.

```yaml
# biome-providers/pipeline.yml
id: VANILLA_PIPELINE
type: BIOME_PROVIDER
provider: PIPELINE
resolution: 4

pipeline:
  source:
    type: SAMPLER
    sampler: continental
    biomes:
      - OCEAN: ${meta:land_threshold}
      - LAND: ${meta:land_threshold}
  stages:
    # (see Stage Configs below)
```

### 3. Global Tunable Parameters (`meta.yml`)

Pack-scoped values consumed by the Expression samplers. Changing these reconfigures the world without editing individual biome or stage files.

```yaml
meta:
  # Continentalness axis (-1.0 = deep ocean, +1.0 = inland/mountains)
  continental_floor: -1.0
  continental_ceiling: 1.0
  land_threshold: 0.0       # Above = land; below = ocean

  # Temperature axis (-1.0 = frozen, +1.0 = hot)
  temperature_floor: -1.0
  temperature_ceiling: 1.0

  # Humidity / vegetation axis (-1.0 = arid, +1.0 = humid)
  humidity_floor: -1.0
  humidity_ceiling: 1.0

  # Erosion axis (-1.0 = flat/eroded, +1.0 = rugged/un-eroded)
  erosion_floor: -1.0
  erosion_ceiling: 1.0

  # Weirdness / ridges axis (-1.0 = valleys, +1.0 = peaks)
  weirdness_floor: -1.0
  weirdness_ceiling: 1.0

  # World-type shortcuts (commented examples)
  # Setting temp_floor = temp_ceiling = 0.8  → all-hot (desert/savanna/jungle)
  # Setting continental_floor = continental_ceiling = -0.5 → all-ocean
```

### 4. Noise Samplers (`noise/biome-samplers.yml`)

Five `EXPRESSION` samplers using Terra's built-in `open_simplex_2` (registered by `NoiseAddon`). Each sampler clamps to the global meta floor/ceiling.

```yaml
noise:
  continental:
    type: EXPRESSION
    variables:
      raw: "open_simplex_2(x/2048, z/2048)"
    expression: "clamp(raw, ${meta:continental_floor}, ${meta:continental_ceiling})"

  temperature:
    type: EXPRESSION
    variables:
      raw: "open_simplex_2(x/1024 + 1000, z/1024)"
    expression: "clamp(raw, ${meta:temperature_floor}, ${meta:temperature_ceiling})"

  humidity:
    type: EXPRESSION
    variables:
      raw: "open_simplex_2(x/1024, z/1024 + 2000)"
    expression: "clamp(raw, ${meta:humidity_floor}, ${meta:humidity_ceiling})"

  erosion:
    type: EXPRESSION
    variables:
      raw: "open_simplex_2(x/512 + 3000, z/512)"
    expression: "clamp(raw, ${meta:erosion_floor}, ${meta:erosion_ceiling})"

  weirdness:
    type: EXPRESSION
    variables:
      raw: "open_simplex_2(x/512, z/512 + 4000)"
    expression: "clamp(raw, ${meta:weirdness_floor}, ${meta:weirdness_ceiling})"

  # Cave noises (3D, used by EXTRUSION provider)
  deep_dark_noise:
    type: EXPRESSION
    variables:
      raw: "open_simplex_2(x/256, y/64, z/256 + 5000)"
    expression: "raw > 0.6 ? 1 : 0"

  dripstone_cave_noise:
    type: EXPRESSION
    variables:
      raw: "open_simplex_2(x/256, y/64, z/256 + 6000)"
    expression: "raw > 0.5 ? 1 : 0"

  lush_cave_noise:
    type: EXPRESSION
    variables:
      raw: "open_simplex_2(x/256, y/64, z/256 + 7000)"
    expression: "raw > 0.5 ? 1 : 0"
```

> **Note**: `open_simplex_2` is already registered by `NoiseAddon.java`. Scale factors (`/2048`, `/1024`, `/512`) are tuned to approximate vanilla continent size, climate patch size, and local feature size. The 3D cave samplers use `y` for vertical variation.

### 5. Pipeline Stage Sequence (Surface Biomes)

| Stage | From | To | Noise | Description |
|---|---|---|---|---|
| 1 | `OCEAN` | 5 ocean biomes by temp band | `temperature` | `FROZEN_OCEAN` → `WARM_OCEAN` |
| 2 | `LAND` | 5 temp-band placeholders | `temperature` | `LAND_FROZEN` → `LAND_HOT` |
| 3 | `LAND_FROZEN` | 2 humidity variants | `humidity` | `SNOWY_TUNDRA`, `ICE_SPIKES` |
| 4 | `LAND_COLD` | 3 humidity variants | `humidity` | `SNOWY_TAIGA`, `TAIGA`, `OLD_GROWTH_SPRUCE_TAIGA` |
| 5 | `LAND_TEMPERATE` | 5 humidity variants | `humidity` | `PLAINS`, `FOREST`, `BIRCH_FOREST`, `DARK_FOREST`, `SWAMP` |
| 6 | `LAND_WARM` | 4 humidity variants | `humidity` | `SAVANNA`, `PLAINS`, `FOREST`, `JUNGLE` |
| 7 | `LAND_HOT` | 4 humidity variants | `humidity` | `DESERT`, `SAVANNA`, `BADLANDS`, `JUNGLE` |
| 8 | All land | Erosion-based terrain form | `erosion` | Flat → hilly within each biome |
| 9 | High erosion land | Weirdness-based peak variants | `weirdness` | `MEADOW` → `JAGGED_PEAKS` / `STONY_PEAKS` |

> **Implementation detail**: Each stage uses `REPLACE` (not `REPLACE_LIST`) with a threshold sampler. `REPLACE` evaluates a `ProbabilityCollection<PipelineBiome>` against a single scalar sampler value. We pre-quantize each axis into bands by using `EXPRESSION` threshold samplers that return a scalar matching the ProbabilityCollection weights.

### 6. Stage Configs (YAML)

Each stage is a separate file in `biome-providers/stages/` for maintainability.

```yaml
# biome-providers/stages/01-temperature-oceans.yml
type: REPLACE
from: OCEAN
to:
  FROZEN_OCEAN: 0.15
  COLD_OCEAN: 0.25
  OCEAN: 0.35
  LUKEWARM_OCEAN: 0.45
  WARM_OCEAN: 0.55
noise:
  type: EXPRESSION
  expression: "clamp(temperature, -1.0, 1.0)"
```

```yaml
# biome-providers/stages/02-temperature-land.yml
type: REPLACE
from: LAND
to:
  LAND_FROZEN: -0.45
  LAND_COLD: -0.15
  LAND_TEMPERATE: 0.15
  LAND_WARM: 0.45
  LAND_HOT: 0.75
noise:
  type: EXPRESSION
  expression: "clamp(temperature, -1.0, 1.0)"
```

```yaml
# biome-providers/stages/08-erosion-landforms.yml
type: REPLACE
from: PLAINS
to:
  WINDSWEPT_HILLS: 0.3
  WINDSWEPT_GRAVELLY_HILLS: 0.5
  MEADOW: 0.7
noise:
  type: EXPRESSION
  expression: "clamp(erosion, -1.0, 1.0)"
```

```yaml
# biome-providers/stages/09-weirdness-peaks.yml
type: REPLACE
from: MEADOW
to:
  FROZEN_PEAKS: -0.3
  JAGGED_PEAKS: 0.0
  STONY_PEAKS: 0.3
noise:
  type: EXPRESSION
  expression: "clamp(weirdness, -1.0, 1.0)"
```

### 7. Biome Definitions (`biomes/*.yml`)

Each file defines one vanilla-mapped biome. The `vanilla` key is critical for NMS decoration passthrough.

```yaml
id: PLAINS
vanilla: minecraft:plains

color:
  fog: 12638463
  water: 4159204
  water-fog: 329011
  sky: 7907327

# Terra terrain linkage — references a sampler from Work Item 3
terrain:
  sampler: terrain/overworld/land/flat
  palette: palettes/grass_and_dirt
```

A complete pack needs ~70 surface biome definitions (oceans, rivers, all land variants, hills, peaks) plus ~10 cave/underground biomes handled by extrusion.

### 8. Cave Biomes via EXTRUSION

Cave biomes are **not** resolved by the 2D pipeline. The `EXTRUSION` provider receives the surface biome from the `PIPELINE` base, then checks each extrusion layer in order. The first matching layer wins.

The extrusion config uses `min-y` / `max-y` for vertical bounds and a `noise` sampler for horizontal distribution. The `REPLACE` type accepts `from: "*"` meaning "match any surface biome."

This is a true 3D biome system: `getBiome(x, y, z, seed)` samples the base pipeline at (x, z), then queries the extrusion layers for that Y level.

## Files to Create / Modify in `packs/BASE/`

| File | Action | Purpose |
|---|---|---|
| `meta.yml` | Create | Global tunable parameters (floors, ceilings, land_threshold) |
| `noise/biome-samplers.yml` | Create | 5 surface noise samplers + 3 cave noise samplers |
| `biome-providers/extrusion.yml` | Create | Top-level `EXTRUSION` provider wrapping the pipeline |
| `biome-providers/pipeline.yml` | Create | 2D `PIPELINE` provider for surface biomes |
| `biome-providers/sources/continental-source.yml` | Create | `SAMPLER` source: continental → `OCEAN`/`LAND` |
| `biome-providers/stages/01-temperature-oceans.yml` | Create | Ocean stratification by temperature |
| `biome-providers/stages/02-temperature-land.yml` | Create | Land stratification into 5 temp bands |
| `biome-providers/stages/03-humidity-frozen.yml` | Create | Frozen land humidity variants |
| `biome-providers/stages/04-humidity-cold.yml` | Create | Cold land humidity variants |
| `biome-providers/stages/05-humidity-temperate.yml` | Create | Temperate land humidity variants |
| `biome-providers/stages/06-humidity-warm.yml` | Create | Warm land humidity variants |
| `biome-providers/stages/07-humidity-hot.yml` | Create | Hot land humidity variants |
| `biome-providers/stages/08-erosion-landforms.yml` | Create | Flat → hilly terrain within biomes |
| `biome-providers/stages/09-weirdness-peaks.yml` | Create | Valley → peak variants in high terrain |
| `biomes/aquatic/*.yml` | Create (~12) | Oceans, deep oceans, rivers, frozen variants |
| `biomes/land/*.yml` | Create (~60) | All surface biomes including hills/mountain variants |
| `biomes/cave/*.yml` | Create (~10) | Deep Dark, Dripstone Caves, Lush Caves (referenced by extrusion) |
| `pack.yml` | Modify | Set `biomes` key to reference `VANILLA_3D` |

## Assumptions / Risks

1. **REPLACE stage threshold syntax**: The existing `ReplaceStage` uses `ProbabilityCollection<PipelineBiome>` driven by a scalar sampler value. The syntax `FROZEN_OCEAN: 0.15` means "if sampler value ≤ 0.15, return FROZEN_OCEAN". ProbabilityCollection cumulative ordering determines band boundaries. This is functionally equivalent to threshold ranges but expressed as scalar cutoffs.

2. **EXTRUSION provider availability**: The `biome-provider-extrusion` addon is present in `common/addons/` and registered in the current codebase. It requires the base provider to implement `getBaseBiome()` (returning a non-empty Optional). `PipelineBiomeProvider` does this. Confirmed working.

3. **Performance**: 9 pipeline stages + 3 extrusion layers evaluated per biome sample. Resolution 4 (one decision per 4×4 column) plus Caffeine LoadingCache in `PipelineBiomeProvider` keeps this lightweight. 3D cave noise adds one extra dimension but only for Y-levels where extrusion is active.

4. **Vanilla decoration compatibility**: Because every biome has `vanilla: minecraft:XXX`, NMS `applyBiomeDecoration` places the correct vanilla features, ores, and structures for that biome. Terra's Bukkit populator also runs (for custom trees) but we've removed non-tree Terra features so there's no conflict.

5. **River biomes**: Vanilla places rivers via a separate noise parameter (river noise) that cuts through land biomes. Terra's pipeline does not have a native "river carve" stage. We approximate rivers by:
   - Adding a `river` noise sampler
   - Adding an early pipeline stage that replaces a narrow `river_noise` band with `RIVER` biome
   - Or accepting that rivers will be missing/approximated by swamp/wetland biomes
   - *Decision*: Defer true river generation to a later iteration; initial version uses humidity-based wetlands as river approximation.

## Verification Plan

1. **Unit test**: Create world with `continental_floor = continental_ceiling = -0.5` → verify all chunks report ocean biomes and no land terrain generates.
2. **Unit test**: Create world with `temperature_floor = temperature_ceiling = 0.8` → verify only desert/savanna/jungle/badlands biomes appear, and vanilla cacti/acacia trees spawn.
3. **Integration test**: Default parameters world. Fly across X/Z and verify biome transitions follow latitudinal pattern: frozen poles → temperate → hot equator (with noise patchiness).
4. **3D test**: Descend below Y=0 in default world. Verify `deep_dark` biome appears in some regions (skulk blocks, wardens spawnable) and `lush_caves` / `dripstone_caves` in others.
5. **Terrain linkage**: Verify each biome generates terrain at correct height (plains flat, jagged peaks tall) via terrain sampler from Work Item 3.
