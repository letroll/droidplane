package fr.julien.quievreux.droidplane2.res

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import java.util.Locale

object AppRes {
    object string {
        const val app_name = 1001
        const val menu_settings = 1002
        const val title_activity_main = 1003
        const val up = 1004
        const val gototop = 1005
        const val yes = 1006
        const val ok = 1007
        const val cancel = 1008
        const val submit = 1009
        const val filenotfound = 1010
        const val noicon = 1011
        const val novalidfile = 1012
        const val copynodetext = 1013
        const val editnodetext = 1014
        const val openlink = 1015
        const val openrichtext = 1016
        const val help = 1017
        const val save = 1018
        const val add = 1019
        const val add_brother = 1020
        const val add_child = 1021
        const val title_activity_rich_text_view = 1022
        const val mindmap_loading = 1023
        const val search = 1024
        const val search_next = 1025
        const val search_prev = 1026
        const val open = 1027
        const val edit = 1028
        const val node_information = 1029
        const val add_child_node = 1030
        const val open_link = 1031
        const val delete = 1032
        const val undo = 1033
        const val switch_to_mindmap_view = 1034
        const val new_mindmap = 1035
        const val help_demo = 1036
        const val discard_changes_title = 1037
        const val discard_changes_message = 1038
        const val discard = 1039
        const val overwrite_file_title = 1040
        const val overwrite_file_message = 1041
        const val overwrite_replace = 1042
        const val default_root_node_title = 1043
        const val startup_title = 1044
        const val recent_files = 1045
        const val browse_files = 1046
        const val no_recent_files = 1047
        const val recent_files_dialog_title = 1048
        const val exit_app = 1049
        const val menu_recent_files = 1050
        const val node_properties = 1051
        const val save_properties = 1052
        const val tab_content = 1053
        const val tab_attributes = 1054
        const val tab_styling = 1055
        const val tab_edges_connectors = 1056
        const val tab_icons_media = 1057
        const val tab_scripts = 1058
        const val node_title = 1059
        const val details = 1060
        const val notes = 1061
        const val latex_formula = 1062
        const val rich_text = 1063
        const val plain_text = 1064
        const val preview_mode = 1065
        const val source_mode = 1066
        const val attributes = 1067
        const val attribute_name = 1068
        const val attribute_value = 1069
        const val add_attribute = 1070
        const val delete_attribute = 1071
        const val hyperlink = 1072
        const val styling = 1073
        const val text_color = 1074
        const val background_color = 1075
        const val node_shape = 1076
        const val shape_fork = 1077
        const val shape_bubble = 1078
        const val shape_oval = 1079
        const val shape_rectangle = 1080
        const val font_size = 1081
        const val bold = 1082
        const val italic = 1083
        const val cloud_grouping = 1084
        const val cloud_enabled = 1085
        const val cloud_color = 1086
        const val cloud_shape = 1087
        const val move_cloud_to_parent = 1088
        const val absorb_child_clouds = 1089
        const val enclosed_in_parent_cloud = 1090
        const val edge_styling = 1091
        const val edge_style = 1092
        const val edge_width = 1093
        const val edge_color = 1094
        const val connectors = 1095
        const val add_connector = 1096
        const val destination_node = 1097
        const val connector_color = 1098
        const val source_label = 1099
        const val middle_label = 1100
        const val target_label = 1101
        const val icons = 1102
        const val assigned_icons = 1103
        const val no_assigned_icons = 1104
        const val clear_all_icons = 1105
        const val remove_icon = 1106
        const val move_icon_left = 1107
        const val move_icon_right = 1108
        const val icon_catalog = 1109
        const val search_icons_placeholder = 1110
        const val category_all = 1111
        const val category_status = 1112
        const val category_priority = 1113
        const val category_emotion = 1114
        const val category_actions_tools = 1115
        const val category_people_objects = 1116
        const val external_object = 1117
        const val image_uri = 1118
        const val image_scale = 1119
        const val scripts_hooks = 1120
        const val script_content = 1121
        const val no_scripts = 1122
        const val check_for_updates = 1123
        const val checking_for_updates = 1124
        const val update_available_title = 1125
        const val update_versions = 1126
        const val update_changelog_title = 1127
        const val update_download_install = 1128
        const val update_view_on_github = 1129
        const val update_later = 1130
        const val update_up_to_date = 1131
        const val update_error = 1132
        const val update_downloading_enqueued = 1133
        const val update_no_apk_asset = 1134
        const val update_no_changelog = 1135
    }

