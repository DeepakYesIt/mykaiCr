package com.mykaimeal.planner.fragment.authfragment.resetpassword.model

data class ResetPasswordModel(
    val code: Int,
    val `data`: ResetPasswordModelData,
    val message: String,
    val success: Boolean
)

data class ResetPasswordModelData(
    val bio: String,
    val bodygoal: Any,
    val calories: Int,
    val carbs: Int,
    val conversions: Any,
    val cooking_for_type: Any,
    val cooking_frequency: Any,
    val country: Any,
    val created_at: String,
    val deleted_at: Any,
    val device_type: Any,
    val eating_out: Any,
    val email: String,
    val email_verified_at: Any,
    val fat: Int,
    val fcm_token: Any,
    val gender: String,
    val id: Int,
    val isNewuser: Int,
    val location_on_off: Int,
    val name: String,
    val notification_status: Int,
    val otp: Int,
    val otp_verify: Int,
    val phone_number: Any,
    val profile_img: String,
    val protien: Int,
    val referral_code: Any,
    val referrals: Any,
    val social_id: Any,
    val social_type: Any,
    val status: Int,
    val take_way: Any,
    val updated_at: String,
    val user_type: Int
)