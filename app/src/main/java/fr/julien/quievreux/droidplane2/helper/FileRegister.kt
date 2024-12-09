package fr.julien.quievreux.droidplane2.helper

import java.io.File

interface FileRegister {
    fun registerFile(file: File)
    fun getfilesDir(): String
}