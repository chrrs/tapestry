package me.chrr.tapestry.gradle.classtweaker

import net.fabricmc.classtweaker.api.ClassTweakerReader
import net.fabricmc.classtweaker.api.visitor.AccessWidenerVisitor
import net.fabricmc.classtweaker.api.visitor.ClassTweakerVisitor
import java.io.BufferedReader

object ClassTweakerConverter {
    fun toAccessTransformer(reader: BufferedReader) =
        AccessTransformerBuilder()
            .also { ClassTweakerReader.create(it).read(reader, "") }
            .build()


    private class AccessTransformerBuilder : ClassTweakerVisitor {
        private val classes = mutableMapOf<String, AtModifier>()
        private val methods = mutableMapOf<String, AtModifier>()
        private val fields = mutableMapOf<String, AtModifier>()

        fun build() =
            StringBuilder().apply {
                for ((clazz, modifier) in classes) {
                    val atModifier = when {
                        modifier.mutable -> "public-f"
                        else -> "public"
                    }

                    appendLine("$atModifier $clazz")
                }


                for ((field, modifier) in methods) {
                    val atModifier = when {
                        modifier.mutable && modifier.accessible -> "public-f"
                        modifier.accessible -> "public+f"
                        else -> "protected-f"
                    }

                    appendLine("$atModifier $field")
                }

                for ((field, modifier) in fields) {
                    val atModifier = when {
                        modifier.mutable -> "public-f"
                        else -> "public"
                    }

                    appendLine("$atModifier $field")
                }
            }.toString()

        override fun visitHeader(namespace: String) {
            if (namespace != "official")
                throw IllegalArgumentException("Class Tweaker namespace must be 'official'")
        }

        override fun visitInjectedInterface(owner: String, iface: String, transitive: Boolean) =
            throw NotImplementedError("Injected interfaces in Class Tweakers are not supported")

        // Access wideners are not exactly mappable to access transformers, so we need
        // to mimic their behaviour somewhat. See https://wiki.fabricmc.net/tutorial:accesswidening.
        override fun visitAccessWidener(owner: String) =
            object : AccessWidenerVisitor {
                val className = owner.replace('/', '.')

                override fun visitClass(access: AccessWidenerVisitor.AccessType, transitive: Boolean) {
                    val modifier = classes.getOrPut(className) { AtModifier() }
                    when (access) {
                        AccessWidenerVisitor.AccessType.ACCESSIBLE -> modifier.accessible = true
                        AccessWidenerVisitor.AccessType.EXTENDABLE -> modifier.mutable = true
                        else -> throw IllegalArgumentException("Class access type $access is not supported")
                    }
                }

                override fun visitMethod(
                    name: String,
                    descriptor: String,
                    access: AccessWidenerVisitor.AccessType,
                    transitive: Boolean
                ) {
                    val modifier = methods.getOrPut("$className $name$descriptor") { AtModifier() }
                    when (access) {
                        AccessWidenerVisitor.AccessType.ACCESSIBLE -> modifier.accessible = true
                        AccessWidenerVisitor.AccessType.EXTENDABLE -> modifier.mutable = true
                        else -> throw IllegalArgumentException("Method access type $access is not supported")
                    }
                }

                override fun visitField(
                    name: String,
                    descriptor: String,
                    access: AccessWidenerVisitor.AccessType,
                    transitive: Boolean
                ) {
                    val modifier = fields.getOrPut("$className $name") { AtModifier() }
                    when (access) {
                        AccessWidenerVisitor.AccessType.ACCESSIBLE -> modifier.accessible = true
                        AccessWidenerVisitor.AccessType.MUTABLE -> modifier.mutable = true
                        else -> throw IllegalArgumentException("Field access type $access is not supported")
                    }
                }
            }
    }

    private data class AtModifier(var accessible: Boolean = false, var mutable: Boolean = false)
}