package fr.julien.quievreux.droidplane2.core.log

class JvmLogger : Logger {
    override fun e(message: String) {
        System.err.println("[ERROR] [droidplane2] $message")
    }

    override fun e(tag: String, message: String) {
        System.err.println("[ERROR] [$tag] $message")
    }

    override fun w(message: String) {
        println("[WARN] [droidplane2] $message")
    }

    override fun w(tag: String, message: String) {
        println("[WARN] [$tag] $message")
    }

    override fun d(message: String) {
        println("[DEBUG] [droidplane2] $message")
    }

    override fun d(tag: String, message: String) {
        println("[DEBUG] [$tag] $message")
    }
}
