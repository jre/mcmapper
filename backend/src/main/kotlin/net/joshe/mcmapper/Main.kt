package net.joshe.mcmapper

import java.io.File
import kotlin.system.exitProcess

/*
  TODO
  read spawn point coords and write to world.json
  don't rewrite files if they didn't change
 */

fun main(args: Array<String>) {
    if (args.size != 2) {
        System.err.println("usage: /path/to/json-config /path/to/output-directory")
        exitProcess(1)
    }
    println("reading config from ${args[0]} and writing data to ${args[1]}")
    convertAllWorlds(readWorldsConf(File(args[0]).absoluteFile), File(args[1]).absoluteFile)
}
