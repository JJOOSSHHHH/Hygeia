package com.hygeia.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import com.hygeia.R
import com.hygeia.databinding.FrgCreateAccountPart1Binding
import java.util.*

class FrgCreateAccountPart1 : Fragment() {
    private lateinit var bind : FrgCreateAccountPart1Binding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        bind = FrgCreateAccountPart1Binding.inflate(inflater, container, false)

        with(bind) {
            txtNewBirthDate.setOnClickListener { btnCalendar.performClick() }
            btnCalendar.setOnClickListener { showDatePickerDialog() }

            //ELEMENT BEHAVIOR
            mainLayout.setOnClickListener {
                requireActivity().getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(requireView().findFocus()?.windowToken, 0)
                mainLayout.requestFocus()
                requireView().findFocus()?.clearFocus()
            }

            //VALIDATION
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
                override fun afterTextChanged(s: Editable?) {
                    btnContinueCreateAccountToPart2.isEnabled = txtNewFirstName.text.isNotEmpty() and
                            txtNewLastName.text.isNotEmpty() and
                            txtNewBirthDate.text.isNotEmpty()
                }
            }
            txtNewFirstName.addTextChangedListener(textWatcher)
            txtNewLastName.addTextChangedListener(textWatcher)
            txtNewBirthDate.addTextChangedListener(textWatcher)

            //NAVIGATION
            btnContinueCreateAccountToPart2.setOnClickListener {
                sendArguments()
            }

            btnBackToLogin.setOnClickListener {
                activity?.onBackPressed()
            }
        }
        return bind.root
    }

    private fun sendArguments() {
        with(bind) {
            val bundle = Bundle().apply {
                putString("gender", cmbGender.selectedItem.toString())
                putString("firstname", txtNewFirstName.text.toString())
                putString("lastname", txtNewLastName.text.toString())
                putString("birthdate", txtNewBirthDate.text.toString())
            }
            val fragment = FrgCreateAccountPart2().apply { arguments = bundle }
            fragmentManager?.beginTransaction()
                ?.setCustomAnimations(androidx.appcompat.R.anim.abc_fade_in, androidx.appcompat.R.anim.abc_fade_out)
                ?.replace(R.id.containerCreateAccount, fragment)
                ?.addToBackStack(null)
                ?.commit()
        }
    }

    private fun showDatePickerDialog() {
        val currentDate = Calendar.getInstance()
        val datePicker = DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
            val age = currentDate.get(Calendar.YEAR) - selectedDate.get(Calendar.YEAR) -
                    if (selectedDate.get(Calendar.DAY_OF_YEAR) > currentDate.get(Calendar.DAY_OF_YEAR)) 1 else 0
            with(bind) {
                val date = "$day/${month+1}/$year"
                if (age >= 11) {
                    txtNewBirthDate.setText(date)
                    txtNewBirthDate.setBackgroundResource(R.drawable.bg_textfield_default)
                    lblCreateAccountErrorMsg1.visibility = View.GONE
                } else {
                    txtNewBirthDate.setBackgroundResource(R.drawable.bg_textfield_error)
                    lblCreateAccountErrorMsg1.visibility = View.VISIBLE
                }
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH))
        datePicker.show()
    }
}