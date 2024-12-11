package fr.julien.quievreux.droidplane2.data.model

enum class RichContentType(val text:String) {
    DETAILS("DETAILS"),
    NODE("NODE"),
    NOTE("NOTE");

   companion object{
       fun fromString(text: String): RichContentType? = entries.firstOrNull { it.text == text }
   }
}