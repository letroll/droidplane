package fr.julien.quievreux.droidplane2.core.log

interface Logger {
    fun e(message: String)
    fun e(tag: String, message: String)

    fun w(message: String)
    fun w(tag: String, message: String)

    fun d(message: String)
    fun d(tag: String, message: String)

    companion object {
        val Default: Logger = object : Logger {
            override fun e(message: String) { println("[ERROR] [Droidplane] $message") }
            override fun e(tag: String, message: String) { println("[ERROR] [$tag] $message") }
            override fun w(message: String) { println("[WARN] [Droidplane] $message") }
            override fun w(tag: String, message: String) { println("[WARN] [$tag] $message") }
            override fun d(message: String) { println("[DEBUG] [Droidplane] $message") }
            override fun d(tag: String, message: String) { println("[DEBUG] [$tag] $message") }
        }
    }
}
