package me.chrr.tapestry.gradle.classtweaker

class AccessTransformer {
    private val classes = mutableMapOf<String, Rule>()
    private val methods = mutableMapOf<String, Rule>()
    private val fields = mutableMapOf<String, Rule>()

    fun clazz(name: String) =
        classes.getOrPut(name) { Rule(Visibility.Private, null) }

    fun method(clazz: String, name: String, descriptor: String) =
        methods.getOrPut("$clazz $name$descriptor") { Rule(Visibility.Private, null) }

    fun field(clazz: String, name: String) =
        fields.getOrPut("$clazz $name") { Rule(Visibility.Private, null) }

    fun build() = StringBuilder().apply {
        for ((name, rule) in classes)
            appendLine("$rule $name")
        for ((name, rule) in methods)
            appendLine("$rule $name")
        for ((name, rule) in fields)
            appendLine("$rule $name")
    }.toString()

    data class Rule(var visibility: Visibility, var final: FinalModifier?) {
        fun merge(visibility: Visibility? = null, final: FinalModifier? = null) {
            if (visibility != null && (this.visibility == null || this.visibility!! < visibility))
                this.visibility = visibility
            if (final != null && (this.final == null || this.final!! < final))
                this.final = final
        }

        override fun toString() =
            "${visibility.name.lowercase()}${
                when (final) {
                    FinalModifier.Add -> "+f"
                    FinalModifier.Remove -> "-f"
                    null -> ""
                }
            }"
    }

    enum class Visibility { Private, Default, Protected, Public }
    enum class FinalModifier { Add, Remove }
}