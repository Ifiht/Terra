# Noise Provider Plugin Plan

## Overview

A new Terra addon that provides a simplified interface for overriding the 6 main Overworld noise parameters used in biome generation: **temperature**, **humidity**, **continentalness**, **erosion**, **weirdness**, and **depth**. This plugin allows pack developers to set any of these noise functions to custom values, including simple constant integers to create line/planar functions (e.g., a vertical line where biome changes only by Y-coordinate).

## Background

From `worldgen.md`, the Overworld biome generation uses 6 noise parameters:

| Parameter | Purpose | Range |
|-----------|---------|-------|
| **temperature** | Determines climate zones (snow/ice vs hot) | -1.0 ~ 1.0 |
| **humidity** (vegetation) | Determines moisture (desert vs jungle) | -1.0 ~ 1.0 |
| **continentalness** (continents) | Determines ocean/beach/land distinction | -1.2 ~ 1.0 |
| **erosion** | Determines flat vs mountainous terrain | -1.0 ~ 1.0 |
| **weirdness** (ridges) | Determines terrain variants, shattered terrain, peaks/valleys | -1.0 ~ 1.0 |
| **depth** | Determines cave biome placement (not directly noise-based) | ~0.0 ~ 1.0 |

These parameters are sampled at each block position during terrain generation. In vanilla Minecraft, they are generated using layered Perlin noise functions with multiple octaves. Terra currently uses the `config-noise-function` addon to define these via complex YAML templates.

## Problem Statement

The existing Terra noise system (`config-noise-function`) is powerful but complex. Pack developers who simply want to:
- Create a world with a single biome (constant noise)
- Create vertical biome bands (noise based only on Y)
- Create horizontal biome stripes (noise based only on X or Z)
- Swap in custom noise algorithms

...must navigate a deeply nested configuration system with templates, samplers, normalizers, and fractal combinators.

## Solution: Simplified Noise Provider Addon

A new addon that exposes a **flat configuration interface** for the 6 main noise parameters while internally using Terra's existing noise API. It acts as a "noise provider" — injecting configured noise functions into the noise router at the appropriate generation stages.

## Architecture

### Key Insight from Existing Terra Code

From analyzing `NoiseAddon.java`, the existing system works by:

1. **Registration Phase**: During `ConfigPackPreLoadEvent`, the addon registers `ObjectTemplate<NoiseSampler>` suppliers in a pack-specific registry keyed by `TypeKey<Supplier<ObjectTemplate<NoiseSampler>>>`.

2. **Template Resolution**: Templates are loaded from YAML and produce `NoiseSampler` instances. Templates support nesting (e.g., FBM = BrownianMotion = sum of octaves).

3. **Injection Point**: The noise router is built from the pack configuration. Noise samplers are resolved by name from the registry and composed into the final noise functions used by the biome provider.

4. **Addon Loading**: Addons implement `AddonInitializer` and are discovered via the manifest addon loader. Each addon has a `BaseAddon` with metadata (key, name, version).

### Noise Provider Design

The noise provider addon does NOT replace the existing noise system. Instead, it provides:

1. **A simplified configuration layer** that translates flat noise parameter definitions into Terra's internal `NoiseSampler` objects.
2. **A set of built-in noise primitives** commonly needed for world generation patterns (constant, linear, gradient, stripe, cellular).
3. **Integration with Terra's noise router** via the existing `ConfigPackPreLoadEvent` mechanism.

### Directory Structure

```
common/addons/noise-provider/
├── build.gradle.kts
├── LICENSE
├── README.md
├── src/
│   └── main/
│       ├── java/
│       │   └── com/dfsek/terra/addons/
│       │       └── noiseprovider/
│       │           ├── NoiseProviderAddon.java       # Main addon initializer
│       │           ├── config/
│       │           │   ├── NoiseParameterConfig.java  # Parsed config for one parameter
│       │           │   └── NoiseProviderConfig.java   # Top-level config holder
│       │           ├── sampler/
│       │           │   ├── ConstantSampler.java        # Returns a fixed value
│       │           │   ├── GradientSampler.java        # Linear gradient along an axis
│       │           │   ├── StripeSampler.java          # Vertical/horizontal stripes
│       │           │   ├── CylinderSampler.java        # Radial gradient from center
│       │           │   └── SineStripeSampler.java      # Sine-based stripes
│       │           └── util/
│       │               └── NoiseBuilder.java           # Builds NoiseSampler from config
│       └── resources/
│           └── terra.addon.yml                        # Addon manifest
```

### Configuration Format

Pack developers will configure noise in a simple YAML file within their pack:

