package com.mz.projectmanager.ui.component.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mz.projectmanager.data.model.Project

@Composable
fun BindSessionDialog(
    projects: List<Project>,
    currentProjectId: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (projectId: String) -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(currentProjectId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择目标项目") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(projects) { project ->
                    ListItem(
                        headlineContent = { Text(project.name) },
                        supportingContent = {
                            Text(
                                project.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingContent = {
                            RadioButton(
                                selected = selectedId == project.id,
                                onClick = { selectedId = project.id }
                            )
                        },
                        modifier = Modifier.clickable { selectedId = project.id }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedId?.let { onConfirm(it) } },
                enabled = selectedId != null && selectedId != currentProjectId
            ) {
                Text("绑定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
