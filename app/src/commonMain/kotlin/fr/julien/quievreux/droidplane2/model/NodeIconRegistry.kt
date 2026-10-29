package fr.julien.quievreux.droidplane2.model

import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Square
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.ArrowUp
import compose.icons.fontawesomeicons.solid.Bookmark
import compose.icons.fontawesomeicons.solid.Edit
import compose.icons.fontawesomeicons.solid.HatWizard
import compose.icons.fontawesomeicons.solid.Image
import compose.icons.fontawesomeicons.solid.Link
import compose.icons.fontawesomeicons.solid.List
import compose.icons.fontawesomeicons.solid.MailBulk
import compose.icons.fontawesomeicons.solid.Paperclip
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Redo
import compose.icons.fontawesomeicons.solid.Video
import compose.icons.fontawesomeicons.solid.VolumeUp
import compose.icons.tablericons.Circle0
import compose.icons.tablericons.Circle1
import compose.icons.tablericons.Circle2
import compose.icons.tablericons.Circle3
import compose.icons.tablericons.Circle4
import compose.icons.tablericons.Circle5
import compose.icons.tablericons.Circle6
import compose.icons.tablericons.Circle7
import compose.icons.tablericons.Circle8
import compose.icons.tablericons.Circle9
import fr.julien.quievreux.droidplane2.res.AppRes as R

/**
 * High-performance compile-time registry for Freeplane node icons.
 * Resolves Freeplane icon identifiers (with case, dash, and underscore normalization)
 * to visual representations, and provides a categorized, searchable catalog.
 */
object NodeIconRegistry {

    private fun normalizeKey(name: String): String =
        name.trim().lowercase().replace('-', '_')

