package co.adityarajput.fileflow.views

import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import co.adityarajput.fileflow.Constants.IS_FIRST_RUN
import co.adityarajput.fileflow.Constants.STATE
import co.adityarajput.fileflow.viewmodels.AppearanceViewModel
import co.adityarajput.fileflow.views.screens.*
import kotlinx.serialization.Serializable

@Composable
fun Navigator(controller: NavHostController, appearanceViewModel: AppearanceViewModel) {
    val isFirstRun = remember {
        controller.context.getSharedPreferences(STATE, MODE_PRIVATE).getBoolean(IS_FIRST_RUN, true)
    }

    NavHost(
        controller,
        if (isFirstRun) Routes.ONBOARDING.name else Routes.RULES.name,
    ) {
        composable(Routes.ONBOARDING.name) {
            OnboardingScreen {
                controller.context.getSharedPreferences(STATE, MODE_PRIVATE)
                    .edit { putBoolean(IS_FIRST_RUN, false) }
                controller.navigate(
                    Routes.RULES.name,
                    NavOptions.Builder().setPopUpTo(Routes.ONBOARDING.name, true).build(),
                )
            }
        }
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
                { controller.navigate(Routes.LICENSES.name) },
                { controller.navigate(Routes.ABOUT.name) },
                controller::popBackStack,
                appearanceViewModel,
            )
        }
        composable(Routes.LICENSES.name) { LicensesScreen(controller::popBackStack) }
        composable(Routes.ABOUT.name) { AboutScreen(controller::popBackStack) }
    }
}

enum class Routes {
    ONBOARDING,
    RULES,
    EXECUTIONS,
    SETTINGS,
    LICENSES,
    ABOUT,
}

@Serializable
data class UpsertRuleRoute(val ruleString: String = "null")
