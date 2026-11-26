package com.doccheck.app.ui.settings

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.doccheck.app.R
import com.doccheck.app.data.local.SessionPreferences
import com.doccheck.app.data.model.User
import com.doccheck.app.data.repository.AuthRepository
import com.doccheck.app.data.repository.ScanRepository
import com.doccheck.app.databinding.FragmentSettingsBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionPreferences: SessionPreferences,
    private val authRepository: AuthRepository,
    private val scanRepository: ScanRepository
) : ViewModel() {

    val user: StateFlow<User?> = sessionPreferences.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isDarkMode: StateFlow<Boolean> = sessionPreferences.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _loggedOut = MutableSharedFlow<Unit>()
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            sessionPreferences.setDarkMode(enabled)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            scanRepository.clearHistory()
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loggedOut.emit(Unit)
        }
    }
}

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        val baseTop = (12 * resources.displayMetrics.density).toInt()
        val baseBottom = resources.getDimensionPixelSize(R.dimen.bottom_scroll_clearance)
        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsScroll) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.settingsContent.updatePadding(top = baseTop + bars.top, bottom = baseBottom + bars.bottom)
            insets
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != viewModel.isDarkMode.value) {
                viewModel.setDarkMode(isChecked)
            }
        }
        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
            Snackbar.make(binding.root, "Scan history cleared", Snackbar.LENGTH_SHORT).show()
        }
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collect { u ->
                        binding.tvUserName.text = u?.fullName ?: "DocCheck Analyst"
                        binding.tvUserEmail.text = u?.email ?: "analyst@doccheck.ai"
                    }
                }
                launch {
                    viewModel.isDarkMode.collect { dark ->
                        if (binding.switchDarkMode.isChecked != dark) {
                            binding.switchDarkMode.isChecked = dark
                        }
                    }
                }
                launch {
                    viewModel.loggedOut.collect {
                        findNavController().navigate(R.id.action_settings_to_auth)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

