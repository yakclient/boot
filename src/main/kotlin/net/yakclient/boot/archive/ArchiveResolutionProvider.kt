package net.yakclient.boot.archive

import com.durganmcbroom.jobs.*
import kotlinx.coroutines.coroutineScope
import net.yakclient.archives.*
import net.yakclient.archives.jpm.JpmResolutionResult
import net.yakclient.archives.zip.ZipResolutionResult
import java.nio.file.Path
import kotlin.io.path.exists

public interface ArchiveResolutionProvider<out R : ResolutionResult> {
    public suspend fun resolve(
        resource: Path,
        classLoader: ClassLoaderProvider<ArchiveReference>,
        parents: Set<ArchiveHandle>,
    ): JobResult<R, ArchiveException>
}

public open class BasicArchiveResolutionProvider<T : ArchiveReference, R : ResolutionResult>(
    protected val finder: ArchiveFinder<T>,
    protected val resolver: ArchiveResolver<T, R>,
) : ArchiveResolutionProvider<R> {
    override suspend fun resolve(
        resource: Path,
        classLoader: ClassLoaderProvider<ArchiveReference>,
        parents: Set<ArchiveHandle>,
    ): JobResult<R, ArchiveException> = jobScope {
        if (!resource.exists()) fail(ArchiveException.ArchiveLoadFailed("Given path: '$resource' to archive does not exist!", jobElement(ArchiveTrace)))

        runCatching {
            resolver.resolve(
                listOf(finder.find(resource)),
                classLoader,
                parents
            ).first()
        }.let {
            if (it.isFailure)
                fail(ArchiveException.ArchiveLoadFailed(
                    it.exceptionOrNull()?.message!!, coroutineScope { jobElement(ArchiveTrace) }
                ))
            else it.getOrNull()!!
        }
    }
}

public object JpmResolutionProvider : BasicArchiveResolutionProvider<ArchiveReference, JpmResolutionResult>(
    Archives.Finders.JPM_FINDER as ArchiveFinder<ArchiveReference>,
    Archives.Resolvers.JPM_RESOLVER
)

public object ZipResolutionProvider : BasicArchiveResolutionProvider<ArchiveReference, ZipResolutionResult>(
    Archives.Finders.ZIP_FINDER as ArchiveFinder<ArchiveReference>,
    Archives.Resolvers.ZIP_RESOLVER
)
