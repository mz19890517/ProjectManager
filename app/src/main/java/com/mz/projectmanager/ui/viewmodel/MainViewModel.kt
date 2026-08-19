package com.mz.projectmanager.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mz.projectmanager.data.model.FileItem
import com.mz.projectmanager.data.model.ProjectItem
import com.mz.projectmanager.data.model.SessionItem
import com.mz.projectmanager.data.model.SortOption
import com.mz.projectmanager.data.repository.FileRepository
import com.mz.projectmanager.data.repository.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionRepo = SessionRepository(application, FileRepository.OPENCODE_DB_PATH)
    private val fileRepo = FileRepository()

    private val _projects = MutableStateFlow<List<ProjectItem>>(emptyList())
    val projects: StateFlow<List<ProjectItem>> = _projects.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionItem>>(emptyList())
    val sessions: StateFlow<List<SessionItem>> = _sessions.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _currentProject = MutableStateFlow<ProjectItem?>(null)
    val currentProject: StateFlow<ProjectItem?> = _currentProject.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _clipboardItems = MutableStateFlow<List<String>>(emptyList())
    val clipboardItems: StateFlow<List<String>> = _clipboardItems.asStateFlow()

    private val _clipboardMode = MutableStateFlow<ClipboardMode?>(null)
    val clipboardMode: StateFlow<ClipboardMode?> = _clipboardMode.asStateFlow()

    fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val projectList = withContext(Dispatchers.IO) {
                    sessionRepo.getAllProjects()
                }
                _projects.value = projectList
            } catch (e: Exception) {
                _errorMessage.value = "加载项目失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProjectDetail(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val project = withContext(Dispatchers.IO) {
                    sessionRepo.getProjectById(projectId)
                }
                _currentProject.value = project

                val sessionList = withContext(Dispatchers.IO) {
                    sessionRepo.getSessionsForProject(projectId)
                }
                _sessions.value = sessionList
            } catch (e: Exception) {
                _errorMessage.value = "加载项目详情失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFiles(path: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fileList = withContext(Dispatchers.IO) {
                    fileRepo.listFiles(path, _sortOption.value)
                }
                fileList.fold(
                    onSuccess = { _files.value = it },
                    onFailure = { _errorMessage.value = "加载文件失败: ${it.message}" }
                )
            } catch (e: Exception) {
                _errorMessage.value = "加载文件失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        val currentPath = _files.value.firstOrNull()?.path?.substringBeforeLast('/')
        if (currentPath != null) {
            loadFiles(currentPath)
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fullPath = "${FileRepository.PROJECTS_ROOT}/$name"
                withContext(Dispatchers.IO) {
                    fileRepo.createDirectory(FileRepository.PROJECTS_ROOT, name)
                    sessionRepo.createProject(fullPath, name)
                }
                loadProjects()
            } catch (e: Exception) {
                _errorMessage.value = "创建项目失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val project = withContext(Dispatchers.IO) {
                    sessionRepo.getProjectById(projectId)
                }
                if (project != null) {
                    withContext(Dispatchers.IO) {
                        sessionRepo.deleteProject(projectId)
                        fileRepo.delete(project.worktree)
                    }
                }
                loadProjects()
            } catch (e: Exception) {
                _errorMessage.value = "删除项目失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val project = withContext(Dispatchers.IO) {
                    sessionRepo.getProjectById(projectId)
                }
                if (project != null) {
                    val newWorktree = project.worktree.substringBeforeLast('/') + "/$newName"
                    withContext(Dispatchers.IO) {
                        fileRepo.rename(project.worktree, newName)
                        sessionRepo.updateProject(projectId, newName, newWorktree)
                    }
                }
                loadProjects()
            } catch (e: Exception) {
                _errorMessage.value = "重命名失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createSession(projectId: String, directory: String, title: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sessionRepo.createSession(projectId, directory, title)
                }
                loadProjectDetail(projectId)
            } catch (e: Exception) {
                _errorMessage.value = "创建对话失败: ${e.message}"
            }
        }
    }

    fun deleteSession(sessionId: String, projectId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sessionRepo.deleteSession(sessionId)
                }
                loadProjectDetail(projectId)
            } catch (e: Exception) {
                _errorMessage.value = "删除对话失败: ${e.message}"
            }
        }
    }

    fun rebindSession(sessionId: String, newProjectId: String, newDirectory: String, currentProjectId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sessionRepo.rebindSession(sessionId, newProjectId, newDirectory)
                }
                loadProjectDetail(currentProjectId)
            } catch (e: Exception) {
                _errorMessage.value = "换绑对话失败: ${e.message}"
            }
        }
    }

    fun copyToClipboard(paths: List<String>) {
        _clipboardItems.value = paths
        _clipboardMode.value = ClipboardMode.COPY
    }

    fun cutToClipboard(paths: List<String>) {
        _clipboardItems.value = paths
        _clipboardMode.value = ClipboardMode.MOVE
    }

    fun pasteToClipboard(destPath: String) {
        val items = _clipboardItems.value
        val mode = _clipboardMode.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    for (item in items) {
                        when (mode) {
                            ClipboardMode.COPY -> fileRepo.copy(item, destPath)
                            ClipboardMode.MOVE -> fileRepo.move(item, destPath)
                        }
                    }
                }
                clearClipboard()
                loadFiles(destPath)
            } catch (e: Exception) {
                _errorMessage.value = "粘贴失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearClipboard() {
        _clipboardItems.value = emptyList()
        _clipboardMode.value = null
    }

    fun createFile(parentPath: String, name: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (name.endsWith('/')) {
                        fileRepo.createDirectory(parentPath, name.dropLast(1))
                    } else {
                        fileRepo.createFile(parentPath, name)
                    }
                }
                loadFiles(parentPath)
            } catch (e: Exception) {
                _errorMessage.value = "创建失败: ${e.message}"
            }
        }
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            try {
                val parentPath = path.substringBeforeLast('/')
                withContext(Dispatchers.IO) {
                    fileRepo.delete(path)
                }
                loadFiles(parentPath)
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.message}"
            }
        }
    }

    fun renameFile(oldPath: String, newName: String) {
        viewModelScope.launch {
            try {
                val parentPath = oldPath.substringBeforeLast('/')
                withContext(Dispatchers.IO) {
                    fileRepo.rename(oldPath, newName)
                }
                loadFiles(parentPath)
            } catch (e: Exception) {
                _errorMessage.value = "重命名失败: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

enum class ClipboardMode {
    COPY, MOVE
}
