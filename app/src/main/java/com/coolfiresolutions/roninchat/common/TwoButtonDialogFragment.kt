package com.coolfiresolutions.roninchat.common

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.coolfiresolutions.roninchat.R
import kotlinx.android.synthetic.main.dialog_two_button.*

class TwoButtonDialogFragment : DialogFragment() {
    private lateinit var bundle: Bundle
    private lateinit var listener: TextTwoButtonDialogListener

    companion object {
        private const val BUNDLE = "bundle"
        private const val CANCEL_BUTTON_TEXT = "cancelButtonText"
        private const val OK_BUTTON_TEXT = "okButtonText"
        private const val BODY_TEXT = "bodyText"
        const val RESPONSE = "response"
        private const val DIALOG_DISMISSED = "dialogDismissed"

        val TAG = TwoButtonDialogFragment::class.java.canonicalName!!

        fun newInstance(body: String, okButtonText: String, cancelButtonText: String, bundle: Bundle): TwoButtonDialogFragment {
            val fragment = TwoButtonDialogFragment()

            // Supply text and bundle for input/callbacks
            val args = Bundle()
            args.putString(BODY_TEXT, body)
            args.putString(OK_BUTTON_TEXT, okButtonText)
            args.putString(CANCEL_BUTTON_TEXT, cancelButtonText)
            args.putBundle(BUNDLE, bundle)
            fragment.arguments = args

            return fragment
        }
    }

    interface TextTwoButtonDialogListener {
        fun onDialogAction(bundle: Bundle)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = if (targetFragment != null) {
            targetFragment as TextTwoButtonDialogListener
        } else {
            context as TextTwoButtonDialogListener
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bundle = arguments!!.getBundle(BUNDLE)!!
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_two_button, container, false)
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        bundle.putBoolean(DIALOG_DISMISSED, true)
        listener.onDialogAction(bundle)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.let {
            tvDetailsText.text = it.getString(BODY_TEXT)
            btnOk.text = it.getString(OK_BUTTON_TEXT)
            btnCancel.text = it.getString(CANCEL_BUTTON_TEXT)
        }

        btnCancel.setOnClickListener {
            onCanceledClicked()
        }

        btnOk.setOnClickListener {
            onOkClicked()
        }
    }

    private fun onOkClicked() {
        bundle.putBoolean(RESPONSE, true)
        bundle.putBoolean(DIALOG_DISMISSED, false)
        listener.onDialogAction(bundle)
        dismiss()
    }

    private fun onCanceledClicked() {
        dismiss()
    }
}
