package com.mykaimeal.planner.fragment.mainfragment.commonscreen

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.gson.Gson
import com.mykaimeal.planner.R
import com.mykaimeal.planner.adapter.AdapterInviteItem
import com.mykaimeal.planner.basedata.BaseApplication
import com.mykaimeal.planner.basedata.NetworkResult
import com.mykaimeal.planner.basedata.SessionManagement
import com.mykaimeal.planner.databinding.FragmentInvitationsScreenBinding
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.statistics.model.ReferralInvitationModel
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.statistics.model.ReferralInvitationModelData
import com.mykaimeal.planner.fragment.mainfragment.commonscreen.statistics.viewmodel.StatisticsViewModel
import com.mykaimeal.planner.messageclass.ErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class InvitationsScreenFragment : Fragment() {

    private lateinit var binding: FragmentInvitationsScreenBinding
    private var adapterInviteItem: AdapterInviteItem? = null
    private lateinit var statisticsViewModel: StatisticsViewModel
    private var referralList: MutableList<ReferralInvitationModelData> =mutableListOf()
    private var referLink: String = ""

    private lateinit var sessionManagement: SessionManagement


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding=FragmentInvitationsScreenBinding.inflate(layoutInflater, container, false)

        statisticsViewModel = ViewModelProvider(this)[StatisticsViewModel::class.java]
        sessionManagement = SessionManagement(requireActivity())

        backButton()

        adapterInviteItem = AdapterInviteItem(referralList, requireActivity())
        binding.rcyFriendsInvite.adapter = adapterInviteItem

        initialize()

        return binding.root
    }

    private fun backButton(){
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigateUp()
            }
        })
    }

    private fun initialize() {

        binding.imgBackInvite.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.spinnerFilterType.setIsFocusable(true)

        binding.spinnerFilterType.setItems(
            listOf("All", "Trial", "Trial over", "Myka", "Redeemed")
        )

        binding.spinnerFilterType.setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newItem ->
            filterData(newItem) // Call your filter function with the selected value
        }

        if (BaseApplication.isOnline(requireContext())) {
            getInvitationList()
        } else {
            BaseApplication.alertError(requireContext(), ErrorMessage.networkError, false)
        }


        binding.textInviteFriends.setOnClickListener {

            shareImageWithText(
                "Hey, My kai an all-in-one app that’s completely changed the way I shop. It saves me time, money," +
                        " and even helps with meal planning without having to step into a supermarket.\" +\n" +
                        " See for yourself with a free gift from me. \nClick on link below:\n\n",
                referLink
            )
        }

        deepLink()

    }

    private fun shareImageWithText(description: String, link: String) {
        // Download image using Glide
        Glide.with(requireContext())
            .asBitmap() // Request a Bitmap image
            .load(R.mipmap.app_icon) // Provide the URL to load the image from
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    try {
                        // Save the image to a file in the app's external storage
                        val file = File(
                            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            "shared_image.png"
                        )
                        val fos = FileOutputStream(file)
                        resource.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.close()

                        // Create URI for the file using FileProvider
                        val uri: Uri = FileProvider.getUriForFile(
                            requireContext(),
                            requireActivity().packageName + ".provider", // Make sure this matches your manifest provider
                            file
                        )

                        // Format the message with line breaks
                        val formattedText = """$description$link""".trimIndent()

                        // Create an intent to share the image and text
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_TEXT, formattedText)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        // Launch the share dialog
                        requireContext().startActivity(
                            Intent.createChooser(
                                shareIntent,
                                "Share Image"
                            )
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("ImageShareError", "onResourceReady: ${e.message}")
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Optional: Handle if the image load is cleared or cancelled
                }
            })
    }

    private fun deepLink(){
        val afUserId = sessionManagement.getId()?.toString().orEmpty()
        val referrerCode = sessionManagement.getReferralCode()?.toString().orEmpty()
        val providerName = sessionManagement.getUserName()?.toString().orEmpty()
        val providerImage = sessionManagement.getImage()?.toString().orEmpty()

        // Base URL for the OneLink template
        val baseURL = "https://mykaimealplanner.onelink.me/mPqu/" // Replace with your OneLink template

        // Deep link URL for when the app is installed
        val deepLink = "mykai://property?" +
                "af_user_id=$afUserId" +
                "&Referrer=$referrerCode" +
                "&providerName=$providerName" +
                "&providerImage=$providerImage"

        // Web fallback URL (e.g., if app is not installed)
        val webLink = "https://www.mykaimealplanner.com" // Replace with your fallback web URL

        // Build OneLink parameters
        val parameters = mapOf(
            "af_dp" to deepLink, // App deep link
            "af_web_dp" to webLink // Web fallback URL
        )


        // Use Uri.Builder to construct the URL with query parameters
        val uriBuilder = Uri.parse(baseURL).buildUpon()
        for ((key, value) in parameters) {
            uriBuilder.appendQueryParameter(key, value)
        }
        // Convert the URI to string and call the completion handler
        val fullURL = uriBuilder.toString()
        referLink = fullURL
        Log.d("link ", "Generated OneLink URL: $fullURL")

    }

    // Filter data using only `data` list
    private fun filterData(filter: String) {
        val filtered = if (filter.equals("All", ignoreCase = true)) {
            referralList // just show current list
        } else {
            referralList.filter { it.status?.trim().equals(filter, ignoreCase = true) }.toMutableList()
        }
        adapterInviteItem?.updateList(filtered)
    }

    private fun getInvitationList() {
        BaseApplication.showMe(requireContext())
        lifecycleScope.launch {
            statisticsViewModel.referralUrl {
                BaseApplication.dismissMe()
                handleApiReferralResponse(it)
            }
        }
    }


    private fun handleApiReferralResponse(result: NetworkResult<String>) {
        when (result) {
            is NetworkResult.Success -> handleSuccessReferralResponse(result.data.toString())
            is NetworkResult.Error -> showAlert(result.message, false)
            else -> showAlert(result.message, false)
        }
    }

    private fun showAlert(message: String?, status: Boolean) {
        BaseApplication.alertError(requireContext(), message, status)
    }

    @SuppressLint("SetTextI18n")
    private fun handleSuccessReferralResponse(data: String) {
        try {
            val apiModel = Gson().fromJson(data, ReferralInvitationModel::class.java)
            Log.d("@@@ addMea List ", "message :- $data")
            if (apiModel.code == 200 && apiModel.success == true) {
                apiModel.data?.let {
                    showInvitationList(it)
                }?:run {
                    invitedValue()
                }
            } else {
                invitedValue()
                if (apiModel.code == ErrorMessage.code) {
                    showAlert(apiModel.message, true)
                } else {
                    showAlert(apiModel.message, false)
                }

            }
        } catch (e: Exception) {
            invitedValue()
            showAlert(e.message, false)
        }
    }

    private fun invitedValue(){
        val htmlText = "You have invited 0 friends to use<b> My Kai</b>"
        val formattedText = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.tvFriendsCountNumber.text = formattedText
    }

    private fun showInvitationList(data: MutableList<ReferralInvitationModelData>) {
        try {
            referralList.clear()
            data.let {
                referralList.addAll(it)
                if (referralList.size > 0) {
                    val invitedCount = referralList.size.toString()
                    val htmlText = "You have invited $invitedCount friends to use<b> My Kai</b>"
                    val formattedText = HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    binding.tvFriendsCountNumber.text = formattedText
                    binding.rcyFriendsInvite.visibility = View.VISIBLE
                    adapterInviteItem?.updateList(referralList)
                } else {
                    invitedValue()
                    binding.rcyFriendsInvite.visibility = View.GONE
                }
            }
        }catch (e:Exception){
            invitedValue()
           Log.d("@Error ","*********"+e.message)
        }
    }

}