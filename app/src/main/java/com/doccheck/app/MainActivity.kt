package com.doccheck.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.doccheck.app.data.local.SessionPreferences
import com.doccheck.app.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var sessionPreferences: SessionPreferences

    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.chatFragment,
        R.id.historyFragment,
        R.id.settingsFragment
    )

    private var currentDestId: Int = R.id.routerFragment
    private var isImeVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeThemePreference()
        setupNavigation()
        setupWindowInsets()
    }

    private fun observeThemePreference() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionPreferences.isDarkMode
                    .distinctUntilChanged()
                    .collect { dark ->
                        val desiredMode = if (dark) {
                            AppCompatDelegate.MODE_NIGHT_YES
                        } else {
                            AppCompatDelegate.MODE_NIGHT_NO
                        }
                        if (AppCompatDelegate.getDefaultNightMode() != desiredMode) {
                            AppCompatDelegate.setDefaultNightMode(desiredMode)
                        }
                    }
            }
        }
    }

    private fun setupNavigation() {
        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHost.navController

        binding.floatingBottomNav.setOnTabSelectedListener { destId ->
            if (navController.currentDestination?.id != destId) {
                val animEnter = if (destId == R.id.chatFragment) R.anim.slide_up_enter else R.anim.nav_enter
                val options = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(false)
                    .setEnterAnim(animEnter)
                    .setExitAnim(R.anim.nav_exit)
                    .setPopEnterAnim(R.anim.nav_enter)
                    .setPopExitAnim(R.anim.nav_exit)
                    .setPopUpTo(R.id.homeFragment, inclusive = false, saveState = true)
                    .build()
                navController.navigate(destId, null, options)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentDestId = destination.id
            if (destination.id in topLevelDestinations) {
                binding.floatingBottomNav.setSelectedDestination(destination.id, animate = true)
            }
            updateBottomNavVisibility()
        }
    }

    private fun setupWindowInsets() {
        val baseMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_margin)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            binding.floatingBottomNav.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                bottomMargin = baseMargin + navBarInsets.bottom
            }
            updateBottomNavVisibility()
            insets
        }
    }

    private fun updateBottomNavVisibility() {
        val shouldShow = currentDestId in topLevelDestinations && !isImeVisible
        binding.floatingBottomNav.isVisible = shouldShow
    }
}