    object drawable {
        const val ic_launcher = 2000
        const val icon_0 = 2001
        const val icon_100 = 2002
        const val icon_25 = 2003
        const val icon_50 = 2004
        const val icon_75 = 2005
        const val icon_bee = 2006
        const val icon_bell = 2007
        const val icon_broken_line = 2008
        const val icon_button = 2009
        const val icon_button_cancel = 2010
        const val icon_button_ok = 2011
        const val icon_calendar = 2012
        const val icon_checked = 2013
        const val icon_clanbomber = 2014
        const val icon_clock = 2015
        const val icon_clock2 = 2016
        const val icon_closed = 2017
        const val icon_decrypted = 2018
        const val icon_desktop_new = 2019
        const val icon_division = 2020
        const val icon_down = 2021
        const val icon_edit = 2022
        const val icon_encrypted = 2023
        const val icon_executable = 2024
        const val icon_family = 2025
        const val icon_fema = 2026
        const val icon_female1 = 2027
        const val icon_female2 = 2028
        const val icon_females = 2029
        const val icon_flag = 2030
        const val icon_flag_black = 2031
        const val icon_flag_blue = 2032
        const val icon_flag_green = 2033
        const val icon_flag_orange = 2034
        const val icon_flag_pink = 2035
        const val icon_flag_yellow = 2036
        const val icon_folder = 2037
        const val icon_forward = 2038
        const val icon_freemind_butterfly = 2039
        const val icon_go = 2040
        const val icon_gohome = 2041
        const val icon_group = 2042
        const val icon_help = 2043
        const val icon_hourglass = 2044
        const val icon_idea = 2045
        const val icon_info = 2046
        const val icon_internet = 2047
        const val icon_internet_warning = 2048
        const val icon_kaddressbook = 2049
        const val icon_kmail = 2050
        const val icon_knotify = 2051
        const val icon_korn = 2052
        const val icon_ksmiletris = 2053
        const val icon_launch = 2054
        const val icon_licq = 2055
        const val icon_male1 = 2056
        const val icon_male2 = 2057
        const val icon_males = 2058
        const val icon_messagebox_warning = 2059
        const val icon_mindmap = 2060
        const val icon_multiplication = 2061
        const val icon_narrative = 2062
        const val icon_negative = 2063
        const val icon_neutral = 2064
        const val icon_password = 2065
        const val icon_pencil = 2066
        const val icon_penguin = 2067
        const val icon_positive = 2068
        const val icon_prepare = 2069
        const val icon_revision = 2070
        const val icon_smiley_angry = 2071
        const val icon_smiley_neutral = 2072
        const val icon_smiley_oh = 2073
        const val icon_smily_bad = 2074
        const val icon_stop = 2075
        const val icon_stop_sign = 2076
        const val icon_subtraction = 2077
        const val icon_user_icon = 2078
        const val icon_very_negative = 2079
        const val icon_very_positive = 2080
        const val icon_xmag = 2081
        const val icon_yes = 2082
        const val minus_alt = 2083
        const val plus_alt = 2084
    }

