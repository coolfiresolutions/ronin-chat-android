package com.coolfiresolutions.roninchat.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.coolfiresolutions.roninchat.R
import com.coolfiresolutions.roninchat.RoninChatApplication
import com.coolfiresolutions.roninchat.common.BaseFragment
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : BaseFragment(), RoninChatApplication.LoginCallback {
    lateinit var listener: LoginFragmentListener

    interface LoginFragmentListener {
        fun onLoginClicked(instanceUrl: String, username: String, password: String, listener: RoninChatApplication.LoginCallback)
    }

    companion object {
        val TAG = LoginFragment::class.java.canonicalName!!

        fun newInstance() = LoginFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listener = activity as LoginFragmentListener
        initClickListeners()
    }

    private fun initClickListeners() {
        btnLogin.setOnClickListener {
            hideKeyboard()
            attemptLogin()
        }
    }

    private fun attemptLogin() {
        when {
            etInstanceUrl.text.isNullOrEmpty() -> Toast.makeText(activity, "Enter an instance url.", Toast.LENGTH_SHORT).show()
            etUsername.text.isNullOrEmpty() -> Toast.makeText(activity, "Enter a username.", Toast.LENGTH_SHORT).show()
            etPassword.text.isNullOrEmpty() -> Toast.makeText(activity, "Enter a password.", Toast.LENGTH_SHORT).show()
            else -> {
                listener.onLoginClicked(etInstanceUrl.text.toString(), etUsername.text.toString(), etPassword.text.toString(), this)
                displayLoader(true)
            }
        }
    }

    private fun displayLoader(shouldDisplay: Boolean) {
        btnLogin.visibility = if (shouldDisplay) View.INVISIBLE else View.VISIBLE
        pbLoginLoader.visibility = if (shouldDisplay) View.VISIBLE else View.INVISIBLE
    }

    override fun onLoginSuccess() {
        activity?.runOnUiThread {
            displayLoader(false)
        }
    }

    override fun onLoginFailure() {
        activity?.runOnUiThread {
            displayLoader(false)
            Toast.makeText(activity, "Unable to authenticate. Ensure your instance URL is correct and that you are connected to the internet.", Toast.LENGTH_SHORT).show()
        }
    }
}