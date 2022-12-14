package net.yakclient.boot.dependency

import net.yakclient.common.util.ServiceMapCollector

public class DependencyProviders : ServiceMapCollector<String, DependencyGraphProvider<*, *>>({
    it.name
}) {
    public fun getByType(name: String): DependencyGraphProvider<*, *>? = services[name]
}