    private val enStrings: Map<Int, String> = mapOf(
        string.app_name to "DroidPlane2",
        string.menu_settings to "Settings",
        string.title_activity_main to "MainActivity",
        string.up to "Up",
        string.gototop to "Top",
        string.yes to "Yes",
        string.ok to "OK",
        string.cancel to "Cancel",
        string.submit to "Submit",
        string.filenotfound to "Sorry, I could not find the file you wanted to open!",
        string.noicon to "no icon",
        string.novalidfile to "Please start DroidPlane by opening a *.mm file\"",
        string.copynodetext to "Copy Node Text",
        string.editnodetext to "Edit Node Text",
        string.openlink to "Open Link",
        string.openrichtext to "Open Rich Text Content",
        string.help to "Help",
        string.save to "Save",
        string.add to "Add",
        string.add_brother to "Add brother",
        string.add_child to "Add child",
        string.title_activity_rich_text_view to "Rich Text View",
        string.mindmap_loading to "Loading",
        string.search to "Search",
        string.search_next to "Search Next",
        string.search_prev to "Search Prev.",
        string.open to "Open",
        string.edit to "Edit",
        string.node_information to "Created : %1\$s Modified : %2\$s",
        string.add_child_node to "Add Child Node",
        string.open_link to "Open Link",
        string.delete to "Delete",
        string.undo to "Undo",
        string.switch_to_mindmap_view to "Mind Map View",
        string.new_mindmap to "New Mindmap",
        string.help_demo to "Help & Demo",
        string.discard_changes_title to "Discard Changes?",
        string.discard_changes_message to "You have unsaved changes that will be lost. Do you want to proceed?",
        string.discard to "Discard",
        string.overwrite_file_title to "Replace Existing File?",
        string.overwrite_file_message to "Do you want to replace the existing file \"%1\$s\"?",
        string.overwrite_replace to "Replace",
        string.default_root_node_title to "Central Idea",
        string.startup_title to "Welcome to DroidPlane",
        string.recent_files to "Recent Files",
        string.browse_files to "Browse other file…",
        string.no_recent_files to "No recent files found",
        string.recent_files_dialog_title to "Recent Files",
        string.exit_app to "Exit App",
        string.menu_recent_files to "Recent Files",
        string.node_properties to "Properties",
        string.save_properties to "Save Properties",
        string.tab_content to "Content",
        string.tab_attributes to "Attributes",
        string.tab_styling to "Styling",
        string.tab_edges_connectors to "Connectors",
        string.tab_icons_media to "Media",
        string.tab_scripts to "Scripts",
        string.node_title to "Title",
        string.details to "Details",
        string.notes to "Notes",
        string.latex_formula to "LaTeX Formula",
        string.rich_text to "Rich Text",
        string.plain_text to "Plain Text",
        string.preview_mode to "Preview",
        string.source_mode to "Source",
        string.attributes to "Attributes",
        string.attribute_name to "Name",
        string.attribute_value to "Value",
        string.add_attribute to "Add Attribute",
        string.delete_attribute to "Delete Attribute",
        string.hyperlink to "Hyperlink / Internal Link",
        string.styling to "Styling",
        string.text_color to "Text Color",
        string.background_color to "Background Color",
        string.node_shape to "Shape",
        string.shape_fork to "Fork",
        string.shape_bubble to "Bubble",
        string.shape_oval to "Oval",
        string.shape_rectangle to "Rectangle",
        string.font_size to "Font Size",
        string.bold to "Bold",
        string.italic to "Italic",
        string.cloud_grouping to "Cloud Grouping",
        string.cloud_enabled to "Enable Cloud",
        string.cloud_color to "Cloud Color",
        string.cloud_shape to "Cloud Shape",
        string.move_cloud_to_parent to "Extend Cloud to Parent",
        string.absorb_child_clouds to "Enclose in Common Cloud",
        string.enclosed_in_parent_cloud to "Enclosed in parent cloud (%1\$s)",
        string.edge_styling to "Branch Edge",
        string.edge_style to "Line Style",
        string.edge_width to "Line Width",
        string.edge_color to "Edge Color",
        string.connectors to "Connectors (Arrow Links)",
        string.add_connector to "Add Connector",
        string.destination_node to "Destination Node ID",
        string.connector_color to "Connector Color",
        string.source_label to "Source Label",
        string.middle_label to "Middle Label",
        string.target_label to "Target Label",
        string.icons to "Icons",
        string.assigned_icons to "Assigned Icons",
        string.no_assigned_icons to "No icons assigned to this node.",
        string.clear_all_icons to "Clear All",
        string.remove_icon to "Remove %1\$s",
        string.move_icon_left to "Move earlier",
        string.move_icon_right to "Move later",
        string.icon_catalog to "Icon Catalog",
        string.search_icons_placeholder to "Search icons…",
        string.category_all to "All",
        string.category_status to "Status & Flags",
        string.category_priority to "Priorities & Numbers",
        string.category_emotion to "Emoticons",
        string.category_actions_tools to "Actions & Tools",
        string.category_people_objects to "People & Objects",
        string.external_object to "External Image / Media",
        string.image_uri to "URI",
        string.image_scale to "Scale Factor",
        string.scripts_hooks to "Script Hooks & Add-ons",
        string.script_content to "Script Content",
        string.no_scripts to "No embedded script hooks found on this node.",
        string.check_for_updates to "Check for updates",
        string.checking_for_updates to "Checking for updates…",
        string.update_available_title to "Update Available: %1\$s",
        string.update_versions to "Current: %1\$s • New: %2\$s",
        string.update_changelog_title to "What's new:",
        string.update_download_install to "Download & Install",
        string.update_view_on_github to "View on GitHub",
        string.update_later to "Later",
        string.update_up_to_date to "You are using the latest version (%1\$s).",
        string.update_error to "Unable to check for updates. Please check your connection.",
        string.update_downloading_enqueued to "Download started. Tap the system notification once complete to install.",
        string.update_no_apk_asset to "No APK file found in this release. Opening GitHub in browser…",
        string.update_no_changelog to "No changelog provided for this release."
    )

    private val frStrings: Map<Int, String> = mapOf(
        string.app_name to "DroidPlane2",
        string.overwrite_file_title to "Remplacer le fichier existant ?",
        string.overwrite_file_message to "Voulez-vous remplacer le fichier existant « %1\$s » ?",
        string.overwrite_replace to "Remplacer"
    )

