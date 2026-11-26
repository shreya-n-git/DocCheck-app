package com.doccheck.app.ui.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.doccheck.app.R
import com.doccheck.app.data.local.SessionPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RouterFragment : Fragment(R.layout.fragment_router) {

    @Inject
    lateinit var sessionPreferences: SessionPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            val onboardingDone = sessionPreferences.hasCompletedOnboarding.first()
            val loggedIn = sessionPreferences.isLoggedIn.first()
            val navController = findNavController()
            when {
                !onboardingDone -> navController.navigate(R.id.action_router_to_welcome)
                !loggedIn -> navController.navigate(R.id.action_router_to_auth)
                else -> navController.navigate(R.id.action_router_to_home)
            }
        }
    }
}

