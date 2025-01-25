package fr.julien.quievreux.droidplane2.ui.view

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import fr.julien.quievreux.droidplane2.R

class EditDescriptionDialog : DialogFragment() {
    private val editDescriptionViewModel: EditDescriptionViewModel by viewModels()
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var edtNodeDescription: EditText
    private var submitListener: ((String) -> (Unit))? = null
    private var description: String = ""

    fun setDescription(description: String) {
        this.description = description
    }

    fun setSubmitListener(submitListener: ((String) -> (Unit))) {
        this.submitListener = submitListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog.window?.attributes?.windowAnimations = R.style.AnimationDialogStyle
        isCancelable = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.dialog_edit_description, container, false)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnSubmit = view.findViewById(R.id.btn_submit)
        edtNodeDescription = view.findViewById(R.id.et_node_description)
        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setViewModel()
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        resizeDialog()
    }

    private fun setViewModel() {
        submitListener?.let {
            editDescriptionViewModel.submitListener = this.submitListener
        }
        if (description.isNotEmpty()) {
            editDescriptionViewModel.setDescription(description)
        }
    }

    private fun setupViews() {
        btnSubmit.setOnClickListener {
            editDescriptionViewModel.submitListener?.invoke(editDescriptionViewModel.description.value)
            dismiss()
        }
        btnCancel.setOnClickListener {
            dismiss()
        }
        edtNodeDescription.apply {
            addTextChangedListener { newText ->
                editDescriptionViewModel.onDescriptionChanged(newText.toString())
            }
            setText(editDescriptionViewModel.description.value)
        }
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = dialog?.window?.attributes

        val displayMetrics = requireActivity().resources.displayMetrics
        val deviceWidth = displayMetrics.widthPixels
        val deviceHeight = displayMetrics.heightPixels

        params?.width = (deviceWidth * 0.8).toInt()
        params?.height = (deviceHeight * 0.5).toInt()
        dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}
