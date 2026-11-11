package com.instamini.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import java.io.File
import kotlin.math.abs

private const val HOME = "https://www.instagram.com/"
private const val DM = "https://www.instagram.com/direct/inbox/"
private const val REQ_FILE = 1

// JS 인터페이스 없이 evaluateJavascript로만 주입.
// 1) Reels 피드/Explore 링크와 피드 게시물을 숨김 (DM으로 받은 단일 릴스 /reel/ 링크는 유지)
// 2) 홈(/)에서는 video를 즉시 정지·소스 제거 (스토리 종료 후 피드 영상 자동재생으로 인한 데이터 손실 방지)
// 3) 단일 릴스(/reel/) 화면에서는 스크롤/스와이프/방향키 이동을 막음
private const val HIDE_JS = """(function(){
if(window.__im)return;window.__im=1;
var s=document.createElement('style');
s.textContent='a[href^="/reels"],a[href^="/explore"],article{display:none!important}';
(document.head||document.documentElement).appendChild(s);
function isHome(){return location.pathname==='/'}
function isReel(){return location.pathname.split('/').indexOf('reel')>=0}
function kill(v){try{v.pause();v.removeAttribute('src');v.preload='none';v.load()}catch(e){}}
new MutationObserver(function(){
if(!isHome())return;
var a=document.querySelectorAll('video');
for(var i=0;i<a.length;i++)kill(a[i]);
}).observe(document.documentElement,{childList:true,subtree:true});
function lock(e){if(isReel())e.preventDefault()}
document.addEventListener('touchmove',lock,{passive:false,capture:true});
document.addEventListener('wheel',lock,{passive:false,capture:true});
document.addEventListener('keydown',function(e){
var k=e.key;
if(isReel()&&(k==='ArrowDown'||k==='ArrowUp'||k==='PageDown'||k==='PageUp'||k===' '))e.preventDefault();
},true);
})()"""

