package com.mykaimeal.planner.fragment.mainfragment.commonscreen.addressmapfullscreen

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.gson.Gson
import com.mykaimeal.planner.R
import com.mykaimeal.planner.activity.MainActivity
import com.mykaimeal.planner.basedata.BaseApplication
import com.mykaimeal.planner.basedata.NetworkResult
import com.mykaimeal.planner.basedata.SessionManagement
import com.mykaimeal.planner.commonworkutils.CommonWorkUtils
import com.mykaimeal.planner.databinding.FragmentAddressMapFullScreenBinding
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.addressmapfullscreen.model.AddAddressModel
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.addressmapfullscreen.viewmodel.AddressMapFullScreenViewModel
import com.mykaimeal.planner.messageclass.ErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

@AndroidEntryPoint
class AddressMapFullScreenFragment : Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentAddressMapFullScreenBinding
    private lateinit var sessionManagement: SessionManagement
    private lateinit var addressMapFullScreenViewModel: AddressMapFullScreenViewModel
    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null
    private lateinit var edtStreetName: EditText
    private lateinit var edtStreetNumber: EditText
    private lateinit var edtApartNumber: EditText
    private lateinit var edtCity: EditText
    private lateinit var edtStates: EditText
    private lateinit var edtPostalCode: EditText
    private lateinit var edtAddress: EditText
    private var userAddress: String? = ""
    private var address: String? = ""
    private var setStatus: String? = "Home"
    private var latitude: String? = ""
    private var longitude: String? = ""
    private var streetName: String? = ""
    private var streetNum: String? = ""
    private var apartNum: String? = ""
    private var city: String? = ""
    private var states: String? = ""
    private var country: String? = ""
    private var zipcode: String? = ""

    private var screenType: String? = ""
    private var addressId: String? = ""
    private var selectType: String? = "Home"

    private lateinit var commonWorkUtils: CommonWorkUtils

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentAddressMapFullScreenBinding.inflate(layoutInflater, container, false)

        addressMapFullScreenViewModel =
            ViewModelProvider(requireActivity())[AddressMapFullScreenViewModel::class.java]

        commonWorkUtils = CommonWorkUtils(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        sessionManagement = SessionManagement(requireContext())

        screenType = arguments?.getString("type", "") ?: ""
        screenType = arguments?.getString("type", "") ?: ""
        userAddress = arguments?.getString("address", "") ?: ""
        apartNum = arguments?.getString("apiApartmentNumber", "") ?: ""
        latitude = arguments?.getString("latitude", "") ?: ""
        longitude = arguments?.getString("longitude", "") ?: ""
        addressId = arguments?.getString("addressId", "") ?: ""
        selectType = arguments?.getString("selectType", "Home") ?: "Home"

        initialize()

        return binding.root
    }

    private fun initialize() {

        binding.tvAddress.text =userAddress.toString()


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when (screenType) {
                        "EnterYourAddress" -> {
                            findNavController().navigate(R.id.enterYourAddressFragment)
                        }
                        "Basket" -> {
                            findNavController().navigate(R.id.basketScreenFragment)
                        }
                        else -> {
                            findNavController().navigateUp()
                        }
                    }

                }
            })

        if (selectType.equals("Home")){
            selectedButton(true)
        }else{
            selectedButton(false)
        }

        binding.llSetHome.setOnClickListener {
            selectedButton(true)
        }

        binding.llSetWork.setOnClickListener {
            selectedButton(false)
        }

        binding.imageCrossWeb.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvConfirmBtn.setOnClickListener {
            fullAddressDialog()
        }
        
        
        
    }


    private fun selectedButton(status:Boolean){
        if (status){
            setStatus = "Home"
            binding.llSetHome.setBackgroundResource(R.drawable.outline_green_border_bg)
            binding.llSetWork.setBackgroundResource(R.drawable.height_type_bg)

            binding.imageHome.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_orange), PorterDuff.Mode.SRC_IN)
            binding.imageWork.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_grays), PorterDuff.Mode.SRC_IN)
        }else{
            setStatus = "Work"
            binding.llSetHome.setBackgroundResource(R.drawable.height_type_bg)
            binding.llSetWork.setBackgroundResource(R.drawable.outline_address_green_border_bg)

            binding.imageHome.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_grays), PorterDuff.Mode.SRC_IN)
            binding.imageWork.setColorFilter(ContextCompat.getColor(requireContext(), R.color.light_orange), PorterDuff.Mode.SRC_IN)
        }
    }


    private fun fullAddressDialog() {
        val dialogMiles = Dialog(requireContext())
        dialogMiles.setContentView(R.layout.alert_dialog_address_popup)
        dialogMiles.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogMiles.setCancelable(false)
        dialogMiles.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        val relConfirm = dialogMiles.findViewById<RelativeLayout>(R.id.relConfirm)
        val imageCross = dialogMiles.findViewById<ImageView>(R.id.imageCross)
        edtStreetName = dialogMiles.findViewById(R.id.edtStreetName)
        edtStreetNumber = dialogMiles.findViewById(R.id.edtStreetNumber)
        edtApartNumber = dialogMiles.findViewById(R.id.edtApartNumber)
        edtCity = dialogMiles.findViewById(R.id.edtCity)
        edtStates = dialogMiles.findViewById(R.id.edtStates)
        edtPostalCode = dialogMiles.findViewById(R.id.edtPostalCode)
        edtAddress = dialogMiles.findViewById(R.id.edtAddress)
        dialogMiles.show()

        if (streetName != "") {
            edtStreetName.setText(streetName.toString())
        }

        if (streetNum != "") {
            edtStreetNumber.setText(streetNum.toString())
        }

        if (apartNum != "") {
            edtApartNumber.setText(apartNum.toString())
        }

        if (city != "") {
            edtCity.setText(city.toString())
        }

        if (states != "") {
            edtStates.setText(states.toString())
        }

        if (address != "") {
            edtAddress.setText(address.toString())
        }

        if (zipcode != "") {
            edtPostalCode.setText(zipcode.toString())
        }

        dialogMiles.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        relConfirm.setOnClickListener {
            if (BaseApplication.isOnline(requireContext())) {
                if (validate()) {
                    streetName = edtStreetName.text.toString().trim()
                    streetNum = edtStreetNumber.text.toString().trim()
                    apartNum = edtApartNumber.text.toString().trim()
                    city = edtCity.text.toString().trim()
                    states = edtStates.text.toString().trim()
                    address = edtAddress.text.toString().trim()
                    zipcode = edtPostalCode.text.toString().trim()
                    addFullAddressApi(dialogMiles)
                }
            } else {
                BaseApplication.alertError(requireContext(), ErrorMessage.networkError, false)
            }

        }

        imageCross.setOnClickListener {
            dialogMiles.dismiss()
        }
    }

    private fun validate(): Boolean {
        // Check if email/phone is empty
        if (edtStreetName.text.toString().trim().isEmpty()) {
            commonWorkUtils.alertDialog(requireActivity(), ErrorMessage.streetNameError, false)
            return false
        } else if (edtStreetNumber.text.toString().trim().isEmpty()) {
            commonWorkUtils.alertDialog(requireActivity(), ErrorMessage.streetNumberError, false)
            return false
        } else if (edtApartNumber.text.toString().trim().isEmpty()) {
            commonWorkUtils.alertDialog(requireActivity(), ErrorMessage.apartNumberError, false)
            return false
        } else if (edtCity.text.toString().trim().isEmpty()) {
            commonWorkUtils.alertDialog(requireActivity(), ErrorMessage.cityEnterError, false)
            return false
        } else if (edtStates.text.toString().trim().isEmpty()) {
            commonWorkUtils.alertDialog(requireActivity(), ErrorMessage.statesEnterError, false)
            return false
        } else if (edtAddress.text.toString().trim().isEmpty()) {
            commonWorkUtils.alertDialog(requireActivity(), ErrorMessage.addressError, false)
            return false
        } else if (edtPostalCode.text.toString().trim().isEmpty()) {
            commonWorkUtils.alertDialog(requireActivity(), ErrorMessage.postalCodeError, false)
            return false
        }
        return true
    }

    private fun addFullAddressApi(dialogMiles: Dialog) {
        BaseApplication.showMe(requireContext())
        lifecycleScope.launch {
            addressMapFullScreenViewModel.addAddressUrl(
                {
                    BaseApplication.dismissMe()
                    handleApiAddAddressResponse(it,dialogMiles)
                },
                latitude,
                longitude,
                streetName,
                streetNum,
                apartNum,
                city,
                states,
                country,
                zipcode,
                "1",
                addressId,
                setStatus
            )
        }
    }

    private fun handleApiAddAddressResponse(result: NetworkResult<String>, dialogMiles: Dialog) {
        when (result) {
            is NetworkResult.Success -> handleSuccessAddAddressResponse(result.data.toString(),dialogMiles)
            is NetworkResult.Error -> showAlert(result.message, false)
            else -> showAlert(result.message, false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleSuccessAddAddressResponse(data: String,dialogMiles: Dialog) {
        try {
            val apiModel = Gson().fromJson(data, AddAddressModel::class.java)
            Log.d("@@@ addMea List ", "message :- $data")
            if (apiModel.code == 200 && apiModel.success == true) {
                dialogMiles.dismiss()
                if (screenType.equals("EnterYourAddress")){
                    findNavController().navigate(R.id.notificationFragment)
                }else {
                    (activity as MainActivity?)?.upBasketCheckOut()
                    findNavController().navigateUp()
                }
            } else {
                if (apiModel.code == ErrorMessage.code) {
                    showAlert(apiModel.message, true)
                } else {
                    showAlert(apiModel.message, false)
                }
            }
        } catch (e: Exception) {
            showAlert(e.message, false)
        }
    }

    private fun showAlert(message: String?, status: Boolean) {
        BaseApplication.alertError(requireContext(), message, status)
    }


    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(requireActivity(), Locale.getDefault())
        try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                streetName = address.thoroughfare ?: "" // Street Name
                streetNum = address.subThoroughfare ?: "" // Street Number
//                apartNum = address.premises ?: "" // Apartment Number
                city = address.locality ?: "" // City
                states = address.adminArea ?: "" // State/Province
                country = address.countryName ?: "" // Country
                zipcode = address.postalCode ?: "" // Zip Code

                Log.d("Address", "Street Name: $streetName")
                Log.d("Address", "Street Number: $streetNum")
                Log.d("Address", "Apartment Number: $apartNum")
                Log.d("Address", "City: $city")
                Log.d("Address", "Country: $country")
                Log.d("Address", "ZIP Code: $zipcode")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val lat = latitude?.toDoubleOrNull() ?: 0.0  // Convert String to Double, default to 0.0 if null
        val lng = longitude?.toDoubleOrNull() ?: 0.0
        mMap = googleMap
        // 🔹 Clear all markers (if any exist)
        mMap.clear()

        // 🔴 Disable indoor maps to hide level picker
        mMap.isIndoorEnabled = false

        val initialPosition = LatLng(lat, lng) // Example: Sydney
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 20f))

        // 🔹 Change marker image when map is moving
        mMap.setOnCameraMoveStartedListener {
            Log.d("TESTING_MAP", "Map is moving...")
            userAddress=""
            // Change marker image while dragging
            binding.markerImage.setImageResource(R.drawable.pin)
        }

        mMap.setOnCameraIdleListener {
            val centerPosition = mMap.cameraPosition.target
            // ✅ Ensure marker is initialized before using it
            if (marker == null) {
//                marker = mMap.addMarker(
//                    MarkerOptions()
//                        .position(centerPosition)
//                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//                )
            } else {
                marker?.position = centerPosition // ✅ Move marker to the center
            }
            // Change marker image while dragging
            binding.markerImage.setImageResource(R.drawable.pin)
            Log.d("TESTING_MAP", "Dragging started at: ${marker?.position?.latitude}, ${marker?.position?.longitude}")
            // ✅ Ensure LatLng is not null before calling function
            val lat = centerPosition.latitude
            val lng = centerPosition.longitude
            latitude = centerPosition.latitude.toString()
            longitude = centerPosition.longitude.toString()
            if (userAddress.equals("")){
                binding.tvAddress.text = getAddressFromLatLng(lat, lng)
                address = getAddressFromLatLng(lat, lng)
                getAddressFromLocation(lat, lng)
            }else{
                getAddressFromLocation(lat, lng)
                val addressParts = listOf(
                    apartNum,
                    streetNum,
                    streetName,
                    city,
                    states,
                    country,
                    zipcode
                )
                val fullAddress = addressParts.filter { !it.isNullOrBlank() }.joinToString(" ")
                address=fullAddress
                userAddress=binding.tvAddress.text.toString()
            }

        }

    }

    private fun getAddressFromLatLng(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            geocoder.getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        } catch (e: Exception) {
            e.printStackTrace()
            null // Return null if an error occurs
        }
    }


    // Manage MapView Lifecycle
    override fun onResume() {
        super.onResume()

    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()

    }

    override fun onPause() {

        super.onPause()
    }

    override fun onDestroy() {

        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()

    }

}