```yaml
# noise-provider.yml (or noise-provider.yaml)
# Simplified noise parameter configuration for Terra

parameters:
  temperature:
    type: perlin
    seed: 12345
    frequency: 0.01
    amplitude: 1.0
    octaves: 6
    
  humidity:
    type: perlin
    seed: 67890
    frequency: 0.008
    amplitude: 1.0
    octaves: 4
    
  continentalness:
    type: perlin
    seed: 11111
    frequency: 0.002
    amplitude: 1.0
    octaves: 5
    
  erosion:
    type: perlin
    seed: 22222
    frequency: 0.003
    amplitude: 1.0
    octaves: 4
    
  weirdness:
    type: perlin
    seed: 33333
    frequency: 0.004
    amplitude: 1.0
    octaves: 6
    
  depth:
    type: constant
    value: 0.0
    # depth is computed from Y position in vanilla, 
    # but can be overridden here

# Alternative: simple patterns
patterns:
  temperature: stripe-vertical    # or "gradient", "cylinder", "perlin", "constant"
  humidity: perlin
  continentalness: perlin
  erosion: perlin
  weirdness: perlin
  depth: constant

# Pattern-specific options
options:
  stripe-vertical:
    axis: y                       # x, y, z, or "auto" (uses height)
  constant:
    value: 0.5                    # fixed value for constant pattern
```

### Built-in Patterns

| Pattern | Description | Use Case |
|---------|-------------|----------|
| `perlin` | Standard Perlin noise (default) | Natural terrain |
| `constant` | Returns a fixed value | Single-biome world |
| `stripe-vertical` | Vertical stripes (noise on X only) | Biome bands by X |
| `stripe-horizontal` | Horizontal stripes (noise on Z only) | Biome bands by Z |
| `stripe-y` | Vertical gradient (noise on Y only) | Horizontal biome layers |
| `cylinder` | Radial gradient from world center | Island-style worlds |
| `sine-stripe` | Sine wave stripes | Periodic biome patterns |
| `flat` | Always returns 0 | Alias for constant:0 |

### Integration with Terra's Noise Router

The addon integrates with Terra's world generation via the existing event system:

1. **Listen to `ConfigPackPreLoadEvent`** (like `NoiseAddon` does).
2. **Load `noise-provider.yml`** from the pack's root directory.
3. **For each configured parameter**, build the appropriate `NoiseSampler` from the built-in samplers.
4. **Register the samplers** in the pack's noise registry under special keys (`NOISE_PROVIDER_TEMP`, `NOISE_PROVIDER_HUMIDITY`, etc.).
5. **Provide a hook** for the biome provider to query these pre-configured samplers.

The key difference from the existing system is that the noise provider **does not require the pack developer to understand Terra's template system**. It handles the conversion internally.

### Implementation Details

#### NoiseProviderAddon.java

```kotlin
class NoiseProviderAddon : AddonInitializer {
    companion object {
        val PARAMETER_TOKEN = TypeKey<Supplier<ObjectTemplate<NoiseSampler>>>()
    }
    
    override fun initialize() {
        val plugin = /* injected */
        val addon = /* injected */
        
        plugin.eventManager.getHandler(FunctionalEventHandler::class.java)
            .register(addon, ConfigPackPreLoadEvent::class.java) { event ->
                // Load noise-provider.yml from the pack
                val config = loadConfig(event.pack)
                if (config == null) return@register
                
                val registry = event.pack.getOrCreateRegistry(PARAMETER_TOKEN)
                
                // For each parameter, build and register the sampler
                config.parameters.forEach { (name, paramConfig) ->
                    val sampler = NoiseBuilder.build(paramConfig)
                    registry.register(addon.key(name.uppercase()), { sampler })
                }
            }
            .priority(100)  // After NoiseAddon (priority 50)
            .failThrough()
    }
}
```

#### NoiseBuilder.java

```java
public class NoiseBuilder {
    public static NoiseSampler build(NoiseParameterConfig config) {
        return switch (config.getType()) {
            case CONSTANT -> new ConstantSampler(config.getValue());
            case STRIPE_VERTICAL -> new StripeSampler(X_AXIS, config.getFrequency(), config.getAmplitude());
            case STRIPE_HORIZONTAL -> new StripeSampler(Z_AXIS, config.getFrequency(), config.getAmplitude());
            case STRIPE_Y -> new StripeSampler(Y_AXIS, config.getFrequency(), config.getAmplitude());
            case CYLINDER -> new CylinderSampler(config.getFrequency(), config.getAmplitude());
            case SINE_STRIPE -> new SineStripeSampler(config.getAxis(), config.getFrequency(), config.getAmplitude());
            case PERLIN -> new PerlinSampler(config.getSeed(), config.getFrequency(), config.getAmplitude(), config.getOctaves());
        };
    }
}
```

#### ConstantSampler.java (example built-in sampler)

```java
public class ConstantSampler implements NoiseSampler {
    private final double value;
    
    public ConstantSampler(double value) {
        this.value = value;
    }
    
    @Override
    public double sample(int x, int y, int z) {
        return value;
    }
    
    @Override
    public double sample(double x, double y, double z) {
        return value;
    }
}
```

#### GradientSampler.java (for Y-based vertical bands)

```java
public class GradientSampler implements NoiseSampler {
    private final double value;  // The constant value
    
    public GradientSampler(double value) {
        this.value = value;
    }
    
    @Override
    public double sample(int x, int y, int z) {
        return value;  // Always returns the same value regardless of position
    }
}
```

