package com.hygeia.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hygeia.ActPurchaseUsingStars
import com.hygeia.ActQrCodeScanner
import com.hygeia.ActRequestMoney
import com.hygeia.ActSendMoney
import com.hygeia.adapters.ArrAdpTransactions
import com.hygeia.classes.ButtonType
import com.hygeia.classes.DataTransactions
import com.hygeia.databinding.ActPurchaseUsingStarsBinding
import com.hygeia.objects.Utilities.dlgInformation
import com.hygeia.objects.Utilities.greetings
import com.hygeia.databinding.FrgMainHomeBinding
import com.hygeia.objects.UserManager
import com.hygeia.objects.Utilities
import com.hygeia.objects.Utilities.dlgConfirmation
import com.hygeia.objects.Utilities.dlgError
import com.hygeia.objects.Utilities.dlgMyQrCode
import com.hygeia.objects.Utilities.dlgStatus
import com.hygeia.objects.Utilities.formatCredits
import com.hygeia.objects.Utilities.formatPoints
import com.hygeia.objects.Utilities.isInternetConnected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FrgMainHome : Fragment(), ArrAdpTransactions.OnTransactionItemClickListener {
    private lateinit var bind: FrgMainHomeBinding
    private lateinit var listOfTransactions: ArrayList<DataTransactions>
    private lateinit var loading: Dialog

    private var db = FirebaseFirestore.getInstance()
    private var userRef = db.collection("User")
    private var transactionRef = db.collection("Transactions")

    private var balance = 0.0
    private var points = 0.0
    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        loading = Utilities.dlgLoading(requireContext())
        bind = FrgMainHomeBinding.inflate(inflater, container, false)
        val (language, greeting) = greetings.entries.random()

        lifecycleScope.launch(Dispatchers.Main) {
            val query = userRef.document(UserManager.uid!!).get().await()
            balance = query.get("balance").toString().toDouble()
            points = query.get("points").toString().toDouble()
        }

        constraintViews()

        with(bind) {

            if (isInternetConnected(requireContext())){
                //POPULATE
                populateMainHome()
                getListOfTransactions()
                val resourceId =
                    resources.getIdentifier(UserManager.walletBackground, "drawable", requireContext().packageName)
                cardWalletBackground.setBackgroundResource(resourceId)
                lblGreetings.text = greeting
                //MAIN FUNCTIONS
                lblGreetings.setOnClickListener {
                    dlgInformation(requireContext(), language).show()
                }
                lblGreetings.text = greeting
                //MAIN FUNCTIONS
                lblGreetings.setOnClickListener {
                    dlgInformation(requireContext(), language).show()
                }
                //BTN LOGOUT
                requireActivity().onBackPressedDispatcher.addCallback(
                    viewLifecycleOwner,
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            onBackPressed()
                        }
                    })
            } else {
                dlgStatus(requireContext(),"no internet").show()
            }

            cardPoints.setOnClickListener {
                if (isInternetConnected(requireContext())){
                    dlgInformation(requireContext(), "introduce stars").show()
                }else{
                    dlgStatus(requireContext(),"no internet").show()
                }
            }

            btnMyQr.setOnClickListener {
                if (isInternetConnected(requireContext())){
                    dlgMyQrCode(requireContext(), UserManager.phoneNumber.toString()).show()
                }else{
                    dlgStatus(requireContext(),"no internet").show()
                }
            }
            cardWallet.setOnClickListener {
                if (isInternetConnected(requireContext())){
                    loading.show()
                    lifecycleScope.launch(Dispatchers.Main) {
                        getWalletBalance()
                    }
                    getListOfTransactions()
                } else {
                    dlgStatus(requireContext(),"no internet").show()
                }
            }
            //NAVIGATION
            btnSendMoney.setOnClickListener {
                if (isInternetConnected(requireContext())){
                    startActivity(Intent(requireContext(), ActSendMoney::class.java))
                }else{
                    dlgStatus(requireContext(),"no internet").show()
                }
            }
            
            btnRequestMoney.setOnClickListener {
              if (isInternetConnected(requireContext())){
                    startActivity(Intent(requireContext(), ActRequestMoney::class.java))
                }else{
                    dlgStatus(requireContext(),"no internet").show()
                }
            }
            btnPurchase.setOnClickListener {
                if (isInternetConnected(requireContext())){
                    if (balance == 0.0){
                        dlgStatus(requireContext(),"0 funds").show()
                    }else {
                        val intent = Intent(requireActivity(), ActQrCodeScanner::class.java)
                        intent.putExtra("From ActQrCodeScanner", "ActPurchase")
                        requireActivity().startActivity(intent)
                    }
                } else {
                    dlgStatus(requireContext(),"no internet").show()
                }
            }
            btnPurchaseUsingPoints.setOnClickListener {
                if (isInternetConnected(requireContext())){
                    if (points == 0.0){
                        dlgStatus(requireContext(),"0 points").show()
                    }else {
                        val intent = Intent(requireActivity(), ActQrCodeScanner::class.java)
                        intent.putExtra("From ActQrCodeScanner", "ActPurchaseUsingStars")
                        requireActivity().startActivity(intent)
                    }
                } else{
                    dlgStatus(requireContext(),"no internet").show()
                }
            }
            return root
        }
    }

    private fun getListOfTransactions() {
        loading.show()
        bind.listViewTransactionHistory.layoutManager = LinearLayoutManager(requireContext())
        listOfTransactions = arrayListOf()

        val query = transactionRef.whereEqualTo("User Reference", UserManager.uid).limit(3).orderBy("Date Created", Query.Direction.DESCENDING)

        query.get().apply {
            addOnSuccessListener { data ->
                listOfTransactions.clear()
                if (!data.isEmpty) {
                    bind.coverTransaction.visibility = View.GONE
                    for (item in data.documents) {
                        val transactionId = item.id
                        val transactionAmount = item.get("Amount")
                        val transactionDate: Timestamp = item.get("Date Created") as Timestamp
                        val transactionNumber = item.get("Number")
                        val transactionReference = item.get("Reference Number")
                        val transactionType = item.get("Type")

                        val transaction = DataTransactions(
                            transactionId,
                            transactionAmount,
                            transactionDate,
                            transactionNumber.toString(),
                            transactionReference.toString(),
                            transactionType.toString()
                        )

                        listOfTransactions.add(transaction)
                    }
                    bind.listViewTransactionHistory.adapter =
                        ArrAdpTransactions(listOfTransactions, this@FrgMainHome)
                }
                loading.dismiss()
            }
        }
    }

    private suspend fun getWalletBalance() {
        val query = userRef.document(UserManager.uid!!).get().await()
        val balance = formatCredits(query.get("balance"))
        bind.lblAmountBalance.text = balance
        loading.dismiss()
    }

    private fun populateMainHome() {
        loading.show()
        userRef.document(UserManager.uid!!).get().addOnSuccessListener {
            UserManager.setUserInformation(it)
            val fullname = "${UserManager.firstname} ${UserManager.lastname}"
            val balance = formatCredits(UserManager.balance)
            val points = formatPoints(UserManager.points.toString().toDouble())

            bind.lblUserFullName.text = fullname
            bind.lblAmountBalance.text = balance
            bind.lblHygeiaPoints.text = points
            loading.dismiss()
        }
    }

    private fun onBackPressed() {
        dlgConfirmation(requireContext(), "log out") {
            if (it == ButtonType.PRIMARY) {
                userRef.document(UserManager.uid!!).update("isOnline", false)
                    .addOnSuccessListener {
                        requireActivity().finish()
                    }
            }
        }.show()
    }
    
    private fun constraintViews() {
        with(bind) {
            when (UserManager.role) {
                "super admin" -> {
                    btnPurchaseUsingPoints.visibility = View.GONE
                    lblPurchaseUsingPoints.visibility = View.GONE
                    btnPurchase.visibility = View.GONE
                    lblPurchase.visibility = View.GONE
                    btnMyQr.visibility = View.GONE
                    lblMyQr.visibility = View.GONE
                    btnRequestMoney.visibility = View.VISIBLE
                    lblRequestMoney.visibility = View.VISIBLE
                    cardPoints.visibility = View.GONE
                }
                "standard" -> {
                    btnRequestMoney.visibility = View.GONE
                    lblRequestMoney.visibility = View.GONE
                }
                "admin" -> {
                    btnPurchaseUsingPoints.visibility = View.GONE
                    lblPurchaseUsingPoints.visibility = View.GONE
                    btnPurchase.visibility = View.GONE
                    lblPurchase.visibility = View.GONE
                    btnRequestMoney.visibility = View.GONE
                    lblRequestMoney.visibility = View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        constraintViews()
        populateMainHome()
        getListOfTransactions()
    }

    override fun onTransactionItemClick(ID: String) {
        Utilities.dlgTransactionDetails(requireContext(), ID).show()
    }
}