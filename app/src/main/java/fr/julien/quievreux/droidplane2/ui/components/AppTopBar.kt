package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.AutoMirrored.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Backpress
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Help
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Open
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchNext
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchPrevious
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Top
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Up
import fr.julien.quievreux.droidplane2.R

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
    hasSearchNavigateButton: Pair<Boolean,Boolean>,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val items = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig", "Grape", "Honeydew")
    val filteredItems = items.filter { it.contains(searchQuery, ignoreCase = true) }

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
                if (showSearch) {
                    val colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = searchQuery,
                                onQueryChange = {
                                    searchQuery = it
                                    onQuery(searchQuery)
                                },
                                onSearch = { expanded = false },
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                enabled = true,
                                placeholder = { Text("Search") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Filled.ArrowBack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable {
                                            searchQuery = ""
                                            showSearch = false
                                        }
                                    )
                                },
                                trailingIcon = {
                                    if (expanded)
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier.clickable {
                                                searchQuery = ""
                                                onBarAction(Top)
                                            }
                                        )
                                },
                                interactionSource = null,
                            )
                        },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = modifier
                            .weight(1f),
                        shape = SearchBarDefaults.inputFieldShape,
                        colors = colors,
                        tonalElevation = 0.dp,
                        shadowElevation = SearchBarDefaults.ShadowElevation,
                        windowInsets = SearchBarDefaults.windowInsets,
                    ) {
                        //Search content here
                        filteredItems.forEach { item ->
                            Text(text = item, fontSize = 15.sp)
                        }
                    }
                    if(hasSearchNavigateButton.first) {
                        BarIcon(
                            imageVector = Filled.KeyboardArrowLeft,
                            onClick = { onBarAction(SearchNext) },
                        )
                    }

                    if(hasSearchNavigateButton.second) {
                        BarIcon(
                            imageVector = Filled.KeyboardArrowRight,
                            onClick = { onBarAction(SearchPrevious) },
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = modifier.clickable {
                            showSearch = true
                        }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher),
                            contentDescription = null,
                            modifier = Modifier,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = text,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (hasBackIcon && !showSearch) {
                FilledIconButton(
                    onClick = {
                        onBarAction(Backpress)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            }
        },
        actions = {
            if (!showSearch) {
                BarIcon(
                    imageVector = Icons.Default.Search,
                    onClick = {
//                        onBarAction(Search)
                        showSearch = true
                    },
                )
            }
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
                        showMenu = false
                        onBarAction(Up)
                    },
                    text = {
                        Text(stringResource(R.string.up))
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        onBarAction(Top)
                    },
                    text = {
                        Text(stringResource(R.string.gototop))
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
                        onBarAction(Open)
                    },
                    text = {
                        Text(stringResource(R.string.open))
                    },
                )
                DropdownMenuItem(
                    onClick = {
                        showMenu = false
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