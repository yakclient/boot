package net.yakclient.boot.maven

import com.durganmcbroom.artifact.resolver.ArtifactReference
import com.durganmcbroom.artifact.resolver.ArtifactStubResolver
import com.durganmcbroom.artifact.resolver.RepositoryFactory
import com.durganmcbroom.artifact.resolver.ResolutionContext
import com.durganmcbroom.artifact.resolver.simple.maven.*
import net.yakclient.archives.ResolutionResult
import net.yakclient.boot.archive.ArchiveKey
import net.yakclient.boot.archive.ArchiveResolutionProvider
import net.yakclient.boot.dependency.DependencyData
import net.yakclient.boot.dependency.DependencyGraph
import net.yakclient.boot.dependency.DependencyNode
import net.yakclient.boot.dependency.VersionIndependentDependencyKey
import net.yakclient.boot.security.PrivilegeAccess
import net.yakclient.boot.security.PrivilegeManager
import net.yakclient.boot.store.DataStore
import net.yakclient.common.util.make
import net.yakclient.common.util.resolve
import net.yakclient.common.util.resource.SafeResource
import java.io.File
import java.io.FileOutputStream
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level

public class MavenDependencyGraph(
    private val path: Path,
    store: DataStore<SimpleMavenArtifactRequest, DependencyData<SimpleMavenArtifactRequest>>,
    archiveResolver: ArchiveResolutionProvider<ResolutionResult>,
    initialGraph: MutableMap<ArchiveKey<SimpleMavenArtifactRequest>, DependencyNode> = HashMap(),
    // TODO not all privileges
    privilegeManager: PrivilegeManager = PrivilegeManager(null, PrivilegeAccess.allPrivileges()) {},
    private val stubResolutionProvider : (SimpleMavenArtifactRepository) -> ArtifactStubResolver<*, SimpleMavenArtifactStub, SimpleMavenArtifactReference> = SimpleMavenArtifactRepository::stubResolver,
    private val factory: RepositoryFactory<SimpleMavenRepositorySettings, SimpleMavenArtifactRequest, SimpleMavenArtifactStub, SimpleMavenArtifactReference, SimpleMavenArtifactRepository> = SimpleMaven
) : DependencyGraph<SimpleMavenArtifactRequest, SimpleMavenArtifactStub, SimpleMavenRepositorySettings>(
    store, factory, archiveResolver, initialGraph, privilegeManager
) {
    override fun cacherOf(settings: SimpleMavenRepositorySettings): ArchiveCacher<*> {
        val artifactRepository = factory.createNew(settings)

        return MavenDependencyCacher(
            ResolutionContext(
                artifactRepository,
                stubResolutionProvider(artifactRepository),
                SimpleMaven.artifactComposer
            )
        )
    }

    override fun writeResource(request: SimpleMavenArtifactRequest, resource: SafeResource): Path {
        val descriptor by request::descriptor

        val jarName = "${descriptor.artifact}-${descriptor.version}.jar"
        val jarPath = path resolve descriptor.group.replace(
            '.',
            File.separatorChar
        ) resolve descriptor.artifact resolve descriptor.version resolve jarName

        if (!Files.exists(jarPath)) {
            logger.log(Level.INFO, "Downloading dependency: '$descriptor'")

            Channels.newChannel(resource.open()).use { cin ->
                jarPath.make()
                FileOutputStream(jarPath.toFile()).use { fout ->
                    fout.channel.transferFrom(cin, 0, Long.MAX_VALUE)
                }
            }
        }

        return jarPath
    }

    private inner class MavenDependencyCacher(
        resolver: ResolutionContext<SimpleMavenArtifactRequest, SimpleMavenArtifactStub, ArtifactReference<*, SimpleMavenArtifactStub>>,
    ) : DependencyCacher(resolver) {
        override fun newLocalGraph(): LocalGraph = MavenLocalGraph()

        private inner class MavenLocalGraph : LocalGraph() {
            override fun getKey(request: SimpleMavenArtifactRequest): VersionIndependentDependencyKey {
                class VersionIndependentMavenKey : VersionIndependentDependencyKey {
                    private val group by request.descriptor::group
                    private val artifact by request.descriptor::artifact
                    private val classifier by request.descriptor::classifier

                    override fun equals(other: Any?): Boolean {
                        if (this === other) return true
                        if (other !is VersionIndependentMavenKey) return false

                        if (group != other.group) return false
                        if (artifact != other.artifact) return false
                        if (classifier != other.classifier) return false

                        return true
                    }

                    override fun hashCode(): Int {
                        var result = group.hashCode()
                        result = 31 * result + artifact.hashCode()
                        result = 31 * result + (classifier?.hashCode() ?: 0)
                        return result
                    }
                }

                return VersionIndependentMavenKey()
            }
        }
    }
}