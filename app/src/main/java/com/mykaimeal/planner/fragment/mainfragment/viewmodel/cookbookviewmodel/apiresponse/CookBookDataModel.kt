package com.mykaimeal.planner.fragment.mainfragment.viewmodel.cookbookviewmodel.apiresponse

data class CookBookDataModel(
    val `data`: CookBookDataRecipeModel?,
    val id: Int,
    val shared: Int?,
    val uri: String?
)