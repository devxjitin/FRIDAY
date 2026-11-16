package com.nandivex.friday.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRulesTest {
    private val sha = "a".repeat(64)

    private fun json(
        code: String = "30000001", name: String = "\"0.0.21\"", url: String = "\"https://friday.nandivex.in/FRIDAY.apk\"",
        hash: String = "\"$sha\"", extra: String = ""
    ) = """{"versionCode":$code,"versionName":$name,"apkUrl":$url,"sha256":$hash$extra}"""

    @Test fun aCompleteManifestIsParsed() {
        val m = UpdateRules.parse(json(extra = ""","notes":" Faster ","minAndroid":29,"rollout":40,"sizeBytes":1000"""))!!
        assertEquals(30000001L, m.versionCode)
        assertEquals("0.0.21", m.versionName)
        assertEquals("Faster", m.notes)
        assertEquals(29, m.minAndroid)
        assertEquals(40, m.rollout)
        assertEquals(sha, m.sha256)
    }

    @Test fun missingOptionalFieldsGetSafeDefaults() {
        val m = UpdateRules.parse(json())!!
        assertEquals(26, m.minAndroid)
        assertEquals(100, m.rollout)
        assertEquals("", m.notes)
    }

    @Test fun rolloutIsKeptBetweenZeroAndOneHundred() {
        assertEquals(100, UpdateRules.parse(json(extra = ""","rollout":900"""))!!.rollout)
        assertEquals(0, UpdateRules.parse(json(extra = ""","rollout":-5"""))!!.rollout)
    }

    @Test fun anIncompleteOrUnsafeManifestIsRefused() {
        assertNull(UpdateRules.parse("not json"))
        assertNull(UpdateRules.parse("{}"))
        assertNull(UpdateRules.parse(json(code = "0")))
        assertNull(UpdateRules.parse(json(name = "\"\"")))
        assertNull(UpdateRules.parse(json(hash = "\"abc\"")))
        assertNull(UpdateRules.parse(json(hash = "\"${"g".repeat(64)}\"")))
        assertNull(UpdateRules.parse(json(extra = ""","sizeBytes":${UpdateRules.MAX_APK_BYTES + 1}""")))
    }

    @Test fun theChecksumIsKeptInLowerCase() {
        assertEquals("ab".repeat(32), UpdateRules.parse(json(hash = "\"${"AB".repeat(32)}\""))!!.sha256)
    }

    @Test fun onlyOurOwnHostsOverHttpsAreAllowed() {
        assertTrue(UpdateRules.isAllowedUrl("https://friday.nandivex.in/FRIDAY.apk"))
        assertTrue(UpdateRules.isAllowedUrl("https://friday-nandivex.web.app/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedUrl("http://friday.nandivex.in/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedUrl("https://evil.example/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedUrl("https://friday.nandivex.in.evil.example/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedUrl("https://friday.nandivex.in@evil.example/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedUrl("https://friday.nandivex.in:8443/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedUrl("file:///sdcard/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedUrl("not a url"))
    }

    @Test fun aManifestPointingAtAnotherHostIsRefused() {
        assertNull(UpdateRules.parse(json(url = "\"https://evil.example/FRIDAY.apk\"")))
        assertNull(UpdateRules.parse(json(url = "\"https://github.com/someone-else/FRIDAY/releases/download/v1/FRIDAY.apk\"")))
    }

    @Test fun theApkMayBeOurOwnGitHubReleaseAsset() {
        val release = "https://github.com/devxjitin/FRIDAY/releases/download/v0.0.22/FRIDAY.apk"
        assertTrue(UpdateRules.isAllowedApkUrl(release))
        assertTrue(UpdateRules.isAllowedApkUrl("https://friday.nandivex.in/FRIDAY.apk"))
        assertEquals(release, UpdateRules.parse(json(url = "\"$release\""))!!.apkUrl)
        assertFalse(UpdateRules.isAllowedApkUrl("https://github.com/devxjitin/FRIDAY/raw/main/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedApkUrl("https://github.com/devxjitin/other/releases/download/v1/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedApkUrl("https://github.com.evil.example/devxjitin/FRIDAY/releases/download/v1/a.apk"))
        assertFalse(UpdateRules.isAllowedApkUrl("http://github.com/devxjitin/FRIDAY/releases/download/v0.0.22/FRIDAY.apk"))
        assertFalse(UpdateRules.isAllowedApkUrl("https://github.com@evil.example/devxjitin/FRIDAY/releases/download/v1/a.apk"))
    }

    @Test fun aGitHubDownloadMayEndInItsAssetStoreButManifestsMayNotComeFromThere() {
        assertTrue(UpdateRules.isAllowedDownloadUrl("https://release-assets.githubusercontent.com/github-production-release-asset/1/a?x=1"))
        assertTrue(UpdateRules.isAllowedDownloadUrl("https://objects.githubusercontent.com/github-production-release-asset/1/a"))
        assertFalse(UpdateRules.isAllowedDownloadUrl("https://evil.example/a.apk"))
        assertFalse(UpdateRules.isAllowedDownloadUrl("http://release-assets.githubusercontent.com/a"))
        assertFalse(UpdateRules.isAllowedUrl("https://github.com/devxjitin/FRIDAY/releases/download/v1/latest.json"))
        assertNull(UpdateRules.parse(json(url = "\"https://release-assets.githubusercontent.com/a.apk\"")))
    }

    @Test fun aPhoneIsOfferedOnlyANewerBuild() {
        val m = UpdateRules.parse(json(code = "100"))!!
        assertTrue(UpdateRules.shouldOffer(m, currentVersionCode = 99, sdkInt = 34, deviceKey = "x"))
        assertFalse(UpdateRules.shouldOffer(m, currentVersionCode = 100, sdkInt = 34, deviceKey = "x"))
        assertFalse(UpdateRules.shouldOffer(m, currentVersionCode = 101, sdkInt = 34, deviceKey = "x"))
    }

    @Test fun aPhoneBelowTheMinimumAndroidIsNotOffered() {
        val m = UpdateRules.parse(json(code = "100", extra = ""","minAndroid":30"""))!!
        assertFalse(UpdateRules.shouldOffer(m, 1, sdkInt = 29, deviceKey = "x"))
        assertTrue(UpdateRules.shouldOffer(m, 1, sdkInt = 30, deviceKey = "x"))
    }

    @Test fun rolloutZeroHoldsTheReleaseBackAndOneHundredOffersItToAll() {
        val keys = (1..200).map { "phone-$it" }
        assertTrue(keys.none { UpdateRules.inRollout(it, 0) })
        assertTrue(keys.all { UpdateRules.inRollout(it, 100) })
    }

    @Test fun aPartialRolloutReachesRoughlyThatShareAndIsStablePerPhone() {
        val keys = (1..1000).map { "phone-$it" }
        val share = keys.count { UpdateRules.inRollout(it, 30) }
        assertTrue("share was $share", share in 200..400)
        keys.take(50).forEach { assertEquals(UpdateRules.bucket(it), UpdateRules.bucket(it)) }
        assertTrue(keys.all { UpdateRules.bucket(it) in 0..99 })
    }

    @Test fun hexIsLowerCaseAndTwoDigitsPerByte() {
        assertEquals("000fff", UpdateRules.hex(byteArrayOf(0, 15, -1)))
        assertNotNull(UpdateRules.hex(ByteArray(0)))
    }
}