    val drawableNames: Map<Int, String> = mapOf(
        drawable.ic_launcher to "ic_launcher",
        drawable.icon_0 to "icon_0",
        drawable.icon_100 to "icon_100",
        drawable.icon_25 to "icon_25",
        drawable.icon_50 to "icon_50",
        drawable.icon_75 to "icon_75",
        drawable.icon_bee to "icon_bee",
        drawable.icon_bell to "icon_bell",
        drawable.icon_broken_line to "icon_broken_line",
        drawable.icon_button to "icon_button",
        drawable.icon_button_cancel to "icon_button_cancel",
        drawable.icon_button_ok to "icon_button_ok",
        drawable.icon_calendar to "icon_calendar",
        drawable.icon_checked to "icon_checked",
        drawable.icon_clanbomber to "icon_clanbomber",
        drawable.icon_clock to "icon_clock",
        drawable.icon_clock2 to "icon_clock2",
        drawable.icon_closed to "icon_closed",
        drawable.icon_decrypted to "icon_decrypted",
        drawable.icon_desktop_new to "icon_desktop_new",
        drawable.icon_division to "icon_division",
        drawable.icon_down to "icon_down",
        drawable.icon_edit to "icon_edit",
        drawable.icon_encrypted to "icon_encrypted",
        drawable.icon_executable to "icon_executable",
        drawable.icon_family to "icon_family",
        drawable.icon_fema to "icon_fema",
        drawable.icon_female1 to "icon_female1",
        drawable.icon_female2 to "icon_female2",
        drawable.icon_females to "icon_females",
        drawable.icon_flag to "icon_flag",
        drawable.icon_flag_black to "icon_flag_black",
        drawable.icon_flag_blue to "icon_flag_blue",
        drawable.icon_flag_green to "icon_flag_green",
        drawable.icon_flag_orange to "icon_flag_orange",
        drawable.icon_flag_pink to "icon_flag_pink",
        drawable.icon_flag_yellow to "icon_flag_yellow",
        drawable.icon_folder to "icon_folder",
        drawable.icon_forward to "icon_forward",
        drawable.icon_freemind_butterfly to "icon_freemind_butterfly",
        drawable.icon_go to "icon_go",
        drawable.icon_gohome to "icon_gohome",
        drawable.icon_group to "icon_group",
        drawable.icon_help to "icon_help",
        drawable.icon_hourglass to "icon_hourglass",
        drawable.icon_idea to "icon_idea",
        drawable.icon_info to "icon_info",
        drawable.icon_internet to "icon_internet",
        drawable.icon_internet_warning to "icon_internet_warning",
        drawable.icon_kaddressbook to "icon_kaddressbook",
        drawable.icon_kmail to "icon_kmail",
        drawable.icon_knotify to "icon_knotify",
        drawable.icon_korn to "icon_korn",
        drawable.icon_ksmiletris to "icon_ksmiletris",
        drawable.icon_launch to "icon_launch",
        drawable.icon_licq to "icon_licq",
        drawable.icon_male1 to "icon_male1",
        drawable.icon_male2 to "icon_male2",
        drawable.icon_males to "icon_males",
        drawable.icon_messagebox_warning to "icon_messagebox_warning",
        drawable.icon_mindmap to "icon_mindmap",
        drawable.icon_multiplication to "icon_multiplication",
        drawable.icon_narrative to "icon_narrative",
        drawable.icon_negative to "icon_negative",
        drawable.icon_neutral to "icon_neutral",
        drawable.icon_password to "icon_password",
        drawable.icon_pencil to "icon_pencil",
        drawable.icon_penguin to "icon_penguin",
        drawable.icon_positive to "icon_positive",
        drawable.icon_prepare to "icon_prepare",
        drawable.icon_revision to "icon_revision",
        drawable.icon_smiley_angry to "icon_smiley_angry",
        drawable.icon_smiley_neutral to "icon_smiley_neutral",
        drawable.icon_smiley_oh to "icon_smiley_oh",
        drawable.icon_smily_bad to "icon_smily_bad",
        drawable.icon_stop to "icon_stop",
        drawable.icon_stop_sign to "icon_stop_sign",
        drawable.icon_subtraction to "icon_subtraction",
        drawable.icon_user_icon to "icon_user_icon",
        drawable.icon_very_negative to "icon_very_negative",
        drawable.icon_very_positive to "icon_very_positive",
        drawable.icon_xmag to "icon_xmag",
        drawable.icon_yes to "icon_yes",
        drawable.minus_alt to "minus_alt",
        drawable.plus_alt to "plus_alt"
    )

    fun getString(id: Int, vararg formatArgs: Any): String {
        val isFrench = Locale.getDefault().language == "fr"
        val template = (if (isFrench) frStrings[id] else null) ?: enStrings[id] ?: ""
        return if (formatArgs.isEmpty()) {
            template
        } else {
            String.format(Locale.getDefault(), template, *formatArgs)
        }
    }
}

@Composable
fun stringResource(id: Int, vararg formatArgs: Any): String = AppRes.getString(id, *formatArgs)

@Composable
expect fun painterResource(id: Int): Painter
