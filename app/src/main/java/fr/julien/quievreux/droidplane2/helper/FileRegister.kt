package fr.julien.quievreux.droidplane2.helper

import android.net.Uri
import java.io.File

interface FileRegister {
    fun registerFile(file: File)
    fun registerFile(file: File, initialUri: Uri?) = registerFile(file)
    fun getfilesDir(): String
}