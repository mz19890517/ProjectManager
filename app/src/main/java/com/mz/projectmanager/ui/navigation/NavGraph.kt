package com.mz.projectmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mz.projectmanager.ui.screen.FileBrowserScreen
import com.mz.projectmanager.ui.screen.ProjectDetailScreen
import com.mz.projectmanager.ui.screen.ProjectListScreen

object Routes {
    const val PROJECT_LIST = "project_list"
    const val PROJECT_DETAIL = "project_detail/{projectId}"
    const val FILE_BROWSER = "file_browser/{path}"
    const val FILE_BROWSER_SELECT = "file_browser_select/{path}/{mode}"

    fun projectDetail(projectId: String) = "project_detail/$projectId"
    fun fileBrowser(path: String) = "file_browser/${java.net.URLEncoder.encode(path, "UTF-8")}"
    fun fileBrowserSelect(path: String, mode: String) =
        "file_browser_select/${java.net.URLEncoder.encode(path, "UTF-8")}/$mode"
}

@Composable
fun ProjectManagerNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.PROJECT_LIST
    ) {
        composable(Routes.PROJECT_LIST) {
            ProjectListScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Routes.projectDetail(projectId))
                },
                onSelectFolder = { path, mode ->
                    navController.navigate(Routes.fileBrowserSelect(path, mode))
                }
            )
        }

        composable(
            route = Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            ProjectDetailScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onOpenFolder = { path ->
                    navController.navigate(Routes.fileBrowser(path))
                }
            )
        }

        composable(
            route = Routes.FILE_BROWSER,
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path") ?: return@composable
            val path = java.net.URLDecoder.decode(encodedPath, "UTF-8")
            FileBrowserScreen(
                initialPath = path,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FILE_BROWSER_SELECT,
            arguments = listOf(
                navArgument("path") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("path") ?: return@composable
            val mode = backStackEntry.arguments?.getString("mode") ?: "projects"
            val path = java.net.URLDecoder.decode(encodedPath, "UTF-8")
            FileBrowserScreen(
                initialPath = path,
                onNavigateBack = { navController.popBackStack() },
                selectionMode = mode,
                onFolderSelected = { selectedPath ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "selected_path",
                        selectedPath
                    )
                    navController.popBackStack()
                }
            )
        }
    }
}
