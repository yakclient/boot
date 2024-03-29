package net.yakclient.boot.archive

public fun interface ArchiveAccessAuditor {
    public fun audit(tree: ArchiveAccessTree): ArchiveAccessTree
}

public fun ArchiveAccessAuditor.chain(other: ArchiveAccessAuditor): ArchiveAccessAuditor = ArchiveAccessAuditor {
    other.audit(this@chain.audit(it))
}

public fun ArchiveAccessTree.prune(target: ArchiveTarget) : ArchiveAccessTree {
    return object : ArchiveAccessTree by this@prune {
        override val targets: Set<ArchiveTarget> = this@prune.targets.filterNotTo(HashSet()) {
            it === target
        }
    }
}