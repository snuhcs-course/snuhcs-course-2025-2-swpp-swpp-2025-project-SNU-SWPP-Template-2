package com.example.mindlog.auth

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest


class TestAuthDispatcher : Dispatcher() {

    /** 회원가입/로그인/리프레시가 성공할지 여부 제어 플래그 */
    var shouldSignupSucceed: Boolean = true
    var shouldLoginSucceed: Boolean = true
    var shouldRefreshSucceed: Boolean = true

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path.orEmpty()
        val method = request.method ?: "GET"

        return when {
            path.startsWith("/auth/signup") && method == "POST" -> {
                if (shouldSignupSucceed) {
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                            """
                            {
                              "access": "signup_access_token",
                              "refresh": "signup_refresh_token"
                            }
                            """.trimIndent()
                        )
                } else {
                    MockResponse()
                        .setResponseCode(400)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""{"detail": "Signup failed in TestAuthDispatcher"}""")
                }
            }

            path.startsWith("/auth/login") && method == "POST" -> {
                if (shouldLoginSucceed) {
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                            """
                            {
                              "access": "login_access_token",
                              "refresh": "login_refresh_token"
                            }
                            """.trimIndent()
                        )
                } else {
                    MockResponse()
                        .setResponseCode(401)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""{"detail": "Invalid credentials in TestAuthDispatcher"}""")
                }
            }

            path.startsWith("/auth/refresh") && method == "POST" -> {
                if (shouldRefreshSucceed) {
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                            """
                            {
                              "access": "refreshed_access_token",
                              "refresh": "refreshed_refresh_token"
                            }
                            """.trimIndent()
                        )
                } else {
                    MockResponse()
                        .setResponseCode(401)
                        .setHeader("Content-Type", "application/json")
                        .setBody("""{"detail": "Refresh failed in TestAuthDispatcher"}""")
                }
            }

            path.startsWith("/auth/verify") && method == "POST" -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"detail": "ok"}""")
            }

            path.startsWith("/auth/logout") && method == "POST" -> {
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"detail": "logged out"}""")
            }

            else -> {
                MockResponse().setResponseCode(404)
            }
        }
    }
}