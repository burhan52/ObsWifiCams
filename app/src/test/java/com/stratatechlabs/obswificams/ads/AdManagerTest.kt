package com.stratatechlabs.obswificams.ads

import com.stratatechlabs.obswificams.BuildConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AdManagerTest {

    @Before
    fun setUp() {
        AdManager.resetForTesting()
    }

    @After
    fun tearDown() {
        AdManager.resetForTesting()
    }

    @Test
    fun testAdmobUnitIdsConfigured() {
        // Verify AdMob App ID and ad unit IDs injected from .env via BuildConfig
        assertEquals("ca-app-pub-2382308752605431~8550584950", BuildConfig.ADMOB_APP_ID)
        assertEquals("ca-app-pub-2382308752605431/1985176601", BuildConfig.ADMOB_OPEN_APP_ID)
        assertEquals("ca-app-pub-2382308752605431/3114425441", BuildConfig.ADMOB_BACK_INTERSTITIAL_ID)
        assertEquals("ca-app-pub-2382308752605431/4419768250", BuildConfig.ADMOB_REWARD_TEA_ID)

        assertTrue(BuildConfig.ADMOB_APP_ID.startsWith("ca-app-pub-"))
        assertTrue(BuildConfig.ADMOB_OPEN_APP_ID.startsWith("ca-app-pub-"))
        assertTrue(BuildConfig.ADMOB_BACK_INTERSTITIAL_ID.startsWith("ca-app-pub-"))
        assertTrue(BuildConfig.ADMOB_REWARD_TEA_ID.startsWith("ca-app-pub-"))
    }

    @Test
    fun testAppOpenAdInitialStateNotAvailable() {
        assertFalse(AdManager.isAppOpenAdAvailable())
    }

    @Test
    fun testBackInterstitialAdInitialStateNotAvailable() {
        assertFalse(AdManager.isBackInterstitialAvailable())
    }

    @Test
    fun testRewardedTeaAdInitialStateNotAvailable() {
        assertFalse(AdManager.isRewardedTeaAvailable())
    }

    @Test
    fun testResetClearsAllAdStates() {
        AdManager.resetForTesting()
        assertFalse(AdManager.isAppOpenAdAvailable())
        assertFalse(AdManager.isBackInterstitialAvailable())
        assertFalse(AdManager.isRewardedTeaAvailable())
    }
}