Note: For a true "line function" (biome changes only along one axis), the developer sets `type: constant` with the desired value. For a gradient along Y, they use `type: stripe-y` with frequency controlling the rate of change.

### Pack Integration

Pack developers add the noise provider to their pack by:

1. Placing `noise-provider.yml` in the pack root.
2. Adding the addon to their pack's manifest or dependency list.
3. Referencing the configured parameters in their biome definitions.

### Build Configuration

```kotlin
// build.gradle.kts
plugins {
    id("java-library")
}

dependencies {
    implementation(project(":terra-api"))
    implementation(project(":common:addons:manifest-api"))
    implementation(project(":common:addons:api-addon-loader"))
    implementation(project(":common:implementation"))
}
```

### Addon Manifest (terraform.yml)

```yaml
name: noise-provider
version: 1.0.0
description: Simplified noise parameter provider for world generation
authors: [Terra Team]
license: MIT
dependencies:
  - manifest-api
  - api-addon-loader
```

## Implementation Steps

1. **Create directory structure** for the addon under `common/addons/noise-provider/`.
2. **Create `build.gradle.kts`** with dependencies on Terra API, manifest API, and addon loader.
3. **Create `terraform.yml`** manifest file.
4. **Implement `NoiseProviderAddon.java`** — the main addon initializer that listens to `ConfigPackPreLoadEvent`.
5. **Implement configuration classes** (`NoiseParameterConfig`, `NoiseProviderConfig`) — plain data classes for parsed YAML.
6. **Implement built-in samplers**:
   - `ConstantSampler` — returns a fixed value (for single-biome worlds)
   - `GradientSampler` — linear gradient along a chosen axis
   - `StripeSampler` — periodic stripes along an axis
   - `CylinderSampler` — radial gradient from world center
   - `SineStripeSampler` — sine-wave periodic stripes
7. **Implement `NoiseBuilder`** — factory that converts config to `NoiseSampler` instances.
8. **Write README.md** with configuration examples and documentation.
9. **Update `settings.gradle.kts`** to include the new module.
10. **Test** by building the project and verifying the addon loads correctly.

## Configuration Examples

### Single Biome World (flat world)
```yaml
parameters:
  temperature: { type: constant, value: 0.5 }
  humidity: { type: constant, value: 0.5 }
  continentalness: { type: constant, value: 0.0 }
  erosion: { type: constant, value: 0.0 }
  weirdness: { type: constant, value: 0.0 }
  depth: { type: constant, value: 0.0 }
```

### Vertical Biome Layers (biomes change by height)
```yaml
parameters:
  temperature: { type: stripe-y, frequency: 0.01, amplitude: 1.0 }
  humidity: { type: constant, value: 0.5 }
  continentalness: { type: constant, value: 0.0 }
  erosion: { type: constant, value: 0.0 }
  weirdness: { type: constant, value: 0.0 }
  depth: { type: constant, value: 0.0 }
```

### Horizontal Biome Bands (X-axis)
```yaml
parameters:
  temperature: { type: stripe-vertical, frequency: 0.005, amplitude: 1.0 }
  humidity: { type: stripe-vertical, frequency: 0.003, amplitude: 1.0 }
  continentalness: { type: stripe-vertical, frequency: 0.002, amplitude: 1.0 }
  erosion: { type: constant, value: 0.0 }
  weirdness: { type: constant, value: 0.0 }
  depth: { type: constant, value: 0.0 }
```

### Island World (radial from center)
```yaml
parameters:
  temperature: { type: cylinder, frequency: 0.001, amplitude: 1.0 }
  humidity: { type: cylinder, frequency: 0.0015, amplitude: 1.0 }
  continentalness: { type: cylinder, frequency: 0.002, amplitude: 1.0 }
  erosion: { type: constant, value: 0.5 }
  weirdness: { type: constant, value: 0.0 }
  depth: { type: constant, value: 0.0 }
```

## Relationship to Existing Terra System

This addon is **complementary** to the existing `config-noise-function` addon:

| Aspect | config-noise-function | noise-provider |
|--------|----------------------|----------------|
| Complexity | High (templates, samplers, normalizers) | Low (flat YAML) |
| Flexibility | Full (any noise composition) | Limited (built-in patterns) |
| Use case | Advanced pack dev | Casual/modpack dev |
| Configuration | Deep YAML hierarchy | Single flat file |
| Noise types | All (Perlin, Simplex, Value, Cellular, etc.) | Core 6 parameters only |

The noise provider internally **uses** Terra's existing noise samplers (PerlinSampler, ConstantSampler, etc.) but exposes them through a simplified interface. It does NOT require replacing the noise router — it registers its built samplers into the existing registry system.

## Future Extensions

- **Custom noise algorithms**: Allow users to provide their own sampler implementations via the addon API.
- **Noise composition**: Support simple arithmetic compositions (e.g., `temperature: { expression: "erosion * 2 + 1" }`).