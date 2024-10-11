package ch.benediktkoeppel.code.droidplane.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ch.benediktkoeppel.code.droidplane.R
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Backpress
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Help
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Open
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Search
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.SearchNext
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.SearchPrevious
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Top
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Up

enum class AppTopBarAction {
    Search,
    SearchNext,
    SearchPrevious,
    Up,
    Top,
    Open,
    Help,
    Backpress,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    text: String,
    hasBackIcon: Boolean,
    onBarAction: (AppTopBarAction) -> Unit,
    modifier: Modifier = Modifier,
) {

    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface
        ),
        title = {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher),
                    contentDescription = null,
                    modifier = Modifier,
                )
                Text(
                    modifier = Modifier,
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
//            Column(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalAlignment = if (isFullScreen) Alignment.CenterHorizontally
//                else Alignment.Start
//            ) {
//                Text(
//                    text = text,
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
////                Text(
////                    modifier = Modifier.padding(top = 4.dp),
////                    text = "${email.threads.size} ${stringResource(id = R.string.messages)}",
////                    style = MaterialTheme.typography.labelMedium,
////                    color = MaterialTheme.colorScheme.outline
////                )
//            }
        },
        navigationIcon = {
            if (hasBackIcon) {
                FilledIconButton(
                    onClick = { onBarAction(Backpress) },
                    modifier = Modifier.padding(8.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                        contentDescription = stringResource(id = R.string.back_button),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        },
        actions = {
            BarIcon(
                imageVector = Icons.Default.Search,
                onClick = { onBarAction(Search) },
            )
            BarIcon(
                imageVector = Icons.Default.MoreVert,
                onClick = { showMenu = true },
            )

            DropdownMenu(
                shadowElevation = 8.dp,
                modifier = Modifier,
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        onBarAction(SearchNext)
                    },
                    text = {
                        Text(stringResource(R.string.search_next))
                    },
                )

                DropdownMenuItem(
                    onClick = {
                        onBarAction(SearchPrevious)
                    },
                    text = {
                        Text(stringResource(R.string.search_prev))
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        onBarAction(Up)
                    },
                    text = {
                        Text(stringResource(R.string.up))
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        onBarAction(Top)
                    },
                    text = {
                        Text(stringResource(R.string.gototop))
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        onBarAction(Open)
                    },
                    text = {
                        Text(stringResource(R.string.open))
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        onBarAction(Help)
                    },
                    text = {
                        Text(stringResource(R.string.help))
                    },
                )
            }
        }
    )
}