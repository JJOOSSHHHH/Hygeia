package com.hygeia.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hygeia.adapters.ArrAdpUsers
import com.hygeia.classes.ButtonType
import com.hygeia.classes.DataUsers
import com.hygeia.databinding.FrgMainUserAccountsBinding
import com.hygeia.objects.Utilities
import com.hygeia.objects.Utilities.dlgConfirmation
import com.hygeia.objects.Utilities.dlgStatus

class FrgMainUserAccounts : Fragment(), ArrAdpUsers.OnUserClickListener {
    private lateinit var bind: FrgMainUserAccountsBinding
    private lateinit var listOfUsers: ArrayList<DataUsers>
    private lateinit var loading: Dialog

    private var db = FirebaseFirestore.getInstance()
    private var userRef = db.collection("User")

    private val query = userRef.whereIn("role", listOf("standard", "admin"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = FrgMainUserAccountsBinding.inflate(inflater, container, false)
        loading = Utilities.dlgLoading(requireContext())

        with(bind) {
            getListOfUsers(query)

            txtLayoutSearch.setEndIconOnClickListener {
                getListOfUsers(query.whereEqualTo("phoneNumber", "${txtLayoutSearch.prefixText}${txtSearch.text.toString()}"))
            }

            mainLayout.setOnClickListener {
                requireContext().getSystemService(InputMethodManager::class.java).apply {
                    hideSoftInputFromWindow(requireView().findFocus()?.windowToken, 0)
                }
                mainLayout.requestFocus()
                requireView().findFocus()?.clearFocus()
            }

            return root
        }
    }

    private fun getListOfUsers(query: Query) {
        loading.show()
        bind.listViewUsers.layoutManager = LinearLayoutManager(requireContext())
        listOfUsers = arrayListOf()

        query.orderBy("dateCreated", Query.Direction.DESCENDING).get().apply {
            addOnSuccessListener { data ->
                listOfUsers.clear()
                if (!data.isEmpty) {
                    for (item in data.documents) {
                        val userId = item.id
                        val userIdentifier = item.get("phoneNumber")
                        val userRole = item.get("role")
                        val userEnable = item.get("isEnabled")

                        val user = DataUsers(
                            userId,
                            userIdentifier.toString(),
                            userRole.toString(),
                            userEnable.toString()
                        )

                        listOfUsers.add(user)
                    }
                    bind.listViewUsers.adapter =
                        ArrAdpUsers(listOfUsers, this@FrgMainUserAccounts)
                    bind.coverListView.visibility = View.GONE
                } else {
                    bind.coverListView.visibility = View.VISIBLE
                }
                loading.dismiss()
            }
            addOnFailureListener {
                Utilities.dlgError(requireContext(), it.toString()).show()
                loading.dismiss()
            }
        }
    }

    private fun updateUserRole(userID: String, role: String) {
        loading.show()
        userRef.document(userID).update("role", role).addOnSuccessListener {
            loading.dismiss()
            dlgStatus(requireContext(), "success update user").apply {
                setOnDismissListener {
                    getListOfUsers(query)
                }
                show()
            }
        }
    }

    private fun updateUserStatus(userID: String, status: Boolean) {
        loading.show()
        userRef.document(userID).update("isEnabled", status).addOnSuccessListener {
            loading.dismiss()
            dlgStatus(requireContext(), "success update user").apply {
                setOnDismissListener {
                    getListOfUsers(query)
                }
                show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        bind.txtSearch.setText("")
    }

    override fun onUserUpdateRoleClick(userID: String, currentRole: String) {
        if (currentRole == "standard") {
            dlgConfirmation(requireContext(), "promote") {
                if (it == ButtonType.PRIMARY) {
                    updateUserRole(userID, "admin")
                }
            }.show()
        } else if (currentRole == "admin") {
            dlgConfirmation(requireContext(), "demote") {
                if (it == ButtonType.PRIMARY) {
                    updateUserRole(userID, "standard")
                }
            }.show()
        }
    }

    override fun onUserUpdateStatusClick(userID: String, isEnabled: String) {
        if (isEnabled == "true") {
            dlgConfirmation(requireContext(), "disable account") {
                if (it == ButtonType.PRIMARY) {
                    updateUserStatus(userID, false)
                }
            }.show()
        } else if (isEnabled == "false") {
            updateUserStatus(userID, true)
        }
    }
}