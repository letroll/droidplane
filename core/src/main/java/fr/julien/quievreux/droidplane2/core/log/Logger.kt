package fr.julien.quievreux.droidplane2.core.log

interface Logger {
    fun e(message: String)
    fun e(tag:String, message: String)

    fun d(message: String)
    fun d(tag:String, message: String)
}