package com.mz.projectmanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mz.projectmanager.ui.component.SessionCard
import com.mz.projectmanager.ui.component.dialogs.CreateSessionDialog
import com.mz.projectmanager.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val currentProject by viewModel.currentProject.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }

    val tabs = listOf("文件浏览", "对话管理")

    LaunchedEffect(projectId) {
        viewModel.loadProjectDetail(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentProject?.name ?: "项目详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showCreateFileDialog = true
                        1 -> showCreateSessionDialog = true
                    }
                }
            ) {
                Icon(
                    when (selectedTab) {
                        0 -> Icons.Default.CreateNewFolder
                        else -> Icons.Default.NoteAdd
                    },
                    contentDescription = when (selectedTab) {
                        0 -> "创建文件"
                        else -> "创建对话"
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (errorMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(errorMessage ?: "")
                }
            }

            when (selectedTab) {
                0 -> {
                    if (currentProject != null) {
                        LaunchedEffect(currentProject) {
                            viewModel.loadFiles(currentProject!!.worktree)
                        }
                        FileListTabContent(
                            projectPath = currentProject!!.worktree,
                            onOpenFolder = onOpenFolder,
                            isLoading = isLoading,
                            viewModel = viewModel
                        )
                    } else if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                1 -> {
                    SessionListTabContent(
                        sessions = sessions,
                        projectId = projectId,
                        isLoading = isLoading,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showCreateSessionDialog) {
        CreateSessionDialog(
            onDismiss = { showCreateSessionDialog = false },
            onConfirm = { title ->
                val directory = currentProject?.worktree ?: ""
                viewModel.createSession(projectId, directory, title)
                showCreateSessionDialog = false
            }
        )
    }

    if (showCreateFileDialog) {
        CreateSessionDialog(
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { name ->
                val path = currentProject?.worktree ?: ""
                viewModel.createFile(path, name)
                showCreateFileDialog = false
            }
        )
    }
}

@Composable
private fun FileListTabContent(
    projectPath: String,
    onOpenFolder: (String) -> Unit,
    isLoading: Boolean,
    viewModel: MainViewModel
) {
    val files by viewModel.files.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无文件",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(files, key = { it.path }) { fileItem ->
                ListItem(
                    headlineContent = { Text(fileItem.name) },
                    supportingContent = {
                        Text(
                            if (fileItem.isDirectory) "文件夹" else "${fileItem.size} 字节",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingContent = {
                        Icon(
                            if (fileItem.isDirectory) Icons.Default.CreateNewFolder else Icons.Default.NoteAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SessionListTabContent(
    sessions: List<com.mz.projectmanager.data.model.SessionItem>,
    projectId: String,
    isLoading: Boolean,
    viewModel: MainViewModel
) {
    var sessionToDelete by remember { mutableStateOf<com.mz.projectmanager.data.model.SessionItem?>(null) }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (sessions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无对话\n点击 + 创建新对话",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    onClick = { },
                    onLongClick = { },
                    onDelete = { sessionToDelete = session },
                    onRebind = { },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除对话「${session.title}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session.id, projectId)
                        sessionToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}
