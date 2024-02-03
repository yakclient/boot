package net.yakclient.boot.loader

import net.yakclient.archives.ArchiveHandle
import net.yakclient.boot.util.packageName
import java.net.URL

public interface ClassProvider {
    public val packages: Set<String>

    public fun findClass(name: String): Class<*>?
}

public open class ArchiveClassProvider(
    protected val archive: ArchiveHandle
) : ClassProvider {
    override val packages: Set<String> by archive::packages

    override fun findClass(name: String): Class<*>? =
        if (packages.contains(name.packageName)) net.yakclient.common.util.runCatching(ClassNotFoundException::class) {
            archive.classloader.loadClass(
                name
            )
        } else null
}

public fun emptyClassProvider() : ClassProvider {
    return object : ClassProvider {
        override val packages: Set<String> = setOf()

        override fun findClass(name: String): Class<*>? {
            return null
        }
    }
}

public fun ArchiveClassProvider(archive: ArchiveHandle?) : ClassProvider = if (archive == null) emptyClassProvider() else ArchiveClassProvider(archive)