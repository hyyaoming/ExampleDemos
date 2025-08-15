package com.example.sampleview.mvi.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.example.sampleview.R
import com.example.sampleview.mvi.base.BaseActivity
import com.xnhz.libbase.dialog.TipDialogLoadingHandler

class LoginActivity : BaseActivity<LoginViewModel, LoginContract.State, LoginContract.Event, LoginContract.Effect>() {

    override val viewModel: LoginViewModel by lazy { LoginViewModel() }

    private val tipLoadingHandler by lazy { TipDialogLoadingHandler(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            viewModel.handleEvent(LoginContract.Event.SubmitLogin)
        }

        findViewById<EditText>(R.id.etUsername).doOnTextChanged { text, _, _, _ ->
            viewModel.handleEvent(LoginContract.Event.UsernameChanged(text.toString()))
        }

        findViewById<EditText>(R.id.etPassword).doOnTextChanged { text, _, _, _ ->
            viewModel.handleEvent(LoginContract.Event.PasswordChanged(text.toString()))
        }
    }

    override fun renderState(state: LoginContract.State) {
        if (state.loginUiState == LoginUiState.Loading) {
            tipLoadingHandler.showTipLoading("登录中..", false, 1000)
        } else {
            tipLoadingHandler.dismissTipLoading()
        }
    }

    override fun handleEffect(effect: LoginContract.Effect) {
        when (effect) {
            is LoginContract.Effect.LoginSuccess -> {
                Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                // 跳转主页
            }
            is LoginContract.Effect.ShowError -> {
                Toast.makeText(this, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
