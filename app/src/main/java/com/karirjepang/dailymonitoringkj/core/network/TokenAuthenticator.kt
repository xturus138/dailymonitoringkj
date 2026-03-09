package com.karirjepang.dailymonitoringkj.core.network

import android.util.Log
import com.karirjepang.dailymonitoringkj.BuildConfig
import com.karirjepang.dailymonitoringkj.core.model.LoginRequest
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Automatically re-authenticates when the server responds with 401.
 * Calls the login endpoint, saves the new token, and retries the failed request.
 *
 * Prevents infinite loops by only retrying once per request.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val TAG = "TokenAuthenticator"

    @Volatile
    private var isRefreshing = false

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we already retried once (prevent infinite loop)
        if (responseCount(response) >= 2) {
            Log.w(TAG, "Already retried once, giving up")
            return null
        }

        // Don't try to refresh if the login endpoint itself returned 401
        if (response.request.url.encodedPath.contains("login")) {
            Log.w(TAG, "Login endpoint returned 401, credentials may be wrong")
            return null
        }

        synchronized(this) {
            if (isRefreshing) return null
            isRefreshing = true
        }

        try {
            val newToken = refreshToken()
            if (newToken != null) {
                tokenManager.saveToken(newToken)
                Log.d(TAG, "Token refreshed successfully")

                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed", e)
        } finally {
            isRefreshing = false
        }

        return null
    }

    /**
     * Performs a synchronous login call to get a new token.
     * Uses a cached bare OkHttpClient with SSL bypass (no interceptors) to avoid circular dependency.
     */
    private fun refreshToken(): String? {
        val client = bareClient

        val loginRequest = LoginRequest(BuildConfig.API_EMAIL, BuildConfig.API_PASSWORD)
        val json = Gson().toJson(loginRequest)
        val body = json.toRequestBody("application/json; charset=UTF-8".toMediaType())

        val request = Request.Builder()
            .url("https://dailymonitoringkj.id/api/login")
            .post(body)
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        return response.use { resp ->
            if (resp.isSuccessful) {
                val responseBody = resp.body?.string()
                val loginResponse = Gson().fromJson(responseBody, LoginResponseRaw::class.java)
                loginResponse?.token
            } else {
                Log.w(TAG, "Token refresh failed: ${resp.code}")
                null
            }
        }
    }

    companion object {
        /** Lazy-initialized bare OkHttpClient — shared across all 401 retries */
        private val bareClient: okhttp3.OkHttpClient by lazy {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            okhttp3.OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    /** Minimal model just for parsing the login response token */
    private data class LoginResponseRaw(val token: String?)
}

