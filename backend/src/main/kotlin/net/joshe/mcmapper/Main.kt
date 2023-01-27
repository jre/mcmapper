package net.joshe.mcmapper

import java.io.File
import kotlin.io.path.name
import kotlin.io.path.toPath
import kotlin.system.exitProcess

/*
  TODO
  read spawn point coords and write to world.json
  don't rewrite files if they didn't change
 */

fun usage() {
    val jar = MainKludge.getMainClass().protectionDomain.codeSource.location.toURI().toPath().name
    System.err.println("usage: ${jar} convert-map JSON-PATH OUTPUT-PATH")
    exitProcess(1)
}

fun main(args: Array<String>) {
    when (args.getOrNull(0)) {
        "convert-map" -> {
            if (args.size != 3)
                usage()
            println("reading config from ${args[1]} and writing data to ${args[2]}")
            convertAllWorlds(readWorldsConf(File(args[1]).absoluteFile), File(args[2]).absoluteFile)
        }
        else -> usage()
    }
}
