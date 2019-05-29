package com.coolfiresolutions.roninchat.common

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import com.coolfiresolutions.roninchat.RoninChatApplication

abstract class BaseFragment : Fragment() {
    open fun getWindowSoftInputMode(): Int {
        return WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.window?.setSoftInputMode(getWindowSoftInputMode())
    }

    fun getApplication(): RoninChatApplication {
        return activity?.application as RoninChatApplication
    }

    protected fun hideKeyboard() {
        val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                ?: return
        inputManager.hideSoftInputFromWindow(view?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}