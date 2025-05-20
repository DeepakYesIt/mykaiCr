package com.mykaimeal.planner.fragment.mainfragment.commonscreen.productpaymentscreen

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.mykaimeal.planner.R
import com.mykaimeal.planner.activity.MainActivity
import com.mykaimeal.planner.adapter.AdapterPaymentCreditDebitItem
import com.mykaimeal.planner.adapter.MonthYearsCardAdapter
import com.mykaimeal.planner.basedata.BaseApplication
import com.mykaimeal.planner.basedata.NetworkResult
import com.mykaimeal.planner.commonworkutils.CommonWorkUtils
import com.mykaimeal.planner.databinding.FragmentPaymentCreditDebitBinding
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.checkoutscreen.viewmodel.CheckoutScreenViewModel
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.productpaymentscreen.model.AddCardMealMeModel
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.productpaymentscreen.model.GetCardMealMeModel
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.productpaymentscreen.model.GetCardMealMeModelData
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.productpaymentscreen.viewmodel.PaymentCreditDebitViewModel
import com.mykaimeal.planner.listener.CardBankListener
import com.mykaimeal.planner.messageclass.ErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class PaymentCreditDebitFragment : Fragment(), CardBankListener {

    private lateinit var binding: FragmentPaymentCreditDebitBinding
    private var checkUnchecked: Boolean? = false
    private lateinit var commonWorkUtils: CommonWorkUtils
    private lateinit var adapterPaymentCreditDebitItem: AdapterPaymentCreditDebitItem
    private var month: Int = 0
    private var year: Int = 0
    private var checkStatus: String = "0"
    private lateinit var paymentCreditDebitViewModel: CheckoutScreenViewModel

    private var cardList: MutableList<GetCardMealMeModelData> = mutableListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentPaymentCreditDebitBinding.inflate(layoutInflater, container, false)
        commonWorkUtils = CommonWorkUtils(requireActivity())
        paymentCreditDebitViewModel = ViewModelProvider(requireActivity())[CheckoutScreenViewModel::class.java]
        adapterPaymentCreditDebitItem = AdapterPaymentCreditDebitItem(requireContext(), cardList, this)
        binding.rcvCardNumber.adapter = adapterPaymentCreditDebitItem
        setupBackNavigation()
        initialize()
        return binding.root
    }

    private fun setupBackNavigation() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            })
    }

    private fun initialize() {

        if (BaseApplication.isOnline(requireActivity())) {
            getCardMealMe()
        } else {
            BaseApplication.alertError(requireContext(), ErrorMessage.networkError, false)
        }

        binding.imgCreditDebit.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.etMonth.setOnClickListener {
            showPopupMonth()
        }

        binding.etYear.setOnClickListener {
            showPopupYears()
        }

        binding.imgCheckUncheck.setOnClickListener {
            checkUnchecked = !checkUnchecked!!
            if (checkUnchecked as Boolean) {
                checkStatus = "1"
                binding.imgCheckUncheck.setImageResource(R.drawable.tick_ckeckbox_images)
            } else {
                checkStatus = "0"
                binding.imgCheckUncheck.setImageResource(R.drawable.uncheck_box_images)
            }
        }

        binding.tvSaveCreditDebitCard.setOnClickListener {
            if (isValidationCard()) {
                if (BaseApplication.isOnline(requireActivity())) {
                    cardSaveApi()
                } else {
                    BaseApplication.alertError(requireContext(), ErrorMessage.networkError, false)
                }
            }
        }

        binding.textAddCardNumber.setOnClickListener {
            binding.cvDebitCard3.visibility = View.VISIBLE
            binding.textAddCardNumber.visibility = View.GONE
        }
    }


    @SuppressLint("MissingInflatedId")
    private fun showPopupYears() {
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val popupView: View? = inflater?.inflate(R.layout.item_month_years, null)
        val popupWindow = PopupWindow(popupView, binding.etYear.width, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
        popupWindow.showAsDropDown(binding.etYear, 0, 0, Gravity.CENTER)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearList = (currentYear..currentYear + 29).map { it.toString() }
        yearList as MutableList
        // Access views inside the inflated layout using findViewById
        val rcyData = popupView?.findViewById<RecyclerView>(R.id.rcyData)
        val adapterMonth= MonthYearsCardAdapter(yearList ,requireContext()){
                data->
            binding.etYear.text=data
            popupWindow.dismiss()
        }
        rcyData?.adapter=adapterMonth
    }


    @SuppressLint("MissingInflatedId")
    private fun showPopupMonth() {
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val popupView: View? = inflater?.inflate(R.layout.item_month_years, null)
        val popupWindow = PopupWindow(popupView, binding.etMonth.width, RelativeLayout.LayoutParams.WRAP_CONTENT, true)
        popupWindow.showAsDropDown(binding.etMonth, 0, 0, Gravity.CENTER)
        val monthList= mutableListOf("01","02","03","04","05","06","07","08","09","10","11","12")
        // Access views inside the inflated layout using findViewById
        val rcyData = popupView?.findViewById<RecyclerView>(R.id.rcyData)
        val adapterMonth= MonthYearsCardAdapter(monthList,requireContext()){
            data->
            binding.etMonth.text=data
            popupWindow.dismiss()
        }
        rcyData?.adapter=adapterMonth
    }

    private fun getCardMealMe() {
        if (BaseApplication.isOnline(requireActivity())) {
            fetchUserCardData()
        } else {
            BaseApplication.alertError(requireContext(), ErrorMessage.networkError, false)
        }
    }

    private fun fetchUserCardData() {
        BaseApplication.showMe(requireContext())
        lifecycleScope.launch {
            paymentCreditDebitViewModel.getCardMealMeUrl { result ->
                BaseApplication.dismissMe()
                handleGetCardApiResponse(result)
            }
        }
    }

    private fun handleGetCardApiResponse(result: NetworkResult<String>) {
        when (result) {
            is NetworkResult.Success -> processGetCardSuccessResponse(result.data.toString())
            is NetworkResult.Error -> showAlert(result.message, false)
            else -> showAlert(result.message, false)
        }
    }

    private fun processGetCardSuccessResponse(response: String) {
        try {
            val apiModel = Gson().fromJson(response, GetCardMealMeModel::class.java)
            Log.d("@@@ Response cardBank ", "message :- $response")
            if (apiModel.code == 200 && apiModel.success == true) {
                showDataInUi(apiModel.data)
            } else {
                handleError(apiModel.code!!,apiModel.message!!)
            }
        } catch (e: Exception) {
            showAlert(e.message, false)
        }
    }

    private fun showDataInUi(data: MutableList<GetCardMealMeModelData>?) {
        cardList.clear()
        data?.let {
            cardList.addAll(it)
        }
        hideShow()
    }

    private fun cardSaveApi() {
        BaseApplication.showMe(requireContext())
        try {
            val type = BaseApplication.detectCardType(binding.etCardNumber.text.toString().trim())
            lifecycleScope.launch {
                paymentCreditDebitViewModel.addCardMealMeUrl({
                    BaseApplication.dismissMe()
                    handleApiAddCardResponse(it)
                },  binding.etCardNumber.text.toString().trim(),
                    binding.etMonth.text.toString().trim(),
                    binding.etYear.text.toString().trim(),
                    binding.etCVVNumber.text.toString().trim(), checkStatus,type
                )
            }
        }catch (e:Exception){
            BaseApplication.dismissMe()
        }
    }

    private fun handleApiAddCardResponse(result: NetworkResult<String>) {
        when (result) {
            is NetworkResult.Success -> handleUpdateAddCardResponse(result.data.toString())
            is NetworkResult.Error -> showAlert(result.message, false)
            else -> showAlert(result.message, false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleUpdateAddCardResponse(data: String) {
        try {
            val apiModel = Gson().fromJson(data, AddCardMealMeModel::class.java)
            Log.d("@@@ Add Card", "message :- $data")
            if (apiModel.code == 200 && apiModel.success) {
                month = 0
                year = 0
                binding.etCardName.text.clear()
                binding.etCardNumber.text.clear()
                binding.etCVVNumber.text.clear()
                binding.etMonth.text = "Month"
                binding.etYear.text = "Year"
                Toast.makeText(requireContext(), apiModel.message, Toast.LENGTH_LONG).show()
                (activity as MainActivity?)?.upBasketCheckOut()
                // When screen load then api call
                getCardMealMe()
            } else {
               handleError(apiModel.code,apiModel.message)
            }
        } catch (e: Exception) {
            showAlert(e.message, false)
        }

    }

    private fun handleError(code: Int, message: String) {
        if (code == ErrorMessage.code) {
            showAlert(message, true)
        } else {
            showAlert(message, false)
        }
    }

    private fun showAlert(message: String?, status: Boolean) {
        BaseApplication.alertError(requireContext(), message, status)
    }



    private fun isValidationCard(): Boolean {
        if (binding.etCardName.text.toString().trim().isEmpty()) {
            BaseApplication.alertError(requireContext(), ErrorMessage.cardholderError, false)
            return false
        } else if (binding.etCardNumber.text.toString().trim().isEmpty()) {
            BaseApplication.alertError(requireContext(), ErrorMessage.cardNumberError, false)
            return false
        } else if (binding.etCVVNumber.text.toString().trim().isEmpty()) {
            BaseApplication.alertError(requireContext(), ErrorMessage.cvvError, false)
            return false
        } else if (binding.etCVVNumber.text.toString().length == 1 || binding.etCVVNumber.text.toString().length == 2) {
            BaseApplication.alertError(requireContext(), ErrorMessage.cvvValidError, false)
            return false
        } else if (binding.etMonth.text.toString().equals("Month", true)) {
            BaseApplication.alertError(requireContext(), ErrorMessage.monthError, false)
            return false
        } else if (binding.etYear.text.toString().equals("Year", true)) {
            BaseApplication.alertError(requireContext(), ErrorMessage.yearError, false)
            return false
        }

        return true
    }

    override fun itemSelect(position: Int?, status: String?, type: String?) {
        if (BaseApplication.isOnline(requireActivity())) {
            if (type!!.equals("delete", true)) {
                deleteApi(status)
            }
            if (type.equals("preferred", true)) {
                preferredApi(status)
            }
        } else {
            BaseApplication.alertError(requireContext(), ErrorMessage.networkError, false)
        }


    }

    private fun preferredApi(status: String?) {
        BaseApplication.showMe(requireContext())
        lifecycleScope.launch {
            paymentCreditDebitViewModel.setPreferredCardMealMeUrl({
                BaseApplication.dismissMe()
                handleApiPreferredCardResponse(it,status)
            }, status)
        }
    }

    private fun handleApiPreferredCardResponse(result: NetworkResult<String>,cardId:String?) {
        when (result) {
            is NetworkResult.Success -> handleUpdatePreferredResponse(result.data.toString(),cardId)
            is NetworkResult.Error -> showAlert(result.message, false)
            else -> showAlert(result.message, false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleUpdatePreferredResponse(data: String,cardId: String?) {
        try {
            val apiModel = Gson().fromJson(data, AddCardMealMeModel::class.java)
            Log.d("@@@ Add Card", "message :- $data")
            if (apiModel.code == 200 && apiModel.success) {
                cardList.forEachIndexed { index, card ->
                    card.status=if (card.id == cardId?.toInt()) 1 else 0
                    cardList[index] = card
                }
                adapterPaymentCreditDebitItem.upDateList(cardList)
                paymentCreditDebitViewModel.dataCheckOut?.card?.forEachIndexed { index, card ->
                    card.status = if (card.id == cardId?.toInt()) 1 else 0
                    paymentCreditDebitViewModel.dataCheckOut?.card?.set(index, card)
                }
                Toast.makeText(requireContext(), apiModel.message, Toast.LENGTH_LONG).show()
            } else {
                handleError(apiModel.code,apiModel.message)
            }
        } catch (e: Exception) {
            showAlert(e.message, false)
        }

    }

    private fun deleteApi(status: String?) {
        BaseApplication.showMe(requireContext())
        lifecycleScope.launch {
            paymentCreditDebitViewModel.deleteCardMealMeUrl({
                BaseApplication.dismissMe()
                handleApiDeleteCardResponse(it,status)
            }, status)
        }
    }

    private fun handleApiDeleteCardResponse(result: NetworkResult<String>,cardId:String?) {
        when (result) {
            is NetworkResult.Success -> handleUpdateDeleteResponse(result.data.toString(),cardId)
            is NetworkResult.Error -> showAlert(result.message, false)
            else -> showAlert(result.message, false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleUpdateDeleteResponse(data: String,cardId:String?) {
        try {
            val apiModel = Gson().fromJson(data, AddCardMealMeModel::class.java)
            Log.d("@@@ Add Card", "message :- $data")
            if (apiModel.code == 200 && apiModel.success) {
                Toast.makeText(requireContext(), apiModel.message, Toast.LENGTH_LONG).show()
                val index = cardList.indexOfFirst { it.id == cardId?.toInt() }
                cardList.removeAt(index)
                paymentCreditDebitViewModel.dataCheckOut?.card?.removeAll { it.id == cardId?.toInt() }
                hideShow()
            } else {
               handleError(apiModel.code,apiModel.message)
            }
        } catch (e: Exception) {
            showAlert(e.message, false)
        }

    }

    private fun hideShow(){
        if (cardList.size > 0) {
            binding.cvDebitCard3.visibility = View.GONE
            binding.textAddCardNumber.visibility = View.VISIBLE
            binding.relSavedCards.visibility = View.VISIBLE
            adapterPaymentCreditDebitItem.upDateList(cardList)
        } else {
            binding.cvDebitCard3.visibility = View.VISIBLE
            binding.textAddCardNumber.visibility = View.GONE
            binding.relSavedCards.visibility = View.GONE
        }
    }

}