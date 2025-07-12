/*
 * This file is part of BaseProject - https://github.com/FlorianMichael/BaseProject
 * Copyright (C) 2024-2025 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.florianmichael.baseproject

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

/**
 * Returns a [MappingsConfigurer] that configures Yarn mappings for the project.
 *
 * Required project property:
 * - `nostalgia_mappings`: The version of Yarn mappings to use.
 *
 * @param version Optional override for the Yarn version.
 */
fun nostalgiaMapped(version: String? = null): MappingsConfigurer = {
    repositories {
        maven("https://maven.wispforest.io")
        maven("https://maven.alphasmoddingserver.com/releases")
    }
    val nostalgiaVersion = version ?: property("nostalgia_mappings") as String
    dependencies {
        "mappings"("me.alphamode:nostalgia:$nostalgiaVersion:v2")
    }
}

/**
 * Sets up Fabric Loom with Minecraft dependencies, mappings, Kotlin support, and mod metadata processing.
 *
 * Required project properties:
 * - `minecraft_version`: Minecraft version to target
 * - `babric_loader_version`: Babric loader version
 *
 * Optional project properties:
 * - `fabric_kotlin_version`: Fabric Kotlin language module version (used if Kotlin plugin is applied)
 * - `supported_minecraft_versions`: Used in mod metadata if provided
 *
 * @param mappings The mappings configuration to apply (Yarn or Mojang+Parchment)
 */
fun Project.setupBabric(mappings: MappingsConfigurer = nostalgiaMapped()) {
    plugins.apply("babric-loom")
    val accessWidenerFile = file("src/main/resources/${project.name.lowercase()}.accesswidener")
    if (accessWidenerFile.exists()) {
        extensions.getByType(LoomGradleExtensionAPI::class.java).apply {
            accessWidenerPath.set(accessWidenerFile)
        }
    }
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.parchmentmc.org/")
        maven("https://maven.glass-launcher.net/snapshots/")
        maven("https://maven.glass-launcher.net/releases/")
        maven("https://maven.glass-launcher.net/babric")
        maven("https://maven.minecraftforge.net/")
    }
    dependencies {
        "minecraft"("com.mojang:minecraft:${property("minecraft_version")}")
        "modImplementation"("babric:fabric-loader:${property("babric_loader_version")}")
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        dependencies {
            "modImplementation"("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
        }
    }
    mappings()
    tasks.named<ProcessResources>("processResources").configure {
        val projectName = project.name
        val projectVersion = project.version
        val projectDescription = project.description
        val mcVersion = if (!project.hasProperty("supported_minecraft_versions")) {
            project.property("minecraft_version") as String
        } else {
            val supportedVersions = project.property("supported_minecraft_versions") as String
            supportedVersions.ifEmpty {
                project.property("minecraft_version") as String
            }
        }
        val latestCommitHash = latestCommitHash()
        filesMatching("fabric.mod.json") {
            expand(
                "version" to projectVersion,
                "implVersion" to "git-${projectName}-${projectVersion}:${latestCommitHash}",
                "description" to projectDescription,
                "mcVersion" to mcVersion,
            )
        }
    }

    excludeRunFolder()
}
