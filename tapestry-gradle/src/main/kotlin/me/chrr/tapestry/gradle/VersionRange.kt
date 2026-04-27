package me.chrr.tapestry.gradle

/** A version range for a dependency, with both an upper bound and a lower bound. */
data class VersionRange(var lower: Bound?, var upper: Bound?) {
    companion object {
        /** Define a version range as "within" the given version, e.g. non-specified parts of the version don't matter.
         *  Ex. the version "within" 26.1 will match 26.1, 26.1.3, but not 26.2. */
        fun within(version: Version) = VersionRange(
            lower = Bound(version, inclusive = true),
            upper = Bound(version.next(), inclusive = false)
        )

        /** Define a version range that only matches the given version. */
        fun exactly(version: Version) = VersionRange(
            lower = Bound(version, inclusive = true),
            upper = Bound(version, inclusive = true)
        )

        /** Parse a version range in a fabric semver-like format, e.g. ">=26.1 <26.3.2" or "~1.21". */
        fun parse(input: String): VersionRange {
            val input = input.trim()

            // If the input is a star, it should match all versions.
            if (input == "*")
                return VersionRange(null, null)

            // If we start with the within operator, we know it's a within version.
            if (input.startsWith("~"))
                return within(Version.parse(input.substring(1)))

            // If we don't start with another control character, we know it's a single version.
            if (!input.contains(' ') && !input.startsWith('<') && !input.endsWith('>'))
                return exactly(Version.parse(input))

            // Otherwise, parse each of the parts separately.
            var lowerBound: Bound? = null
            var upperBound: Bound? = null

            for (part in input.split(' '))
                when {
                    part.startsWith(">=") -> lowerBound = Bound(Version.parse(part.substring(2)), inclusive = true)
                    part.startsWith("<=") -> upperBound = Bound(Version.parse(part.substring(2)), inclusive = true)
                    part.startsWith('>') -> lowerBound = Bound(Version.parse(part.substring(1)), inclusive = false)
                    part.startsWith('<') -> upperBound = Bound(Version.parse(part.substring(1)), inclusive = false)
                }

            return VersionRange(lowerBound, upperBound)
        }
    }

    /** Convert this range to a Fabric-compatible version dependency. */
    fun toFabricVersionString() =
        listOfNotNull(
            lower?.let { "${if (it.inclusive) ">=" else ">"}${it.version}" },
            upper?.let { "${if (it.inclusive) "<=" else "<"}${it.version}" },
        )
            .ifEmpty { listOf("*") }
            .joinToString(separator = " ")

    /** Convert this range to a NeoForge-compatible version dependency. */
    fun toNeoForgeVersionString() =
        listOfNotNull(
            lower?.let { "${if (it.inclusive) "[" else "("}${it.version}" },
            upper?.let { "${it.version}${if (it.inclusive) "]" else ")"}" },
        )
            .ifEmpty { listOf("[", ")") }
            .joinToString(separator = ",")

    /** A single version boundary for a version range, either inclusive or exclusive. */
    data class Bound(var version: Version, var inclusive: Boolean = false)
}

/** A semver-like parsed version. */
data class Version(val major: Int, val minor: Int? = null, val patch: Int? = null, val suffix: String? = null) {
    companion object {
        /** Parse a semver-like version from the given string. */
        fun parse(input: String): Version {
            val parts = input.split("-", limit = 2)
            val nums = parts[0].split(".", limit = 3).map { it.toInt() }
            require(nums.isNotEmpty()) { "Invalid version: $input" }

            return Version(
                major = nums[0],
                minor = nums.getOrNull(1),
                patch = nums.getOrNull(2),
                suffix = parts.getOrNull(1),
            )
        }
    }

    /** Get the "previous" version from this version, decreasing the lowest significant part of the version. */
    fun previous() =
        when {
            patch != null && patch > 0 -> Version(major, minor, patch - 1)
            minor != null && minor > 0 -> Version(major, minor - 1, 999)
            else -> Version(major - 1, 999, 999)
        }

    /** Get the "next" version from this version, increasing the lowest significant part of the version possible. */
    fun next() =
        when {
            patch != null -> Version(major, minor, patch + 1)
            minor != null -> Version(major, minor + 1)
            else -> Version(major + 1)
        }

    override fun toString() =
        StringBuilder("$major")
            .also { if (patch != null || minor != null) it.append('.').append(minor ?: "0") }
            .also { if (patch != null) it.append('.').append(minor) }
            .also { if (suffix != null) it.append('-').append(minor) }
            .toString()
}