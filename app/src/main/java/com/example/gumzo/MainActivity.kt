package com.example.gumzo

            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.activity.enableEdgeToEdge
            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.Surface
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.lifecycle.viewmodel.compose.viewModel
            import androidx.navigation.NavType
            import androidx.navigation.compose.NavHost
            import androidx.navigation.compose.composable
            import androidx.navigation.compose.rememberNavController
            import androidx.navigation.navArgument
            import com.example.gumzo.ui.auth.LoginScreen
            import com.example.gumzo.ui.auth.RegisterScreen
            import com.example.gumzo.ui.chat.ChatListScreen
            import com.example.gumzo.ui.chat.ChatRoomScreen
            import com.example.gumzo.ui.theme.GumzoTheme
            import com.example.gumzo.viewmodel.AuthViewModel
            import com.example.gumzo.viewmodel.ChatListViewModel
            import com.example.gumzo.viewmodel.ChatRoomViewModel

            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    enableEdgeToEdge()
                    setContent {
                        GumzoTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                GumzoApp()
                            }
                        }
                    }
                }
            }

            @Composable
            fun GumzoApp() {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel()

                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("chat_list") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = { navController.navigate("register") },
                            viewModel = authViewModel
                        )
                    }

                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("chat_list") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToLogin = { navController.popBackStack() },
                            viewModel = authViewModel
                        )
                    }

                    composable("chat_list") {
                        val chatListViewModel: ChatListViewModel = viewModel()
                        ChatListScreen(
                            viewModel = chatListViewModel,
                            onChatRoomClick = { chatRoomId, chatRoomName ->
                                navController.navigate("chat_room/$chatRoomId/$chatRoomName")
                            },
                            onSignOut = {
                                chatListViewModel.signOut()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(
                        route = "chat_room/{chatRoomId}/{chatRoomName}",
                        arguments = listOf(
                            navArgument("chatRoomId") { type = NavType.StringType },
                            navArgument("chatRoomName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: return@composable
                        val chatRoomName = backStackEntry.arguments?.getString("chatRoomName") ?: "Chat Room"
                        val chatRoomViewModel = ChatRoomViewModel(chatRoomId)
                        ChatRoomScreen(
                            chatRoomId = chatRoomId,
                            chatRoomName = chatRoomName,
                            viewModel = chatRoomViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }