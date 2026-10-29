package fr.julien.quievreux.droidplane2.helper

/**
 * Outcome of resolving the URI and display name returned by the system file picker
 * against the currently active mindmap document.
 */
sealed interface SaveResolution {
    /**
     * The user is saving a new mindmap, saving the demo mindmap, or explicitly changed
     * the filename in the system picker to create a separate file ("Save As" behavior).
     */
    data class WriteNewFile(
        val targetUriString: String,
        val displayName: String,
    ) : SaveResolution

    /**
     * The user explicitly selected the exact same underlying document URI in the system
     * picker (the OS picker already prompted for overwrite and returned originalUri).
     */
    data class DirectOverwrite(
        val targetUriString: String,
        val displayName: String,
    ) : SaveResolution

    /**
     * The user validated the pre-filled filename in the system picker while editing an
     * existing file, causing Android's DocumentsProvider to create a temporary duplicate
     * URI (often with an incremented "(n)" suffix). Requires user confirmation before
     * overwriting [originalUriString] and deleting [duplicatePickerUriString].
     */
    data class ConfirmOverwrite(
        val originalUriString: String,
        val originalDisplayName: String,
        val duplicatePickerUriString: String,
        val duplicateDisplayName: String,
    ) : SaveResolution
}

/**
 * Pure-Kotlin resolver that determines whether a file picker result should write a new file,
 * directly overwrite the existing URI, or trigger the in-app overwrite confirmation flow.
 */
object SaveTargetResolver {

    private const val DEMO_RESOURCE_SCHEME = "android.resource"

    fun isEditingExistingUserFile(originalUriString: String?): Boolean {
        if (originalUriString.isNullOrBlank()) return false
        return !originalUriString.startsWith("$DEMO_RESOURCE_SCHEME:")
    }

    fun resolve(
        originalUriString: String?,
        originalDisplayName: String?,
        pickedUriString: String,
        pickedDisplayName: String,
    ): SaveResolution {
        if (!isEditingExistingUserFile(originalUriString) || originalDisplayName.isNullOrBlank()) {
            return SaveResolution.WriteNewFile(
                targetUriString = pickedUriString,
                displayName = pickedDisplayName,
            )
        }

        if (pickedUriString == originalUriString) {
            return SaveResolution.DirectOverwrite(
                targetUriString = originalUriString,
                displayName = originalDisplayName,
            )
        }

        return if (isSameOrAutoIncrementedFileName(originalDisplayName, pickedDisplayName)) {
            SaveResolution.ConfirmOverwrite(
                originalUriString = originalUriString!!,
                originalDisplayName = originalDisplayName,
                duplicatePickerUriString = pickedUriString,
                duplicateDisplayName = pickedDisplayName,
            )
        } else {
            SaveResolution.WriteNewFile(
                targetUriString = pickedUriString,
                displayName = pickedDisplayName,
            )
        }
    }

    /**
     * Returns true if [pickedFileName] is identical to [originalFileName] or matches Android
     * DocumentsProvider's auto-incremented duplicate naming conventions:
     * - `<base> (<n>).<ext>` (e.g., `"carte (1).mm"`, `"carte (2).mm"`)
     * - `<base> (<n>)` (e.g., `"carte (1)"`)
     * - `<originalFileName> (<n>)` (e.g., `"carte.mm (1)"`)
     */
    fun isSameOrAutoIncrementedFileName(
        originalFileName: String,
        pickedFileName: String,
    ): Boolean {
        val trimmedOriginal = originalFileName.trim()
        val trimmedPicked = pickedFileName.trim()
        if (trimmedOriginal.isEmpty() || trimmedPicked.isEmpty()) return false

        if (trimmedPicked == trimmedOriginal) return true

        val dotIndex = trimmedOriginal.lastIndexOf('.')
        val hasExtension = dotIndex > 0 && dotIndex < trimmedOriginal.length - 1

        if (hasExtension) {
            val baseName = trimmedOriginal.substring(0, dotIndex)
            val extension = trimmedOriginal.substring(dotIndex + 1)
            val basePattern = Regex(
                "^${Regex.escape(baseName)}\\s*\\([1-9]\\d*\\)(?:\\.${Regex.escape(extension)})?$"
            )
            if (basePattern.matches(trimmedPicked)) {
                return true
            }
        }

        val fullPattern = Regex("^${Regex.escape(trimmedOriginal)}\\s*\\([1-9]\\d*\\)$")
        return fullPattern.matches(trimmedPicked)
    }
}
