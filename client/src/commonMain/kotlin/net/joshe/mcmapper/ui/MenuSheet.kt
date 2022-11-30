@file:OptIn(ExperimentalMaterialApi::class)

package net.joshe.mcmapper.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class MenuSheetState(
    var items: MutableState<List<Pair<String, String>>>,
    var onSelect: (String) -> Unit = {},
    val sheetState: ModalBottomSheetState,
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberMenuSheetState() = remember {
    MenuSheetState(
        items = mutableStateOf(emptyList()),
        onSelect = {},
        sheetState = ModalBottomSheetState(ModalBottomSheetValue.Hidden),
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MenuSheetLayout(state: MenuSheetState, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()

    ModalBottomSheetLayout(
        sheetState = state.sheetState,
        sheetContent = {
            Column {
                if (state.items.value.isEmpty())
                    // XXX
                    Box(modifier = Modifier.size(1.dp))
                for ((itemKey, itemLabel) in state.items.value) {
                    key(itemKey) {
                        TextButton(onClick = {
                            scope.launch { state.sheetState.hide() }
                            state.onSelect(itemKey)
                        }) {
                            Text(text = itemLabel)
                        }
                    }
                }
            }
        },
        content = content)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MenuSheetButton(state: MenuSheetState, items: Map<String,String>, selectedKey: String?, noneSelected: String,
                    onSelect: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    if (items.isEmpty())
        return
    OutlinedButton(onClick = {
        state.items.value = items.keys.sorted().map { k -> Pair(k, items.getValue(k)) }
        state.onSelect = onSelect
        scope.launch { state.sheetState.show() }
    }) {
        Text(text = items[selectedKey] ?: noneSelected)
    }
}
