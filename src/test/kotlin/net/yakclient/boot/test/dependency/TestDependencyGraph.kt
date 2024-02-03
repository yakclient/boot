package net.yakclient.boot.test.dependency

import bootFactories
import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.durganmcbroom.artifact.resolver.simple.maven.HashType
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenArtifactRequest
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings
import com.durganmcbroom.jobs.JobName
import kotlinx.coroutines.runBlocking
import net.yakclient.boot.archive.ArchiveAccessTree
import net.yakclient.boot.archive.ArchiveGraph
import net.yakclient.boot.archive.ArchiveTarget
import net.yakclient.boot.dependency.BasicDependencyNode
import net.yakclient.boot.dependency.DependencyNode
import net.yakclient.boot.main.createMavenDependencyGraph
import net.yakclient.boot.maven.MavenDependencyResolver
import orThrow
import java.nio.file.Files
import kotlin.test.Test

class TestDependencyGraph {
    @Test
    fun `Test maven basic dependency loading`() {
        val basePath = Files.createTempDirectory("m2cache")

        val maven = createMavenDependencyGraph()
        val archiveGraph = ArchiveGraph(basePath,
            listOf(
                "net.yakclient:archives:1.1-SNAPSHOT",
                "net.yakclient:archives-mixin:1.1-SNAPSHOT",
                "io.arrow-kt:arrow-core:1.1.2",
                "org.jetbrains.kotlinx:kotlinx-cli:0.3.5",
                "net.yakclient:boot:2.0-SNAPSHOT",
                "com.durganmcbroom:artifact-resolver:1.0-SNAPSHOT",
                "com.durganmcbroom:artifact-resolver-jvm:1.0-SNAPSHOT",
                "com.durganmcbroom:artifact-resolver-simple-maven:1.0-SNAPSHOT",
                "com.durganmcbroom:artifact-resolver-simple-maven-jvm:1.0-SNAPSHOT",
                "net.bytebuddy:byte-buddy-agent:1.12.18",
                "net.yakclient:common-util:1.0-SNAPSHOT",
                "com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4",
                "net.yakclient:archive-mapper:1.2-SNAPSHOT",
                "net.yakclient:object-container:1.0-SNAPSHOT",
                "com.durganmcbroom:jobs:1.0-SNAPSHOT",
                "com.durganmcbroom:jobs-logging:1.0-SNAPSHOT",
                "com.durganmcbroom:jobs-progress:1.0-SNAPSHOT",
                "com.durganmcbroom:jobs-progress-simple:1.0-SNAPSHOT",
                "com.durganmcbroom:jobs-progress-simple-jvm:1.0-SNAPSHOT",
            ).map {
                SimpleMavenDescriptor.parseDescription(it)!!
            }.associateWithTo(HashMap()) {
                BasicDependencyNode(
                    it,
                    null,
                    setOf(),
                    object : ArchiveAccessTree {
                        override val descriptor: ArtifactMetadata.Descriptor = it
                        override val targets: Set<ArchiveTarget> = setOf()
                    },
                    MavenDependencyResolver(
                        parentClassLoader = ClassLoader.getPlatformClassLoader(),
                    )
                )
            }
        )

        val request = SimpleMavenArtifactRequest(
            "net.yakclient.minecraft:minecraft-provider-def:1.0-SNAPSHOT",
            includeScopes = setOf("compile", "runtime", "import")
        )

        cacheAndGet(archiveGraph, request, SimpleMavenRepositorySettings.local(
            preferredHash = HashType.SHA1
        ), maven)
    }

    @Test
    fun `Test maven dual dependency loading`() {
        val basePath = Files.createTempDirectory("m2cache")

        val maven = createMavenDependencyGraph()
        val archiveGraph = ArchiveGraph(basePath)

        val request = SimpleMavenArtifactRequest(
            "net.yakclient.minecraft:minecraft-provider-def:1.0-SNAPSHOT",
            includeScopes = setOf("compile", "runtime", "import")
        )

        cacheAndGet(archiveGraph, request, SimpleMavenRepositorySettings.local(
            preferredHash = HashType.SHA1
        ), maven)

        separator()

        val secondRequest = SimpleMavenArtifactRequest(
            "io.arrow-kt:arrow-core:1.2.1",
            includeScopes = setOf("compile", "runtime", "import")
        )

        cacheAndGet(archiveGraph, secondRequest,SimpleMavenRepositorySettings.mavenCentral(
            preferredHash = HashType.SHA1
        ), maven)
    }

    private fun cacheAndGet(
        archiveGraph: ArchiveGraph,
        request: SimpleMavenArtifactRequest,
        repository: SimpleMavenRepositorySettings,
        maven: MavenDependencyResolver
    ) {
        val node = runBlocking(bootFactories() + JobName("test")) {
            archiveGraph.cache(
                request,
                repository,
                maven
            ).orThrow()

            archiveGraph.get(request.descriptor, maven)
        }.orThrow()

        node.prettyPrint { handle, depth ->
            val str = (0..depth).joinToString(separator = "   ") { "" } + handle.descriptor.name
            println(str)
        }
        separator()
        println(node.access.targets.joinToString(separator = "\n") {
            it.descriptor.name
        })
    }
}