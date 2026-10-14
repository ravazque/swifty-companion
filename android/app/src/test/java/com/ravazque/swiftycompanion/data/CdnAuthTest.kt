package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.FakeIntra
import com.ravazque.swiftycompanion.data.auth.CdnAuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

// Profile photos need the app token on the CDN; nothing else may receive it.
class CdnAuthTest {
    private val intra = FakeIntra()

    @After
    fun tearDown() = intra.close()

    private fun get(client: OkHttpClient, path: String) {
        client.newCall(Request.Builder().url(intra.server.url(path)).build()).execute().close()
    }

    @Test
    fun onlyProfilePhotosOfTheCdnGetTheToken() {
        intra.repository()
        val cdn = OkHttpClient.Builder().addInterceptor(CdnAuthInterceptor(intra.tokens, intra.server.hostName)).build()
        val elsewhere = OkHttpClient.Builder().addInterceptor(CdnAuthInterceptor(intra.tokens, "cdn.example.com")).build()

        get(cdn, "/users/0a1b/medium_jdoe.gif")
        get(cdn, "/coalition/image/401/logo.svg")
        get(elsewhere, "/users/0a1b/medium_jdoe.gif")

        assertEquals(listOf("Bearer t1", null, null), intra.userAuthHeaders)
        assertEquals(1, intra.tokenRequests.get())
    }
}
