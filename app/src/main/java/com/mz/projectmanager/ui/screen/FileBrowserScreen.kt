package com.mz.projectmanager.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mz.projectmanager.ui.component.FileListItem
import com.mz.projectmanager.ui.component.PathBar
import com.mz.projectmanager.ui.component.dialogs.CreateProjectDialog
import com.mz.projectmanager.ui.component.dialogs.DeleteDialog
import com.mz.projectmanager.ui.component.dialogs.RenameDialog
import com.mz.projectmanager.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    initialPath: String,
    onNavigateBack: () -> Unit,
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val files by viewModel.files.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val clipboardItems by viewModel.clipboardItems.collectAsState()
    val clipboardMode by viewModel.clipboardMode.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var currentPath by remember { mutableStateOf(initialPath) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<com.mz.projectmanager.data.model.FileItem?>(null) }

    LaunchedEffect(currentPath) {
        viewModel.loadFiles(currentPath)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件浏览") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新建") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            PathBar(
                path = currentPath,
                onPathClick = { path ->
                    currentPath = path
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (clipboardItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (clipboardMode == com.mz.projectmanager.ui.viewmodel.ClipboardMode.COPY)
                                Icons.Default.ContentCopy else Icons.Default.ContentCut,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${clipboardItems.size} 个项目已${if (clipboardMode == com.mz.projectmanager.ui.viewmodel.ClipboardMode.COPY) "复制" else "剪切"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            viewModel.pasteToClipboard(currentPath)
                        }) {
                            Text("粘贴")
                        }
                        IconButton(onClick = { viewModel.clearClipboard() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    }
                }
            }

            if (files.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "文件夹为空",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files) { file ->
                        FileListItem(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    currentPath = file.path
                                }
                            },
                            onLongClick = {
                                selectedFile = file
                            }
                        )
                    }
                }
            }
        }
    }

    selectedFile?.let { file ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { selectedFile = null }
        ) {
            if (file.isDirectory) {
                DropdownMenuItem(
                    text = { Text("进入") },
                    onClick = {
                        currentPath = file.path
                        selectedFile = null
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                )
            }
            DropdownMenuItem(
                text = { Text("重命名") },
                onClick = {
                    showRenameDialog = true
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    viewModel.copyToClipboard(listOf(file.path))
                    selectedFile = null
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("剪切") },
                onClick = {
                    viewModel.cutToClipboard(listOf(file.path))
                    selectedFile = null
                },
                leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showDeleteDialog = true
                },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            title = "新建",
            confirmText = "创建",
            onConfirm = { name ->
                viewModel.createFile(currentPath, name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showRenameDialog && selectedFile != null) {
        RenameDialog(
            currentName = selectedFile!!.name,
            onConfirm = { newName ->
                viewModel.renameFile(selectedFile!!.path, newName)
                showRenameDialog = false
                selectedFile = null
            },
            onDismiss = {
                showRenameDialog = false
                selectedFile = null
            }
        )
    }

    if (showDeleteDialog && selectedFile != null) {
        DeleteDialog(
            itemName = selectedFile!!.name,
            onConfirm = {
                viewModel.deleteFile(selectedFile!!.path)
                showDeleteDialog = false
                selectedFile = null
            },
            onDismiss = {
                showDeleteDialog = false
                selectedFile = null
            }
        )
    }
}
