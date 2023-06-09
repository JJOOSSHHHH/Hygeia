package com.hygeia.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hygeia.ActMachine
import com.hygeia.ActSales
import com.hygeia.adapters.ArrAdpMachines
import com.hygeia.classes.ButtonType
import com.hygeia.classes.DataMachines
import com.hygeia.databinding.FrgMainAdminToolsBinding
import com.hygeia.objects.MachineManager
import com.hygeia.objects.UserManager
import com.hygeia.objects.Utilities.dlgConfirmation
import com.hygeia.objects.Utilities.dlgLoading
import com.hygeia.objects.Utilities.dlgStatus
import com.hygeia.objects.Utilities.isInternetConnected
import com.hygeia.objects.Utilities.msg
import java.util.Calendar
import java.util.Date
import java.util.Random

class FrgMainAdminTools : Fragment(), ArrAdpMachines.OnMachineItemClickListener {
    private lateinit var bind: FrgMainAdminToolsBinding
    private lateinit var listOfMachines: ArrayList<DataMachines>
    private lateinit var loading: Dialog

    private var db = FirebaseFirestore.getInstance()
    private var machinesRef = db.collection("Machines")
    private var historyRef = db.collection("History")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bind = FrgMainAdminToolsBinding.inflate(layoutInflater, container, false)
        loading = dlgLoading(requireContext())

        with(bind) {
            if (isInternetConnected(requireContext())) {
                getListOfMachines()
            } else {
                dlgStatus(requireContext(), "no internet").show()
            }

            return root
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getListOfMachines() {
        bind.cover.visibility = View.VISIBLE

        bind.lblMessage.text = "Fetching all your vending machines, please wait..."
        bind.loading.visibility = View.VISIBLE
        bind.listViewMachines.layoutManager = LinearLayoutManager(requireContext())
        listOfMachines = arrayListOf()

        val query = machinesRef.whereEqualTo("isEnabled", true).orderBy("Name")

        query.get().apply {
            addOnSuccessListener { data ->
                listOfMachines.clear()
                bind.cover.visibility = View.GONE
                if (!data.isEmpty) {
                    for (item in data.documents) {
                        val machine: DataMachines? = item.toObject(DataMachines::class.java)
                        listOfMachines.add(machine!!)
                    }
                }
                val newMachine = DataMachines("New Machine")
                listOfMachines.add(newMachine)

                bind.listViewMachines.adapter = ArrAdpMachines(
                    listOfMachines, this@FrgMainAdminTools
                )
            }
            addOnFailureListener {
                requireContext().msg("Please try again.")
            }
        }
    }

    private fun addVendingMachine() {
        val machineId = getRandomId()
        val vendingMachineData = hashMapOf(
            "Location" to "Location Not Set",
            "MachineID" to machineId,
            "Status" to "Offline",
            "User Connected" to 0,
            "isEnabled" to true
        )
        getCurrentVendingMachineCount { count ->
            if (count != -1) {
                val vendingMachineName = generateVendingMachineName(count)
                vendingMachineData["Name"] = vendingMachineName
                machinesRef.document(machineId).set(vendingMachineData).addOnSuccessListener {
                    val historyData = hashMapOf(
                        "Content" to "Added new vending machine, Vendo No. $vendingMachineName.",
                        "Date Created" to Timestamp(Date()),
                        "Full Name" to "${UserManager.firstname} ${UserManager.lastname}",
                        "Machine ID" to machineId,
                        "Machine Name" to vendingMachineName,
                        "Type" to "Created",
                        "User Reference" to UserManager.uid
                    )
                    historyRef.document().set(historyData)
                    dlgStatus(requireContext(), "success adding vending machine").apply {
                        loading.dismiss()
                        setOnDismissListener {
                            createProductDocuments(vendingMachineData["MachineID"].toString())
                            getListOfMachines()
                        }
                        show()
                    }
                }
            }
        }
    }

    private fun createProductDocuments(vendingMachineId: String) {
        val productsCollection = machinesRef.document(vendingMachineId).collection("Products")
        for (i in 1..3) {
            val productName = "Item $i"
            val productData = hashMapOf(
                "Name" to productName,
                "Price" to 1,
                "Price in Points" to 1,
                "Quantity" to 0,
                "Reward Points" to 1,
                "Slot" to i,
                "Status" to "0"
            )
            productsCollection.document().set(productData, SetOptions.merge())
        }
    }

    private fun getRandomId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = Random()
        val sb = StringBuilder()
        for (i in 0 until 20) { // Generate a 20-character UUID
            val index = random.nextInt(chars.length)
            sb.append(chars[index])
        }
        return sb.toString()
    }

    private fun generateVendingMachineName(count: Int): String {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val vendingMachineCount = count + 1
        return "$currentYear-${vendingMachineCount.toString().padStart(4, '0')}"
    }

    private fun getCurrentVendingMachineCount(callback: (Int) -> Unit) {
        machinesRef.get()
            .addOnSuccessListener {
                val count = it.size()
                callback(count)
            }
    }

    override fun onResume() {
        super.onResume()
        getListOfMachines()
    }

    override fun onMachineInventoryItemClick(machineID: String) {
        loading.show()
        machinesRef.document(machineID.trim()).get().addOnSuccessListener { data ->
            MachineManager.setMachineInformation(data)
            startActivity(Intent(requireContext(), ActMachine::class.java))
            loading.dismiss()
        }
    }

    override fun onMachineSalesItemClick(machineID: String) {
        loading.show()
        machinesRef.document(machineID.trim()).get().addOnSuccessListener { data ->
            MachineManager.setMachineInformation(data)
            startActivity(Intent(requireContext(), ActSales::class.java))
            loading.dismiss()
        }
    }

    override fun onMachineAddItemClick() {
        if (isInternetConnected(requireContext())) {
            dlgConfirmation(requireContext(), "add vending machine") {
                if (it == ButtonType.PRIMARY) {
                    loading.show()
                    addVendingMachine()
                }
            }.show()
        } else {
            dlgStatus(requireContext(), "no internet").show()
        }
    }
}