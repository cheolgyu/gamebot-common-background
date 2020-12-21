package com.highserpot.background

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.highserpot.background.service.BackgroundService
import com.kakao.sdk.auth.LoginClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.template.model.*
import com.kakao.sdk.user.UserApiClient

open class MediaProjectionActivity : AppCompatActivity() {
    var mIntent: Intent? = null
    var REQ_CODE_OVERLAY_PERMISSION = 1

    fun new_bg(): Intent {
        if (mIntent == null) {
            mIntent = Intent(applicationContext, BackgroundService::class.java)
            return mIntent!!
        } else {
            return mIntent!!
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mediaprojection)

        val textView = findViewById<TextView>(R.id.manual)
        textView.text = Html.fromHtml(getString(R.string.manual))

        val action = intent.extras?.getString("action")
        mIntent = new_bg()
        if (action != null && action == "stop") {
            stopService(mIntent!!)
        }

        var keyHash = Utility.getKeyHash(this)
        Log.d("keyHash", keyHash)
        KakaoSdk.init(this, getString(R.string.NATIVE_APP_KEY))

    }
    var TAG = "카카오"
    fun login_kakao(view: View?) {
        // 로그인 조합 예제

// 로그인 공통 callback 구성
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e(TAG, "로그인 실패", error)
            }
            else if (token != null) {
                Log.i(TAG, "로그인 성공 ${token.accessToken}")


            }
        }

// 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (LoginClient.instance.isKakaoTalkLoginAvailable(this)) {
            LoginClient.instance.loginWithKakaoTalk(this, callback = callback)
        } else {
            LoginClient.instance.loginWithKakaoAccount(this, callback = callback)
        }

        // 토큰 정보 보기
        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                Log.e(TAG, "토큰 정보 보기 실패", error)
            }
            else if (tokenInfo != null) {
                Log.i(TAG, "토큰 정보 보기 성공" +
                        "\n회원번호: ${tokenInfo.id}" +
                        "\n만료시간: ${tokenInfo.expiresIn} 초")
            }
        }
    }

    fun kakao_send(view: View?) {
        val defaultFeed = FeedTemplate(
            content = Content(
                title = "딸기 치즈 케익",
                description = "#케익 #딸기 #삼평동 #카페 #분위기 #소개팅",
                imageUrl = "http://mud-kage.kakao.co.kr/dn/Q2iNx/btqgeRgV54P/VLdBs9cvyn8BJXB3o7N8UK/kakaolink40_original.png",
                link = Link(
                    webUrl = "https://developers.kakao.com",
                    mobileWebUrl = "https://developers.kakao.com"
                )
            ),
            social = Social(
                likeCount = 286,
                commentCount = 45,
                sharedCount = 845
            ),
            buttons = listOf(
                Button(
                    "웹으로 보기",
                    Link(
                        webUrl = "https://developers.kakao.com",
                        mobileWebUrl = "https://developers.kakao.com"
                    )
                ),
                Button(
                    "앱으로 보기",
                    Link(
                        androidExecParams = mapOf("key1" to "value1", "key2" to "value2"),
                        iosExecParams = mapOf("key1" to "value1", "key2" to "value2")
                    )
                )
            )
        )

        TalkApiClient.instance.sendDefaultMemo(defaultFeed) { error ->
            if (error != null) {
                Log.e(TAG, "나에게 보내기 실패", error)
            } else {
                Log.i(TAG, "나에게 보내기 성공")
            }
        }


    }



    fun service_stop_btn(view: View?) {
        stopService(mIntent)
    }

    fun service_start_btn(view: View?) {
        Log.d(
            "탑뷰",
            Settings.canDrawOverlays(applicationContext).toString()
        )
        if (Settings.canDrawOverlays(applicationContext)) {
            if (CheckTouch(this).chk()) {
                var mediaProjectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                var captureIntent: Intent = mediaProjectionManager.createScreenCaptureIntent()
                startActivityForResult(captureIntent, 1000)
            } else {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.accessibility_service_description_need),
                    Toast.LENGTH_SHORT
                ).show()

            }
        } else {
            onObtainingPermissionOverlayWindow()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            mIntent?.putExtra("resultCode", resultCode)
            mIntent?.putExtra("data", data)
            startService(mIntent)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    open fun onObtainingPermissionOverlayWindow() {
        Log.d(
            "탑뷰", "권한"

        )
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION)
    }

    fun danger_click(view: View?) {
        CheckTouch(this).alert_dialog()
    }

    fun sample_click(view: View?) {
        var intent =  Intent(applicationContext,SampleActivity::class.java)
        startActivity(intent)
    }

}