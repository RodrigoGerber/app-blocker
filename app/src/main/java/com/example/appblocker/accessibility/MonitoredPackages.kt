package com.example.appblocker.accessibility

import com.example.appblocker.rules.BlockingRule

/**
 * Derives the set of package names the accessibility service should watch.
 *
 * - Empty while globally paused (so the service's hot path is a single,
 *   always-false set lookup and nothing is blocked).
 * - Otherwise the packages of *enabled* rules only — disabled rules need no
 *   monitoring since the policy would allow them anyway.
 *
 * Pure and unit-testable; the service just collects rules+pause through this.
 */
object MonitoredPackages {
    fun from(rules: List<BlockingRule>, paused: Boolean): Set<String> {
        if (paused) return emptySet()
        return rules.asSequence()
            .filter { it.enabled }
            .map { it.packageName }
            .toSet()
    }
}
