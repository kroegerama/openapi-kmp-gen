package com.kroegerama.openapi.kmp.gen.generator

import com.kroegerama.openapi.kmp.gen.Logger
import com.kroegerama.openapi.kmp.gen.OptionSet
import com.kroegerama.openapi.kmp.gen.poet.PoetGenerator
import com.kroegerama.openapi.kmp.gen.spec.SpecConverter
import com.kroegerama.openapi.kmp.gen.spec.SpecModel
import com.kroegerama.openapi.kmp.gen.spec.SpecParser

class Generator(
    private val options: OptionSet,
    private val logger: Logger
) {

    fun generate() {
        logger.lifecycle("selected options: $options")

        val fileHelper = FileHelper(
            options = options,
            logger = logger
        )
        val spec = SpecParser(
            specFile = options.specFile,
            options = options,
            logger = logger
        ).parseAndResolve()

        val specModel = SpecConverter(
            spec = spec,
            options = options
        ).convert()

        printSpecModel(specModel)

        fileHelper.prepare()

        val files = PoetGenerator(
            specModel = specModel,
            options = options
        ).createFileSpecs()

        files.forEach(fileHelper::writeFileSpec)
    }

    private fun printSpecModel(specModel: SpecModel) {
        logger.info("# Metadata #")
        logger.info(specModel.metadata.toString())

        logger.info("# SecuritySchemes #")
        if (specModel.securitySchemes.isEmpty()) {
            logger.info("\t(none)")
        }
        specModel.securitySchemes.forEach { securityScheme ->
            logger.info("\t- $securityScheme")
        }

        logger.info("# APIs #")
        if (specModel.apis.isEmpty()) {
            logger.info("\t(none)")
        }
        specModel.apis.forEach { api ->
            logger.info(api.name)
            api.operations.forEach { operation ->
                logger.info("\t- $operation")
            }
        }

        logger.info("# Schemas #")
        if (specModel.schemas.isEmpty()) {
            logger.info("\t(none)")
        }
        specModel.schemas.forEach { schema ->
            logger.info("\t- $schema")
        }
    }
}
