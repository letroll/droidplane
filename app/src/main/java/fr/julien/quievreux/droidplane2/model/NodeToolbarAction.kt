package fr.julien.quievreux.droidplane2.model

/**
 * Constants for node toolbar actions.
 * Using constants instead of string literals for consistency and maintainability.
 */
object NodeToolbarAction {
    const val PROPERTIES = "properties"
    const val OPEN_LINK = "open_link"
    const val ADD_CHILD = "add_child"
    const val DELETE = "delete"

    val ALL_ACTIONS = listOf(PROPERTIES, OPEN_LINK, ADD_CHILD, DELETE)
}