@file:OptIn(ExperimentalMaterialApi::class)

package net.joshe.mcmapper.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
data class MenuSheetState(
    val sheetState: ModalBottomSheetState,
    val sheetContent: MutableState<@Composable (ColumnScope.() -> Unit)>,
) {
    suspend fun show() = sheetState.show()
    suspend fun hide() = sheetState.hide()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberMenuSheetState() = remember {
    MenuSheetState(
        sheetState = ModalBottomSheetState(ModalBottomSheetValue.Hidden),
        sheetContent = mutableStateOf({
            // XXX fixed in 1.4 https://issuetracker.google.com/issues/216693030
            Box(modifier=Modifier.size(1.dp))
        }),
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MenuSheetLayout(state: MenuSheetState, content: @Composable () -> Unit) {
    ModalBottomSheetLayout(
        sheetState = state.sheetState,
        sheetContent = { Column { state.sheetContent.value(this) } },
        content = content,
    )
}

@Composable
fun MenuSheetButton(state: MenuSheetState, text: String,
                    content: @Composable (ColumnScope.() -> Unit)) {
    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = {
            state.sheetContent.value = content
            scope.launch { state.show() }
        },
        content = { Text(text = text) },
    )
}

@Composable
fun MenuSheetRadioItem(state: MenuSheetState, selected: Boolean, onClick: () -> Unit,
                       content: @Composable (RowScope.() -> Unit)) {
    val scope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().selectable(
            selected = selected,
            role = Role.RadioButton,
            onClick = {
                scope.launch { state.hide() }
                onClick()
            }
        )
    ) {
        RadioButton(selected = selected, onClick = null)
        content()
    }
}

@Composable
fun MenuSheetCheckItem(state: MenuSheetState, selected: Boolean, onChange: (Boolean) -> Unit,
                       content: @Composable (RowScope.() -> Unit)) {
    val scope = rememberCoroutineScope()
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().toggleable(
            value = selected,
            role = Role.Checkbox,
            onValueChange = { value ->
                scope.launch { state.hide() }
                onChange(value)
            }
        )
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        content()
    }
}