class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var settingsPane: View
    private lateinit var cacheSize: TextView
    private lateinit var tabs: Array<TextView>   // 0 = Stories, 1 = DM, 2 = 설정
    private lateinit var modes: Array<TextView>  // 0 = 라이트, 1 = 다크

    private var dark = false
    private var webTab = 1 // 0 = Stories, 1 = DM
    private var storiesMode = false
    private var filePicker: ValueCallback<Array<Uri>>? = null
    private var downX = 0f
    private var downY = 0f

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        dark = getSharedPreferences("s", MODE_PRIVATE).getBoolean("dark", false)
        setTheme(if (dark) R.style.Theme_InstaMini_Dark else R.style.Theme_InstaMini)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyBars(color(R.attr.imBg))

        web = findViewById(R.id.web)
        settingsPane = findViewById(R.id.settings_pane)
        cacheSize = findViewById(R.id.cache_size)
        tabs = arrayOf(
            findViewById(R.id.tab_stories),
            findViewById(R.id.tab_dm),
            findViewById(R.id.tab_settings)
        )
        modes = arrayOf(findViewById(R.id.opt_light), findViewById(R.id.opt_dark))

        CookieManager.getInstance().setAcceptCookie(true)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            if (Build.VERSION.SDK_INT in 29..32) {
                forceDark = if (dark) WebSettings.FORCE_DARK_AUTO else WebSettings.FORCE_DARK_OFF
            }
        }
        web.webViewClient = client
        web.webChromeClient = chrome

        for (i in tabs.indices) tabs[i].setOnClickListener { select(i) }
        for (i in modes.indices) modes[i].setOnClickListener { setDark(i == 1) }
        markModes()

        findViewById<View>(R.id.cache_clear).setOnClickListener {
            // 임시 캐시 전부 삭제: HTTP 캐시 + cacheDir. 쿠키/Local Storage/DOM Storage는 dataDir에 있어 영향 없음.
            web.clearCache(true)
            Thread {
                clearDir(cacheDir)
                runOnUiThread { refreshCacheSize() }
            }.start()
        }

        select(1)
    }

    private val client = object : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val u = request.url
            if (!isInstagram(u.host)) {
                openExternal(u)
                return true
            }
            val p = pathOf(u)
            if (isBlocked(p)) return true
            if (p == "/" && !storiesMode) {
                view.loadUrl(DM)
                return true
            }
            return false
        }

        // SPA 내부 이동(history.pushState)도 여기서 잡힌다.
        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            if (url == null) return
            val u = Uri.parse(url)
            if (!isInstagram(u.host)) return
            val p = pathOf(u)
            if (isBlocked(p) || (p == "/" && !storiesMode)) {
                if (view.canGoBack()) view.goBack() else view.loadUrl(DM)
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (url != null && isInstagram(Uri.parse(url).host)) {
                view.evaluateJavascript(HIDE_JS, null)
            }
        }

        // 렌더러 프로세스가 죽으면(저메모리 등) 앱 크래시 대신 액티비티를 새로 만든다.
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            recreate()
            return true
        }
    }

    // DM 사진/파일 첨부: 시스템 파일 선택기 사용 (별도 권한 불필요)
    private val chrome = object : WebChromeClient() {
        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams
        ): Boolean {
            filePicker?.onReceiveValue(null)
            filePicker = callback
            return try {
                startActivityForResult(params.createIntent(), REQ_FILE)
                true
            } catch (e: ActivityNotFoundException) {
                filePicker = null
                false
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_FILE) {
            filePicker?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            filePicker = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // 좌→우 스와이프 = Stories, 우→좌 스와이프 = DM.
    // 스토리 트레이/노트 행의 가로 스크롤과 겹치지 않도록 화면 아래 70%에서 시작한 스와이프만 인식한다.
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
            }
            MotionEvent.ACTION_UP -> onSwipe(e.x - downX, e.y - downY)
        }
        return super.dispatchTouchEvent(e)
    }

    private fun onSwipe(dx: Float, dy: Float) {
        if (settingsPane.visibility == View.VISIBLE) return
        if (abs(dx) < web.width * 0.35f || abs(dx) < abs(dy) * 2) return
        if (downY < web.height * 0.3f) return
        val path = pathOf(Uri.parse(web.url ?: return))
        if (dx > 0 && webTab == 1 && path.startsWith("/direct/inbox")) select(0)
        else if (dx < 0 && webTab == 0 && path == "/") select(1)
    }

    private fun select(i: Int) {
        for (k in tabs.indices) {
            val on = k == i
            tabs[k].setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
            tabs[k].setTextColor(color(if (on) R.attr.imText else R.attr.imGray))
        }
        if (i == 2) {
            settingsPane.visibility = View.VISIBLE
            refreshCacheSize()
            return
        }
        settingsPane.visibility = View.GONE
        // 설정에서 돌아올 때는 재로딩하지 않는다. WebView는 계속 살아 있다.
        if (i != webTab || web.url == null) {
            webTab = i
            storiesMode = i == 0
            web.loadUrl(if (i == 0) HOME else DM)
        }
    }

    private fun setDark(d: Boolean) {
        if (d == dark) return
        getSharedPreferences("s", MODE_PRIVATE).edit().putBoolean("dark", d).apply()
        recreate() // 테마 적용을 위해 한 번 재생성. 로그인(쿠키)은 유지됨.
    }

    private fun markModes() {
        for (k in modes.indices) {
            val on = (k == 1) == dark
            modes[k].setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
            modes[k].setTextColor(color(if (on) R.attr.imText else R.attr.imGray))
        }
    }

    @Suppress("DEPRECATION")
    private fun applyBars(bg: Int) {
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val v = window.decorView
        v.systemUiVisibility = if (dark) v.systemUiVisibility and light.inv() else v.systemUiVisibility or light
    }

    private fun color(attr: Int): Int {
        val t = TypedValue()
        theme.resolveAttribute(attr, t, true)
        return t.data
    }

    private fun refreshCacheSize() {
        cacheSize.setText(R.string.cache_calculating)
        Thread {
            val n = dirSize(cacheDir)
            runOnUiThread {
                cacheSize.text = getString(R.string.cache_size, Formatter.formatShortFileSize(this, n))
            }
        }.start()
    }

    private fun dirSize(f: File): Long {
        if (f.isFile) return f.length()
        var n = 0L
        f.listFiles()?.forEach { n += dirSize(it) }
        return n
    }

    private fun clearDir(dir: File) {
        dir.listFiles()?.forEach {
            if (it.isDirectory) clearDir(it)
            it.delete()
        }
    }

    private fun openExternal(u: Uri) {
        if (u.scheme != "http" && u.scheme != "https") return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, u))
        } catch (e: ActivityNotFoundException) {
            // 처리할 앱이 없으면 무시
        }
    }

    private fun isInstagram(host: String?): Boolean =
        host != null && (host == "instagram.com" || host.endsWith(".instagram.com"))

    private fun pathOf(u: Uri): String = u.path.orEmpty().ifEmpty { "/" }

    private fun isBlocked(path: String): Boolean =
        path.startsWith("/explore") || path.split('/').any { it == "reels" }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when {
            settingsPane.visibility == View.VISIBLE -> select(webTab)
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        web.resumeTimers()
    }

    override fun onPause() {
        web.onPause()
        web.pauseTimers()
        CookieManager.getInstance().flush() // 로그인 쿠키를 디스크에 확정 저장
        super.onPause()
    }

    override fun onDestroy() {
        (web.parent as? ViewGroup)?.removeView(web)
        web.destroy()
        super.onDestroy()
    }
}