    // Comprehensive catalog of all available icons in Droidplane
    private val ALL_DESCRIPTORS: List<NodeIconDescriptor> = listOf(
        // === STATUS & FLAGS ===
        NodeIconDescriptor("yes", NodeIconVisual.Resource(R.drawable.icon_yes), "Yes", NodeIconCategory.STATUS),
        NodeIconDescriptor("button_ok", NodeIconVisual.Resource(R.drawable.icon_button_ok), "OK", NodeIconCategory.STATUS, listOf("button-ok")),
        NodeIconDescriptor("button_cancel", NodeIconVisual.Resource(R.drawable.icon_button_cancel), "Cancel", NodeIconCategory.STATUS, listOf("button-cancel")),
        NodeIconDescriptor("stop", NodeIconVisual.Resource(R.drawable.icon_stop), "Stop", NodeIconCategory.STATUS),
        NodeIconDescriptor("stop_sign", NodeIconVisual.Resource(R.drawable.icon_stop_sign), "Stop Sign", NodeIconCategory.STATUS, listOf("stop-sign")),
        NodeIconDescriptor("checked", NodeIconVisual.Resource(R.drawable.icon_checked), "Checked", NodeIconCategory.STATUS, listOf("check")),
        NodeIconDescriptor("unchecked", NodeIconVisual.Vector(FontAwesomeIcons.Regular.Square), "Unchecked", NodeIconCategory.STATUS),
        NodeIconDescriptor("flag-red", NodeIconVisual.Resource(R.drawable.icon_flag), "Red Flag", NodeIconCategory.STATUS, listOf("flag", "flag_red")),
        NodeIconDescriptor("flag-black", NodeIconVisual.Resource(R.drawable.icon_flag_black), "Black Flag", NodeIconCategory.STATUS, listOf("flag_black")),
        NodeIconDescriptor("flag-blue", NodeIconVisual.Resource(R.drawable.icon_flag_blue), "Blue Flag", NodeIconCategory.STATUS, listOf("flag_blue")),
        NodeIconDescriptor("flag-green", NodeIconVisual.Resource(R.drawable.icon_flag_green), "Green Flag", NodeIconCategory.STATUS, listOf("flag_green")),
        NodeIconDescriptor("flag-orange", NodeIconVisual.Resource(R.drawable.icon_flag_orange), "Orange Flag", NodeIconCategory.STATUS, listOf("flag_orange")),
        NodeIconDescriptor("flag-pink", NodeIconVisual.Resource(R.drawable.icon_flag_pink), "Pink Flag", NodeIconCategory.STATUS, listOf("flag_pink")),
        NodeIconDescriptor("flag-yellow", NodeIconVisual.Resource(R.drawable.icon_flag_yellow), "Yellow Flag", NodeIconCategory.STATUS, listOf("flag_yellow")),
        NodeIconDescriptor("closed", NodeIconVisual.Resource(R.drawable.icon_closed), "Closed", NodeIconCategory.STATUS),
        NodeIconDescriptor("encrypted", NodeIconVisual.Resource(R.drawable.icon_encrypted), "Encrypted", NodeIconCategory.STATUS),
        NodeIconDescriptor("decrypted", NodeIconVisual.Resource(R.drawable.icon_decrypted), "Decrypted", NodeIconCategory.STATUS),
        NodeIconDescriptor("password", NodeIconVisual.Resource(R.drawable.icon_password), "Password", NodeIconCategory.STATUS),

        // === PRIORITIES & NUMBERS ===
        NodeIconDescriptor("full-0", NodeIconVisual.Vector(TablerIcons.Circle0), "Number 0", NodeIconCategory.PRIORITY, listOf("full_0", "priority-0", "priority_0")),
        NodeIconDescriptor("full-1", NodeIconVisual.Vector(TablerIcons.Circle1), "Priority 1", NodeIconCategory.PRIORITY, listOf("full_1", "priority-1", "priority_1")),
        NodeIconDescriptor("full-2", NodeIconVisual.Vector(TablerIcons.Circle2), "Priority 2", NodeIconCategory.PRIORITY, listOf("full_2", "priority-2", "priority_2")),
        NodeIconDescriptor("full-3", NodeIconVisual.Vector(TablerIcons.Circle3), "Priority 3", NodeIconCategory.PRIORITY, listOf("full_3", "priority-3", "priority_3")),
        NodeIconDescriptor("full-4", NodeIconVisual.Vector(TablerIcons.Circle4), "Priority 4", NodeIconCategory.PRIORITY, listOf("full_4", "priority-4", "priority_4")),
        NodeIconDescriptor("full-5", NodeIconVisual.Vector(TablerIcons.Circle5), "Priority 5", NodeIconCategory.PRIORITY, listOf("full_5", "priority-5", "priority_5")),
        NodeIconDescriptor("full-6", NodeIconVisual.Vector(TablerIcons.Circle6), "Priority 6", NodeIconCategory.PRIORITY, listOf("full_6", "priority-6", "priority_6")),
        NodeIconDescriptor("full-7", NodeIconVisual.Vector(TablerIcons.Circle7), "Priority 7", NodeIconCategory.PRIORITY, listOf("full_7", "priority-7", "priority_7")),
        NodeIconDescriptor("full-8", NodeIconVisual.Vector(TablerIcons.Circle8), "Priority 8", NodeIconCategory.PRIORITY, listOf("full_8", "priority-8", "priority_8")),
        NodeIconDescriptor("full-9", NodeIconVisual.Vector(TablerIcons.Circle9), "Priority 9", NodeIconCategory.PRIORITY, listOf("full_9", "priority-9", "priority_9")),
        NodeIconDescriptor("0", NodeIconVisual.Resource(R.drawable.icon_0), "0%", NodeIconCategory.PRIORITY, listOf("0%")),
        NodeIconDescriptor("25", NodeIconVisual.Resource(R.drawable.icon_25), "25%", NodeIconCategory.PRIORITY, listOf("25%")),
        NodeIconDescriptor("50", NodeIconVisual.Resource(R.drawable.icon_50), "50%", NodeIconCategory.PRIORITY, listOf("50%")),
        NodeIconDescriptor("75", NodeIconVisual.Resource(R.drawable.icon_75), "75%", NodeIconCategory.PRIORITY, listOf("75%")),
        NodeIconDescriptor("100", NodeIconVisual.Resource(R.drawable.icon_100), "100%", NodeIconCategory.PRIORITY, listOf("100%")),

        // === EMOTIONS ===
        NodeIconDescriptor("very_positive", NodeIconVisual.Resource(R.drawable.icon_very_positive), "Very Positive", NodeIconCategory.EMOTION, listOf("very-positive")),
        NodeIconDescriptor("positive", NodeIconVisual.Resource(R.drawable.icon_positive), "Positive", NodeIconCategory.EMOTION),
        NodeIconDescriptor("neutral", NodeIconVisual.Resource(R.drawable.icon_neutral), "Neutral", NodeIconCategory.EMOTION),
        NodeIconDescriptor("negative", NodeIconVisual.Resource(R.drawable.icon_negative), "Negative", NodeIconCategory.EMOTION),
        NodeIconDescriptor("very_negative", NodeIconVisual.Resource(R.drawable.icon_very_negative), "Very Negative", NodeIconCategory.EMOTION, listOf("very-negative")),
        NodeIconDescriptor("smiley_neutral", NodeIconVisual.Resource(R.drawable.icon_smiley_neutral), "Smiley Neutral", NodeIconCategory.EMOTION, listOf("smiley-neutral")),
        NodeIconDescriptor("smiley_oh", NodeIconVisual.Resource(R.drawable.icon_smiley_oh), "Smiley Surprised", NodeIconCategory.EMOTION, listOf("smiley-oh")),
        NodeIconDescriptor("smiley_angry", NodeIconVisual.Resource(R.drawable.icon_smiley_angry), "Smiley Angry", NodeIconCategory.EMOTION, listOf("smiley-angry")),
        NodeIconDescriptor("smily_bad", NodeIconVisual.Resource(R.drawable.icon_smily_bad), "Smiley Bad", NodeIconCategory.EMOTION, listOf("smily-bad", "smiley-bad", "smiley_bad")),
        NodeIconDescriptor("ksmiletris", NodeIconVisual.Resource(R.drawable.icon_ksmiletris), "Smiletris", NodeIconCategory.EMOTION),

        // === ACTIONS & TOOLS ===
        NodeIconDescriptor("pencil", NodeIconVisual.Resource(R.drawable.icon_pencil), "Pencil", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("edit", NodeIconVisual.Resource(R.drawable.icon_edit), "Edit", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("attach", NodeIconVisual.Vector(FontAwesomeIcons.Solid.Paperclip), "Attachment", NodeIconCategory.ACTIONS_TOOLS, listOf("paperclip")),
        NodeIconDescriptor("link", NodeIconVisual.Vector(FontAwesomeIcons.Solid.Link), "Link", NodeIconCategory.ACTIONS_TOOLS, listOf("hyperlink")),
        NodeIconDescriptor("bookmark", NodeIconVisual.Vector(FontAwesomeIcons.Solid.Bookmark), "Bookmark", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("help", NodeIconVisual.Resource(R.drawable.icon_help), "Help", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("idea", NodeIconVisual.Resource(R.drawable.icon_idea), "Idea", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("info", NodeIconVisual.Resource(R.drawable.icon_info), "Info", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("messagebox_warning", NodeIconVisual.Resource(R.drawable.icon_messagebox_warning), "Warning", NodeIconCategory.ACTIONS_TOOLS, listOf("messagebox-warning", "warning")),
        NodeIconDescriptor("knotify", NodeIconVisual.Resource(R.drawable.icon_knotify), "Notification", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("calendar", NodeIconVisual.Resource(R.drawable.icon_calendar), "Calendar", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("clock", NodeIconVisual.Resource(R.drawable.icon_clock), "Clock", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("clock2", NodeIconVisual.Resource(R.drawable.icon_clock2), "Clock 2", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("hourglass", NodeIconVisual.Resource(R.drawable.icon_hourglass), "Hourglass", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("bell", NodeIconVisual.Resource(R.drawable.icon_bell), "Bell", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("addition", NodeIconVisual.Vector(FontAwesomeIcons.Solid.Plus), "Add / Plus", NodeIconCategory.ACTIONS_TOOLS, listOf("plus")),
        NodeIconDescriptor("subtraction", NodeIconVisual.Resource(R.drawable.icon_subtraction), "Subtract / Minus", NodeIconCategory.ACTIONS_TOOLS, listOf("minus")),
        NodeIconDescriptor("multiplication", NodeIconVisual.Resource(R.drawable.icon_multiplication), "Multiply", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("division", NodeIconVisual.Resource(R.drawable.icon_division), "Divide", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("broken_line", NodeIconVisual.Resource(R.drawable.icon_broken_line), "Broken Line", NodeIconCategory.ACTIONS_TOOLS, listOf("broken-line")),
        NodeIconDescriptor("down", NodeIconVisual.Resource(R.drawable.icon_down), "Down", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("up", NodeIconVisual.Vector(FontAwesomeIcons.Solid.ArrowUp), "Up", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("back", NodeIconVisual.Vector(FontAwesomeIcons.Solid.ArrowLeft), "Back", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("forward", NodeIconVisual.Resource(R.drawable.icon_forward), "Forward", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("go", NodeIconVisual.Resource(R.drawable.icon_go), "Go", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("gohome", NodeIconVisual.Resource(R.drawable.icon_gohome), "Go Home", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("launch", NodeIconVisual.Resource(R.drawable.icon_launch), "Launch", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("redo", NodeIconVisual.Vector(FontAwesomeIcons.Solid.Redo), "Redo", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("prepare", NodeIconVisual.Resource(R.drawable.icon_prepare), "Prepare", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("revision", NodeIconVisual.Resource(R.drawable.icon_revision), "Revision", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("xmag", NodeIconVisual.Resource(R.drawable.icon_xmag), "Magnifier", NodeIconCategory.ACTIONS_TOOLS, listOf("search", "zoom")),
        NodeIconDescriptor("audio", NodeIconVisual.Vector(FontAwesomeIcons.Solid.VolumeUp), "Audio", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("video", NodeIconVisual.Vector(FontAwesomeIcons.Solid.Video), "Video", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("wizard", NodeIconVisual.Vector(FontAwesomeIcons.Solid.HatWizard), "Wizard", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("list", NodeIconVisual.Vector(FontAwesomeIcons.Solid.List), "List", NodeIconCategory.ACTIONS_TOOLS),
        NodeIconDescriptor("button", NodeIconVisual.Resource(R.drawable.icon_button), "Button", NodeIconCategory.ACTIONS_TOOLS),

        // === PEOPLE & OBJECTS ===
        NodeIconDescriptor("family", NodeIconVisual.Resource(R.drawable.icon_family), "Family", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("group", NodeIconVisual.Resource(R.drawable.icon_group), "Group", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("fema", NodeIconVisual.Resource(R.drawable.icon_fema), "Female Sign", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("female1", NodeIconVisual.Resource(R.drawable.icon_female1), "Female 1", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("female2", NodeIconVisual.Resource(R.drawable.icon_female2), "Female 2", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("females", NodeIconVisual.Resource(R.drawable.icon_females), "Females", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("male1", NodeIconVisual.Resource(R.drawable.icon_male1), "Male 1", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("male2", NodeIconVisual.Resource(R.drawable.icon_male2), "Male 2", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("males", NodeIconVisual.Resource(R.drawable.icon_males), "Males", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("user_icon", NodeIconVisual.Resource(R.drawable.icon_user_icon), "User", NodeIconCategory.PEOPLE_OBJECTS, listOf("user-icon", "user")),
        NodeIconDescriptor("penguin", NodeIconVisual.Resource(R.drawable.icon_penguin), "Penguin", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("bee", NodeIconVisual.Resource(R.drawable.icon_bee), "Bee", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("freemind_butterfly", NodeIconVisual.Resource(R.drawable.icon_freemind_butterfly), "Butterfly", NodeIconCategory.PEOPLE_OBJECTS, listOf("butterfly", "freemind-butterfly")),
        NodeIconDescriptor("folder", NodeIconVisual.Resource(R.drawable.icon_folder), "Folder", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("kaddressbook", NodeIconVisual.Resource(R.drawable.icon_kaddressbook), "Address Book", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("kmail", NodeIconVisual.Resource(R.drawable.icon_kmail), "Mail (App)", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("mail", NodeIconVisual.Vector(FontAwesomeIcons.Solid.MailBulk), "Mail", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("internet", NodeIconVisual.Resource(R.drawable.icon_internet), "Internet", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("internet_warning", NodeIconVisual.Resource(R.drawable.icon_internet_warning), "Internet Warning", NodeIconCategory.PEOPLE_OBJECTS, listOf("internet-warning")),
        NodeIconDescriptor("desktop_new", NodeIconVisual.Resource(R.drawable.icon_desktop_new), "Desktop", NodeIconCategory.PEOPLE_OBJECTS, listOf("desktop-new")),
        NodeIconDescriptor("executable", NodeIconVisual.Resource(R.drawable.icon_executable), "Executable", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("clanbomber", NodeIconVisual.Resource(R.drawable.icon_clanbomber), "Bomb", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("korn", NodeIconVisual.Resource(R.drawable.icon_korn), "Korn", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("licq", NodeIconVisual.Resource(R.drawable.icon_licq), "Licq", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("mindmap", NodeIconVisual.Resource(R.drawable.icon_mindmap), "Mindmap", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("narrative", NodeIconVisual.Resource(R.drawable.icon_narrative), "Narrative", NodeIconCategory.PEOPLE_OBJECTS),
        NodeIconDescriptor("image", NodeIconVisual.Vector(FontAwesomeIcons.Solid.Image), "Image", NodeIconCategory.PEOPLE_OBJECTS)
    )

    // Lookup index mapping normalized name -> NodeIconVisual
    private val LOOKUP_MAP: Map<String, NodeIconVisual> = buildMap {
        ALL_DESCRIPTORS.forEach { desc ->
            put(normalizeKey(desc.id), desc.visual)
            desc.aliases.forEach { alias ->
                put(normalizeKey(alias), desc.visual)
            }
        }
    }

    /**
     * Resolves a raw Freeplane icon name into its visual representation.
     * Returns NodeIconVisual.Unmapped(iconName) if no match is found.
     */
    fun resolve(iconName: String): NodeIconVisual {
        if (iconName.isBlank()) return NodeIconVisual.Unmapped(iconName)
        val key = normalizeKey(iconName)
        return LOOKUP_MAP[key] ?: NodeIconVisual.Unmapped(iconName)
    }

    /**
     * Returns the full catalog of available icons.
     */
    fun getAllIcons(): List<NodeIconDescriptor> = ALL_DESCRIPTORS

    /**
     * Filters the catalog by category and text search query (case-insensitive on id, label, aliases).
     */
    fun filterIcons(
        query: String = "",
        category: NodeIconCategory = NodeIconCategory.ALL
    ): List<NodeIconDescriptor> {
        val trimmedQuery = query.trim().lowercase()
        return ALL_DESCRIPTORS.filter { desc ->
            val matchesCategory = (category == NodeIconCategory.ALL || desc.category == category)
            val matchesQuery = trimmedQuery.isEmpty() ||
                desc.id.lowercase().contains(trimmedQuery) ||
                desc.label.lowercase().contains(trimmedQuery) ||
                desc.aliases.any { it.lowercase().contains(trimmedQuery) }
            matchesCategory && matchesQuery
        }
    }
}
