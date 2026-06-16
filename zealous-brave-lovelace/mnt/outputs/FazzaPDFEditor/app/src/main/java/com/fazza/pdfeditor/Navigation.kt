package com.fazza.pdfeditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fazza.pdfeditor.ui.screens.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PdfViewer : Screen("viewer/{filePath}") {
        fun createRoute(filePath: String): String {
            val encoded = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
            return "viewer/$encoded"
        }
    }
    object PdfEditor : Screen("editor/{filePath}") {
        fun createRoute(filePath: String): String {
            val encoded = URLEncoder.encode(filePath, StandardCharsets.UTF_8.toString())
            return "editor/$encoded"
        }
    }
    object MergeSplit : Screen("merge_split")
}

@Composable
fun FazzaNavGraph(
    navController: NavHostController,
    startFilePath: String? = null
) {
    LaunchedEffect(startFilePath) {
        startFilePath?.let {
            navController.navigate(Screen.PdfViewer.createRoute(it))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onOpenViewer = { filePath ->
                    navController.navigate(Screen.PdfViewer.createRoute(filePath))
                },
                onOpenEditor = { filePath ->
                    navController.navigate(Screen.PdfEditor.createRoute(filePath))
                },
                onMergeSplit = {
                    navController.navigate(Screen.MergeSplit.route)
                }
            )
        }

        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("filePath") ?: return@composable
            val filePath = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
            PdfViewerScreen(
                filePath = filePath,
                onBack = { navController.popBackStack() },
                onEditRequested = {
                    navController.navigate(Screen.PdfEditor.createRoute(filePath))
                }
            )
        }

        composable(
            route = Screen.PdfEditor.route,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("filePath") ?: return@composable
            val filePath = URLDecoder.decode(encoded, StandardCharsets.UTF_8.toString())
            PdfEditorScreen(
                filePath = filePath,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MergeSplit.route) {
            MergeSplitScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
