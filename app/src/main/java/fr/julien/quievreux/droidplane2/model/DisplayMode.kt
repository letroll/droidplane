package fr.julien.quievreux.droidplane2.model

/**
 * Defines the presentation mode for viewing a mindmap document.
 */
enum class DisplayMode {
    /**
     * Classic hierarchical linear list of child nodes (NodeList view).
     */
    LIST,

    /**
     * 2D spatial mindmap canvas layout similar to desktop Freeplane.
     */
    MIND_MAP,
}
