package co.adityarajput.fileflow.views

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import co.adityarajput.fileflow.viewmodels.AppearanceViewModel
import co.adityarajput.fileflow.views.screens.*
import kotlinx.serialization.Serializable

@Composable
fun Navigator(controller: NavHostController, appearanceViewModel: AppearanceViewModel) {
    NavHost(
        controller,
        Routes.RULES.name,
    ) {
        composable(Routes.RULES.name) {
            RulesScreen(
                { controller.navigate(UpsertRuleRoute(it)) },
                { controller.navigate(Routes.EXECUTIONS.name) },
                { controller.navigate(Routes.SETTINGS.name) },
            )
        }
        composable<UpsertRuleRoute> {
            UpsertRuleScreen(
                it.toRoute<UpsertRuleRoute>().ruleString,
                controller::popBackStack,
            )
        }
        composable(Routes.EXECUTIONS.name) { ExecutionsScreen(controller::popBackStack) }
        composable(Routes.SETTINGS.name) {
            SettingsScreen(
                { controller.navigate(Routes.ABOUT.name) },
                controller::popBackStack,
                appearanceViewModel,
            )
        }
        composable(Routes.ABOUT.name) { AboutScreen(controller::popBackStack) }
    }
}

enum class Routes {
    RULES,
    EXECUTIONS,
    SETTINGS,
    ABOUT,
}

@Serializable
data class UpsertRuleRoute(val ruleString: String = "null")
