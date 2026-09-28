package com.yeobaek.data.local

import com.russhwolf.settings.Settings

class GuideOnboardingPreferences(
    private val settings: Settings,
) {
    fun saveGuidePage(page: Int) {
        settings.putInt(GUIDE_PAGE, page)
    }

    fun saveGuideState(state: Boolean) {
        settings.putBoolean(GUIDE_STATE, state)
    }

    fun saveOnboardingState(state: Boolean) {
        settings.putBoolean(ONBOARDING_STATE, state)
    }

    fun getGuidePage(): Int? = settings.getIntOrNull(GUIDE_PAGE)

    fun getGuideState(): Boolean = settings.getBooleanOrNull(GUIDE_STATE) ?: false

    fun getOnboardingState(): Boolean = settings.getBooleanOrNull(ONBOARDING_STATE) ?: false

    companion object {
        const val GUIDE_STATE = "guideState"
        const val ONBOARDING_STATE = "onboardingState"
        const val GUIDE_PAGE = "guidePage"
    }
}
