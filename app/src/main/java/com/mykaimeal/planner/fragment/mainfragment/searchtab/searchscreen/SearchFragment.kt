package com.mykaimeal.planner.fragment.mainfragment.searchtab.searchscreen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mykaimeal.planner.OnItemClickListener
import com.mykaimeal.planner.R
import com.mykaimeal.planner.activity.MainActivity
import com.mykaimeal.planner.adapter.SearchMealAdapter
import com.mykaimeal.planner.adapter.SearchMealCatAdapter
import com.mykaimeal.planner.adapter.SearchMealRecipeAdapter
import com.mykaimeal.planner.adapter.SearchRecipeAdapter
import com.mykaimeal.planner.apiInterface.BaseUrl
import com.mykaimeal.planner.basedata.BaseApplication
import com.mykaimeal.planner.basedata.NetworkResult
import com.mykaimeal.planner.basedata.SessionManagement
import com.mykaimeal.planner.commonworkutils.CommonWorkUtils
import com.mykaimeal.planner.databinding.FragmentSearchBinding
import com.mykaimeal.planner.fragment.commonfragmentscreen.commonModel.UpdatePreferenceSuccessfully
import com.mykaimeal.planner.fragment.mainfragment.searchtab.searchscreen.apiresponse.Category
import com.mykaimeal.planner.fragment.mainfragment.searchtab.searchscreen.apiresponse.Data
import com.mykaimeal.planner.fragment.mainfragment.searchtab.searchscreen.apiresponse.Ingredient
import com.mykaimeal.planner.fragment.mainfragment.searchtab.searchscreen.apiresponse.MealType
import com.mykaimeal.planner.fragment.mainfragment.searchtab.searchscreen.apiresponse.Recipe
import com.mykaimeal.planner.fragment.mainfragment.searchtab.searchscreen.apiresponse.SearchApiResponse
import com.mykaimeal.planner.fragment.mainfragment.searchtab.searchscreen.viewmodel.SearchRecipeViewModel
import com.mykaimeal.planner.fragment.mainfragment.viewmodel.recipedetails.apiresponse.RecipeModel
import com.mykaimeal.planner.listener.RecipeDetailListener
import com.mykaimeal.planner.messageclass.ErrorMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class SearchFragment : Fragment(),View.OnClickListener, OnItemClickListener, RecipeDetailListener {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private var searchRecipeAdapter: SearchRecipeAdapter? = null
    private var searchMealAdapter: SearchMealAdapter? = null
    private var searchMealCatAdapter: SearchMealCatAdapter? = null
    private var searchMealRecipeAdapter: SearchMealRecipeAdapter? = null
    private lateinit var commonWorkUtils: CommonWorkUtils
    private lateinit var sessionManagement: SessionManagement
    private lateinit var searchRecipeViewModel:SearchRecipeViewModel
    private var clickedUrl: String = ""
    private var ingredient: MutableList<Ingredient> = mutableListOf()
    private var mealType: MutableList<MealType> = mutableListOf()
    private var category: MutableList<Category> = mutableListOf()
    private var recipe: MutableList<Recipe> = mutableListOf()
    private var cookbookList: MutableList<com.mykaimeal.planner.fragment.mainfragment.viewmodel.planviewmodel.apiresponsecookbooklist.Data> = mutableListOf()
    private lateinit var textListener: TextWatcher
    private var textChangedJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        (activity as MainActivity?)?.changeBottom("search")
        (activity as MainActivity?)?.alertStatus=false

        (activity as? MainActivity)?.binding?.let {
            it.llIndicator.visibility = View.VISIBLE
            it.llBottomNavigation.visibility = View.VISIBLE
        }

        binding.cardViewAddRecipe.visibility=View.GONE

        clickedUrl = arguments?.getString("ClickedUrl", "").toString()

        searchRecipeViewModel = ViewModelProvider(requireActivity())[SearchRecipeViewModel::class.java]

        cookbookList.clear()

        val data= com.mykaimeal.planner.fragment.mainfragment.viewmodel.planviewmodel.apiresponsecookbooklist.Data("","",0,"","Favourites",0,"",0)
        cookbookList.add(0,data)

        commonWorkUtils = CommonWorkUtils(requireActivity())
        sessionManagement = SessionManagement(requireContext())


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.homeFragment)
                }
            })

        initialize()


        searchRecipeViewModel.search?.let {
            binding.etIngRecipeSearchBar.setText(it)
        }

        if (searchRecipeViewModel.data!=null){
            showDataInUi(searchRecipeViewModel.data)
        }else{
            // This Api call when the screen in loaded
            searchApi("")
        }


        binding.scrollview.setOnRefreshListener {
            // This Api call when the screen in loaded
            binding.etIngRecipeSearchBar.text.clear()
            searchApi("")
        }
        return binding.root
    }


    private fun searchRecipeApi(searchText: String) {
        BaseApplication.showMe(requireContext())
        lifecycleScope.launch {
            // Create a JsonObject for the main JSON structure
            val jsonObject = JsonObject()
            jsonObject.addProperty("search",searchText)
            // Log the final JSON data
            Log.d("final data", "******$jsonObject")
            searchRecipeViewModel.recipeSearchApi({
                binding.scrollview.isRefreshing=false
                BaseApplication.dismissMe()
                when (it) {
                    is NetworkResult.Success -> {
                        try {
                            val apiModel = Gson().fromJson(it.data, SearchApiResponse::class.java)
                            Log.d("@@@ Recipe Details ", "message :- $it.data")
                            if (apiModel.code == 200 && apiModel.success) {
                                apiModel.data.let {
                                    it1 -> showDataInUi(it1)
                                }
                            } else {
                                if (apiModel.code == ErrorMessage.code) {
                                    showAlert(apiModel.message, true)
                                } else {
                                    if (!apiModel.message.equals("Search query cannot be empty.")) {
                                        showAlert(apiModel.message, false)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.d("AddMeal", "message:--" + e.message)
                        }
                    }
                    is NetworkResult.Error -> {
                        showAlert(it.message, false)
                    }
                    else -> {
                        showAlert(it.message, false)
                    }
                }
            }, jsonObject)
        }
    }

    private fun showDataInUi(it1: Data?) {

        try {

            searchRecipeViewModel.setData(it1,binding.etIngRecipeSearchBar.text.toString())

            ingredient.clear()
            mealType.clear()
            category.clear()
            recipe.clear()


            it1?.ingredient?.let {
                ingredient.addAll(it)
            }
            it1?.mealType?.let {
                mealType.addAll(it)
            }
            it1?.category?.let {
                category.addAll(it)
            }

            it1?.recipes?.let {
                recipe.addAll(it)
            }

            if (ingredient.size>0){
                searchRecipeAdapter = SearchRecipeAdapter(ingredient, requireActivity(),this)
                binding.rcySearchRecipe.adapter = searchRecipeAdapter
                binding.llSearchRecipientIng.visibility=View.VISIBLE
            }else{
                binding.llSearchRecipientIng.visibility=View.GONE
            }

            if (mealType.size>0){
                searchMealAdapter = SearchMealAdapter(mealType, requireActivity(),this)
                binding.rcySearchMeal.adapter = searchMealAdapter
                binding.llSearchByMeal.visibility=View.VISIBLE
            }else{
                binding.llSearchByMeal.visibility=View.GONE
            }

            if (category.size>0){
                searchMealCatAdapter = SearchMealCatAdapter(category, requireActivity(),this)
                binding.rcyPopularCat.adapter = searchMealCatAdapter
                binding.llPopularCat.visibility=View.VISIBLE
            }else{
                binding.llPopularCat.visibility=View.GONE
            }

            if (recipe.size>0){
                searchMealRecipeAdapter=SearchMealRecipeAdapter(recipe,requireActivity(),this)
                binding.rcyrecipe.adapter=searchMealRecipeAdapter
                binding.llrecipe.visibility = View.VISIBLE
            }else{
                binding.llrecipe.visibility = View.GONE
            }

            if (it1?.preference_status!=null){
                if (it1.preference_status == 0){
                    binding.imgPreferences.setImageResource(R.drawable.toggle_off_icon)
                }else{
                    binding.imgPreferences.setImageResource(R.drawable.toggle_on_icon)
                }
            }else{
                binding.imgPreferences.setImageResource(R.drawable.toggle_off_icon)
            }

        }catch (e:Exception){
            Log.d("Error","*****"+e.message)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI(view: View) {
        // Set up touch listener for non-EditText views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard(view)
                false
            }
        }

        // If a layout container, iterate over children
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }


    }

    private fun hideKeyboard(view: View) {
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun lunchApi() {
        if (BaseApplication.isOnline(requireActivity())) {
            BaseApplication.showMe(requireContext())
            lifecycleScope.launch {
                searchRecipeViewModel.recipeforSearchApi {
                    BaseApplication.dismissMe()
                    binding.scrollview.isRefreshing=false
                    handleApiSearchResponse(it)
                }
            }
        }else{
            binding.scrollview.isRefreshing = false
            BaseApplication.alertError(requireContext(), ErrorMessage.networkError, false)
        }
    }

    private fun handleApiSearchResponse(result: NetworkResult<String>) {
        when (result) {
            is NetworkResult.Success -> handleSuccessSearchPreferences(result.data.toString())
            is NetworkResult.Error -> showAlert(result.message, false)
            else -> showAlert(result.message, false)
        }
    }

    private fun showAlert(message: String?, status: Boolean) {
        BaseApplication.alertError(requireContext(), message, status)
    }

    @SuppressLint("SetTextI18n")
    private fun handleSuccessSearchPreferences(data: String) {
        try {
            val apiModel = Gson().fromJson(data, SearchApiResponse::class.java)
            Log.d("@@@ Recipe Details ", "message :- $data")
            if (apiModel.code == 200 && apiModel.success) {
                showData(apiModel.data)
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

    private fun showData(data: Data?) {
        try {
//            searchRecipeViewModel.setData(data)

            ingredient.clear()
            mealType.clear()
            category.clear()
            data?.ingredient?.let {
                ingredient.addAll(it)
            }
            data?.mealType?.let {
                mealType.addAll(it)
            }
            data?.category?.let {
                category.addAll(it)
            }

            if (ingredient.size>0){
                searchRecipeAdapter = SearchRecipeAdapter(ingredient, requireActivity(),this)
                binding.rcySearchRecipe.adapter = searchRecipeAdapter
                binding.llSearchRecipientIng.visibility=View.VISIBLE
            }else{
                binding.llSearchRecipientIng.visibility=View.GONE
            }

            if (mealType.size>0){
                searchMealAdapter = SearchMealAdapter(mealType, requireActivity(),this)
                binding.rcySearchMeal.adapter = searchMealAdapter
                binding.llSearchByMeal.visibility=View.VISIBLE
            }else{
                binding.llSearchByMeal.visibility=View.GONE
            }

            if (category.size>0){
                searchMealCatAdapter = SearchMealCatAdapter(category, requireActivity(),this)
                binding.rcyPopularCat.adapter = searchMealCatAdapter
                binding.llPopularCat.visibility=View.VISIBLE
            }else{
                binding.llPopularCat.visibility=View.GONE
            }

            if (data?.preference_status!=null){
                if (data.preference_status == 0){
                    binding.imgPreferences.setImageResource(R.drawable.toggle_off_icon)
                }else{
                    binding.imgPreferences.setImageResource(R.drawable.toggle_on_icon)
                }
            }else{
                binding.imgPreferences.setImageResource(R.drawable.toggle_off_icon)
            }

        }catch (e:Exception){
            showAlert(e.message, false)
        }
    }

    private fun initialize() {

        if (sessionManagement.getUserName() != null) {
            val name = BaseApplication.getColoredSpanned(
                "Hello",
                "#06C169"
            ) + BaseApplication.getColoredSpanned(", " + sessionManagement.getUserName(), "#000000")
            binding.tvUserName.text = Html.fromHtml(name)
        }

        if (sessionManagement.getImage()!=null){
            Glide.with(requireContext())
                .load(BaseUrl.imageBaseUrl+sessionManagement.getImage())
                .placeholder(R.drawable.mask_group_icon)
                .error(R.drawable.mask_group_icon)
                .into(binding.imageProfile)
        }

        binding.relViewAll.setOnClickListener(this)
        binding.imgHeartRed.setOnClickListener(this)
        binding.imageProfile.setOnClickListener(this)
        binding.imgBasketIcon.setOnClickListener(this)
        binding.imgFilterIcon.setOnClickListener(this)
        binding.imgPreferences.setOnClickListener(this)

//        binding.etIngRecipeSearchBar.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(editable: Editable) {
//                val query = editable.toString().trim()
//                if (query.isNotEmpty()) {
//                    if (binding.rcySearchRecipe.visibility == View.VISIBLE) {
//                        filterIngredients(query)
//                    }
//                    if (binding.rcySearchMeal.visibility == View.VISIBLE) {
//                        filterMealType(query)
//                    }
//                    if (binding.rcyPopularCat.visibility == View.VISIBLE) {
//                        filterPopular(query)
//                    }
//                } else {
//                    // If the query is empty, reset the lists to show all original data
//                    resetLists()
//                }
//            }
//        })


        textListener = object : TextWatcher {
            private var searchFor = "" // Or view.editText.text.toString()
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString()
                if (searchText != searchFor) {
                    searchFor = searchText
                    textChangedJob?.cancel()
                    // Launch a new coroutine in the lifecycle scope
                    textChangedJob = lifecycleScope.launch {
                        delay(1000)  // Debounce time
                        if (searchText.equals(searchFor,true)) {
                            searchApi(searchText)
                        }
                    }
                }else{
                    searchApi("")
                }
            }
        }
    }

    private fun searchApi(value:String){
        if (BaseApplication.isOnline(requireActivity())) {
            searchRecipeApi(value)
        } else {
            binding.scrollview.isRefreshing=false
            BaseApplication.alertError(
                requireContext(),
                ErrorMessage.networkError,
                false
            )
        }
    }

    private fun resetLists() {
        if (ingredient.size>0) {
            ingredient.let { searchRecipeAdapter?.submitList(it) } // Reset recipe list
            binding.llSearchRecipientIng.visibility = View.VISIBLE
        } else {
            binding.llSearchRecipientIng.visibility = View.GONE
        }

        if (mealType.size>0) {
            searchMealAdapter?.submitList(mealType) // Reset meal type list
            binding.llSearchByMeal.visibility = View.VISIBLE
        } else {
            binding.llSearchByMeal.visibility = View.GONE
        }

        if (category.size>0) {
            searchMealCatAdapter?.submitList(category) // Reset popular category list
            binding.llPopularCat.visibility = View.VISIBLE
        } else {
            binding.llPopularCat.visibility = View.GONE
        }
    }

    private fun filterPopular(editText: String) {
        val filteredList: MutableList<Category> =
            java.util.ArrayList<Category>()
        for (item in category) {
            if (item.name!!.lowercase().contains(editText.lowercase(Locale.getDefault()))) {
                filteredList.add(item)
            }
        }
        if (filteredList.size > 0) {
            searchMealCatAdapter!!.filterList(filteredList)
            binding.llPopularCat.visibility = View.VISIBLE
        } else {
            binding.llPopularCat.visibility = View.GONE
        }
    }

    private fun filterMealType(editText: String) {
        val filteredList: MutableList<MealType> =
            java.util.ArrayList<MealType>()
        for (item in mealType) {
            if (item.name!!.lowercase().contains(editText.lowercase(Locale.getDefault()))) {
                filteredList.add(item)
            }
        }
        if (filteredList.size > 0) {
            searchMealAdapter!!.filterList(filteredList)
            binding.llSearchByMeal.visibility = View.VISIBLE
        } else {
            binding.llSearchByMeal.visibility = View.GONE
        }
    }

    private fun filterIngredients(editText: String) {
        val filteredList: MutableList<Ingredient> =
            java.util.ArrayList<Ingredient>()
        for (item in ingredient) {
            if (item.name!!.lowercase().contains(editText.lowercase(Locale.getDefault()))) {
                filteredList.add(item)
            }
        }
        if (filteredList.size > 0) {
            searchRecipeAdapter!!.filterList(filteredList)
            binding.llSearchRecipientIng.visibility = View.VISIBLE
        } else {
            binding.llSearchRecipientIng.visibility = View.GONE
        }
    }

    override fun onClick(item: View?) {
        when(item!!.id){
            R.id.relViewAll->{
                findNavController().navigate(R.id.allIngredientsFragment)
            }

            R.id.imageProfile->{
                findNavController().navigate(R.id.settingProfileFragment)
            }

            R.id.imgHeartRed->{
                (activity as MainActivity?)?.upDateCookBook()
                findNavController().navigate(R.id.cookBookFragment)
            }

            R.id.imgBasketIcon->{
                (activity as MainActivity?)?.upBasket()
                findNavController().navigate(R.id.basketScreenFragment)
            }

            R.id.imgFilterIcon->{
                findNavController().navigate(R.id.filterSearchFragment)
            }
            R.id.imgPreferences->{
                if (BaseApplication.isOnline(requireActivity())){
                    upDatePreferences()
                }else{
                    BaseApplication.alertError(requireContext(), ErrorMessage.networkError, false)
                }
            }
        }
    }

    private fun upDatePreferences() {
        BaseApplication.showMe(requireContext())
        lifecycleScope.launch {
            searchRecipeViewModel.recipePreferencesApi{
                BaseApplication.dismissMe()
                handleApiResponse(it)
            }
        }
    }

    private fun handleApiResponse(result: NetworkResult<String>) {
        when (result) {
            is NetworkResult.Success -> handleSuccessPreferences(result.data.toString())
            is NetworkResult.Error -> showAlert(result.message, false)
            else -> showAlert(result.message, false)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleSuccessPreferences(data: String) {
        try {
            val updateModel = Gson().fromJson(data, UpdatePreferenceSuccessfully::class.java)
            if (updateModel.code == 200 && updateModel.success) {
                (activity as MainActivity?)?.upDatePlan()
                searchApi("")
            } else {
                handleError(updateModel.code,updateModel.message)
            }
        }catch (e:Exception){
            Log.d("bodyGoal@@@","message"+e.message)
        }
    }

    override fun itemClick(position: Int?, status: String?, type: String?) {

        (activity as MainActivity?)?.upDateSearchItemClick()
        if (type.equals("MealCat",true) || type.equals("Meal",true) ){
            val bundle = Bundle().apply {
                putString("recipeName",status)
                putString("screenType","Search")
                putString("type",type)
            }

            findNavController().navigate(R.id.searchedRecipeBreakfastFragment,bundle)
        }
        if (type.equals("ingredient",true)){
            val bundle = Bundle().apply {
                putString("recipeName",status)
                putString("screenType","Ingredients")
            }
            findNavController().navigate(R.id.searchedRecipeBreakfastFragment,bundle)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.etIngRecipeSearchBar.addTextChangedListener(textListener)
    }

    override fun onPause() {
        binding.etIngRecipeSearchBar.removeTextChangedListener(textListener)
        super.onPause()
    }

    override fun itemRecipeSelect(uri: String?, mealType: String?, type: String?) {
        val bundle = Bundle().apply {
            putString("uri", uri)
            putString("mealType", mealType)
        }
        findNavController().navigate(R.id.recipeDetailsFragment, bundle)
    }


}