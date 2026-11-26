package com.doccheck.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.doccheck.app.R
import com.doccheck.app.databinding.FragmentAuthBinding
import com.doccheck.app.util.GlassBlurHelper
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthFragment : Fragment(R.layout.fragment_auth) {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private val args: AuthFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAuthBinding.bind(view)

        GlassBlurHelper.applyBackdropBlur(binding.ivAuthArt, 22f)
        applyInsets()

        if (savedInstanceState == null) {
            viewModel.setSignUpMode(args.startTab == 1)
        }

        setupListeners()
        observeState()
    }

    private fun applyInsets() {
        val baseBottom = (32 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.authScroll) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.authContent.updatePadding(top = bars.top, bottom = baseBottom + bars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            if (!findNavController().popBackStack()) {
                requireActivity().finish()
            }
        }
        binding.tabLogin.setOnClickListener { viewModel.setSignUpMode(false) }
        binding.tabSignUp.setOnClickListener { viewModel.setSignUpMode(true) }

        binding.btnPrimaryAuth.setOnClickListener {
            viewModel.submit(
                fullName = binding.etFullName.text?.toString().orEmpty(),
                email = binding.etEmail.text?.toString().orEmpty(),
                password = binding.etPassword.text?.toString().orEmpty(),
                confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty(),
                nameRequiredMsg = getString(R.string.err_name_required),
                invalidEmailMsg = getString(R.string.err_invalid_email),
                passwordShortMsg = getString(R.string.err_password_short),
                passwordMismatchMsg = getString(R.string.err_password_mismatch)
            )
        }

        binding.btnGoogleSignIn.setOnClickListener { viewModel.signInWithGoogle() }

        binding.tvForgotPassword.setOnClickListener {
            viewModel.sendPasswordReset(
                email = binding.etEmail.text?.toString().orEmpty(),
                invalidEmailMsg = getString(R.string.err_invalid_email),
                resetSentMsg = getString(R.string.auth_reset_sent)
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderMode(state.isSignUpMode)
                        binding.tilFullName.error = state.nameError
                        binding.tilEmail.error = state.emailError
                        binding.tilPassword.error = state.passwordError
                        binding.tilConfirmPassword.error = state.confirmPasswordError

                        binding.btnPrimaryAuth.isEnabled = !state.isLoading
                        binding.btnGoogleSignIn.isEnabled = !state.isLoading
                        binding.pbAuthLoading.isVisible = state.isLoading
                        binding.btnPrimaryAuth.text = if (state.isLoading) {
                            ""
                        } else if (state.isSignUpMode) {
                            getString(R.string.auth_signup_cta)
                        } else {
                            getString(R.string.auth_login_cta)
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            AuthEvent.NavigateHome -> {
                                findNavController().navigate(R.id.action_auth_to_home)
                            }
                            is AuthEvent.ShowMessage -> {
                                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderMode(signUp: Boolean) {
        binding.tilFullName.isVisible = signUp
        binding.tilConfirmPassword.isVisible = signUp
        binding.tvForgotPassword.isVisible = !signUp

        binding.tvAuthHeading.setText(
            if (signUp) R.string.auth_create_account_title else R.string.auth_welcome_back
        )
        binding.tvAuthSubtitle.setText(
            if (signUp) R.string.auth_create_account_sub else R.string.auth_welcome_sub
        )

        val activeText = ContextCompat.getColor(requireContext(), R.color.ink_inverse)
        val inactiveText = ContextCompat.getColor(requireContext(), R.color.ink)
        binding.tabLogin.setTextColor(if (signUp) inactiveText else activeText)
        binding.tabSignUp.setTextColor(if (signUp) activeText else inactiveText)

        binding.segmentContainer.post {
            val container = _binding?.segmentContainer ?: return@post
            val indicator = _binding?.segmentIndicator ?: return@post
            val halfWidth = (container.width - container.paddingStart - container.paddingEnd) / 2
            if (halfWidth > 0) {
                indicator.updateLayoutParams { width = halfWidth }
                val targetX = if (signUp) halfWidth.toFloat() else 0f
                indicator.animate().translationX(targetX).setDuration(220L).start()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

