package com.aniket.myevent.processor

import com.aniket.myevent.annotations.MyEvent
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import java.io.OutputStream

class MyEventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {

        val symbols = resolver
            .getSymbolsWithAnnotation(MyEvent::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        val dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray())

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(MyEventKClassVisitor(dependencies), Unit) }

        return unableToProcess.toList()
    }

    private inner class MyEventKClassVisitor(val dependencies: Dependencies) : KSVisitorVoid() {
        private val packageName = "com.aniket.myevent"

        override fun visitClassDeclaration(
            classDeclaration:
            KSClassDeclaration, data: Unit
        ) {
            if (classDeclaration.isAbstract()) {
                logger.error(
                    "||Class Annotated with MyEvent should kotlin data class", classDeclaration
                )
            }

            if (classDeclaration.classKind != ClassKind.CLASS) {
                logger.error(
                    "||Class Annotated with Projections should kotlin data class", classDeclaration
                )
            }

            val className = classDeclaration.simpleName.getShortName()
            val classPackage = classDeclaration.packageName.asString() + "." + className
            val classVariableNameInCamelCase = className.replaceFirst(
                className[0],
                className[0].lowercaseChar()
            ) //need this for using in generated code

            logger.warn("package $classPackage")

            val properties = classDeclaration.primaryConstructor?.parameters ?: emptyList()

            if (properties.isEmpty())
                logger.error("No variables found in class", classDeclaration)

            val hashmapEntries = StringBuilder()
            val bundleEntries = StringBuilder()
            for (prop in properties) {
                // Throw Error if param is not primitive
                if (prop.isNotKotlinPrimitive())
                    logger.error("|| Event params variables should be Primitive", prop)

                val propName = prop.name?.getShortName() ?: ""
                logger.warn("|| ${prop.name?.getShortName()}")

                hashmapEntries.append(
                    """
                put("$propName", $classVariableNameInCamelCase.$propName)

                """.trimMargin()
                )


                val propPrimitiveTypeName = prop.getPrimitiveTypeName()
                bundleEntries.append(
                    """
                put$propPrimitiveTypeName("$propName", $classVariableNameInCamelCase.$propName)

                """.trimMargin()
                )
            }


            val toGenerateFileName = "${classDeclaration.simpleName.getShortName()}Event"

            val outputStream: OutputStream = codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName,
                fileName = toGenerateFileName
            )



            outputStream.write(
                """
    |package $packageName

    |import $classPackage
    |import android.os.Bundle
    |import $packageName.Event

    |class $toGenerateFileName(val $classVariableNameInCamelCase: $className): Event {
    |   override fun getHashMapOfParamsForCustomAnalytics(): HashMap<*, *>? {
    |       val map = HashMap<String, Any>().apply {
    |       $hashmapEntries
    |       }
    |       return map
    |    }
    |    
    |    override fun getBundleOfParamsForFirebase(): Bundle {
    |       val bundle = Bundle().apply {
    |       $bundleEntries
    |       }
    |       return bundle
    |    }
    |}
    """.trimMargin().toByteArray()
            )
        }
    }
}

fun KSValueParameter.isNotKotlinPrimitive(): Boolean {

    return when (type.element?.toString()) {
        "String", "Int", "Short", "Number", "Boolean", "Byte", "Char", "Float", "Double", "Long", "Unit", "Any" -> false
        else -> true
    }
}

fun KSValueParameter.getPrimitiveTypeName(): String {

    return type.element?.toString() ?: throw IllegalAccessException()
}
