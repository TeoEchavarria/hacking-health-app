package com.samsung.android.health.sdk.sample.healthdiary.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.samsung.android.health.sdk.sample.healthdiary.R
import kotlinx.coroutines.launch
import com.samsung.android.health.sdk.sample.healthdiary.api.models.LoginRequest
import com.samsung.android.health.sdk.sample.healthdiary.databinding.ActivityLoginBinding
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.AuthViewModel
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.HealthViewModelFactory
import com.google.gson.Gson
import java.io.File

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authViewModel: AuthViewModel
    private val debugGson = Gson()

    // #region agent log
    private fun debugLog(hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
        try {
            val payload = mapOf(
                "sessionId" to "debug-session",
                "runId" to "pre-fix",
                "hypothesisId" to hypothesisId,
                "location" to "LoginActivity.kt",
                "message" to message,
                "data" to data,
                "timestamp" to System.currentTimeMillis()
            )
            File("/Users/teoechavarria/Documents/hh/.cursor/debug.log")
                .appendText(debugGson.toJson(payload) + "\n")
        } catch (_: Exception) {
            // best-effort logging only
        }
    }
    // #endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = HealthViewModelFactory(this)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.viewModel = authViewModel
        binding.lifecycleOwner = this

        setupViews()
        observeAuthState()
    }

    private fun setupViews() {
        binding.btnLogin.setOnClickListener {
            performLogin()
        }
        
        binding.etLoginPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performLogin()
                true
            } else {
                false
            }
        }
    }
    
    private fun performLogin() {
        val username = binding.etLoginUsername.text.toString().trim()
        val password = binding.etLoginPassword.text.toString().trim()
        
        when {
            username.isEmpty() -> {
                showLoginError(getString(R.string.login_username_required))
                return
            }
            password.isEmpty() -> {
                showLoginError(getString(R.string.login_password_required))
                return
            }
        }
        
        hideStatusMessage()
        showLoginError(null)
        
        val request = LoginRequest(
            username = username,
            password = password,
            fcmToken = ""
        )
        authViewModel.login(request)
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.authState.collect { state ->
                    when (state) {
                        is AuthViewModel.AuthState.Idle -> {
                            // Estado inicial
                        }
                        is AuthViewModel.AuthState.Loading -> showLoading(true)
                        is AuthViewModel.AuthState.Success -> {
                            showLoading(false)
                            Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                            showLoginError(null)
                            // #region agent log
                            debugLog(
                                hypothesisId = "H1",
                                message = "Auth success, launching HealthMainActivity",
                                data = mapOf("activity" to "HealthMainActivity")
                            )
                            // #endregion

                            val intent = Intent(this@LoginActivity, HealthMainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        is AuthViewModel.AuthState.Error -> {
                            showLoading(false)
                            showLoginError(state.message)
                        }
                    }
                }
            }
        }

        // Observar mensajes de error
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.errorMessage.collect { errorMsg ->
                    errorMsg?.let { 
                        showLoading(false)
                        showLoginError(it)
                    }
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        if (show) {
            binding.tvStatusMessage.text = getString(R.string.auth_loading)
            binding.tvStatusMessage.visibility = View.VISIBLE
        } else {
            hideStatusMessage()
        }
    }

    private fun hideStatusMessage() {
        binding.tvStatusMessage.text = ""
        binding.tvStatusMessage.visibility = View.GONE
    }
    
    private fun showLoginError(message: String?) {
        binding.tvLoginError.text = message ?: ""
        binding.tvLoginError.visibility = if (message.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
}

