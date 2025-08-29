object Versions {
    object Terra {
        const val overworldConfig = "v1.3.4"
    }
    
    object Libraries {
        const val tectonic = "4.2.1"
        const val paralithic = "0.8.1"
        const val strata = "1.3.2"
        
        const val cloud = "2.0.0"
        
        const val caffeine = "3.1.8"
        
        const val slf4j = "2.0.16"

        object Internal {
            const val shadow = "8.3.3"
            const val apacheText = "1.12.0"
            const val apacheIO = "2.17.0"
            const val guava = "33.3.1-jre"
            const val asm = "9.7.1"
            const val snakeYml = "2.3"
            const val jetBrainsAnnotations = "26.0.1"
            const val junit = "5.11.3"
            const val nbt = "6.1"
        }
    }
    
    object Mod {
        const val mixin = "0.15.3+mixin.0.8.7"
        
        const val minecraft = "1.21.4"
        const val yarn = "$minecraft+build.8"
        const val fabricLoader = "0.16.10"
        
        const val architecuryLoom = "1.7.413"
        const val architecturyPlugin = "3.4.159"

    }
    
    object Bukkit {
        const val minecraft = "1.21.4"
        const val paperBuild = "$minecraft-R0.1-SNAPSHOT"
        const val paper = paperBuild
        const val paperLib = "1.0.8"
        const val reflectionRemapper = "0.1.1"
        const val paperDevBundle = paperBuild
        const val runPaper = "2.3.1"
        const val paperWeight = "1.7.2"
        const val cloud = "2.0.0-beta.10"
    }
    
    object CLI {
        const val logback = "1.5.8"
        const val picocli = "4.7.6"
    }
}
