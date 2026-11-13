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

// JS 인터페이스 없이 evaluateJavascript로만 주입한다. (Instagram 화면 구조는 직접 확인하지 못해 추정 기반)
// 1) Reels 피드/Explore 링크, 피드 게시물(article) 숨김
// 2) 홈(/): "추천 게시물" 블록·로딩 스피너·그 뒤의 모든 요소(하단 Instagram 메뉴 포함) 숨김, video 정지
//    -> 스토리 아래에서 피드가 계속 로딩되는 것을 막는다. 숨김은 html.im-home 클래스일 때만 적용되어 페이지를 벗어나면 풀린다.
// 3) DM 목록(/direct/inbox): 맨 위 헤더 줄(뒤로가기·계정명·새 메시지) 숨김
// 4) 릴스 화면(주소에 reel, 또는 화면 60% 이상의 큰 video)에서는 스크롤/스와이프/방향키 이동을 막음. 스토리는 제외.
private const val HIDE_JS = """(function(){
if(window.__im)return;window.__im=1;
var d=document,H=d.documentElement;
var s=d.createElement('style');
s.textContent='a[href^="/reels"],a[href^="/explore"],article{display:none!important}'
+'html.im-home [data-imh],html.im-home svg[aria-label="Loading..."],html.im-home svg[aria-label="로드 중..."],html.im-home [role="progressbar"],html.im-inbox [data-imi]{display:none!important}';
(d.head||H).appendChild(s);
function P(){return location.pathname}
function isHome(){return P()==='/'}
function isInbox(){return P().indexOf('/direct/inbox')===0}
function isStory(){return P().indexOf('/stories')===0}
function isReelUrl(){return P().split('/').indexOf('reel')>=0}
function bigVideo(){var a=d.querySelectorAll('video'),h=innerHeight*0.6;for(var i=0;i<a.length;i++){var r=a[i].getBoundingClientRect();if(r.height>h&&r.bottom>0&&r.top<innerHeight)return true}return false}
function locked(){return !isStory()&&(isReelUrl()||bigVideo())}
function kill(v){try{v.pause();v.removeAttribute('src');v.preload='none';v.load()}catch(e){}}
function mark(n){if(n&&n.setAttribute)n.setAttribute('data-imh','1')}
function after(n){var c=0;while(n&&n!==d.body&&c<12){var x=n.nextElementSibling;while(x){mark(x);x=x.nextElementSibling}n=n.parentElement;c++}}
function blockOf(n){var c=0;while(n.parentElement&&n.parentElement!==d.body&&!n.parentElement.querySelector('img,canvas')&&c<12){n=n.parentElement;c++}return n}
function findText(){var w=d.createTreeWalker(d.body,NodeFilter.SHOW_TEXT),n;while((n=w.nextNode())){if(/추천 게시물|Suggested [Pp]osts/.test(n.nodeValue))return n.parentElement}return null}
function feed(){
var a=[],t=findText();if(t)a.push(t);
var sp=d.querySelectorAll('svg[aria-label="Loading..."],svg[aria-label="로드 중..."],[role="progressbar"]');
for(var i=0;i<sp.length;i++)a.push(sp[i]);
for(var j=0;j<a.length;j++){var b=blockOf(a[j]);mark(b);after(b)}
var l=d.querySelectorAll('a[href="/direct/inbox/"]');
for(var k=0;k<l.length;k++){var e=l[k];while(e&&e!==d.body){if(getComputedStyle(e).position==='fixed'&&e.getBoundingClientRect().bottom>innerHeight*0.7){mark(e);break}e=e.parentElement}}
var v=d.querySelectorAll('video');for(var m=0;m<v.length;m++)kill(v[m]);
}
var hd=null;
function inboxHeader(){
if(hd&&hd.isConnected)return;
var w=innerWidth,all=d.elementsFromPoint(w/2,30),e,r;
for(var i=0;i<all.length;i++){e=all[i];if(e===H||e===d.body)continue;r=e.getBoundingClientRect();
if(r.top>-2&&r.top<24&&r.height>=32&&r.height<=96&&r.width>=w*0.9&&e.querySelectorAll('svg').length>=2){hd=e;e.setAttribute('data-imi','1');return}}
}
var tm=0;
function run(){tm=0;
H.classList.toggle('im-home',isHome());
H.classList.toggle('im-inbox',isInbox());
if(isHome())feed();
if(isInbox())inboxHeader();
}
function sched(){if(!tm)tm=setTimeout(run,400)}
new MutationObserver(sched).observe(H,{childList:true,subtree:true});
addEventListener('popstate',sched);
run();
var lk=false;
d.addEventListener('touchstart',function(){lk=locked()},{passive:true,capture:true});
d.addEventListener('touchmove',function(e){if(lk)e.preventDefault()},{passive:false,capture:true});
d.addEventListener('wheel',function(e){if(locked())e.preventDefault()},{passive:false,capture:true});
d.addEventListener('keydown',function(e){
var k=e.key;
if((k==='ArrowDown'||k==='ArrowUp'||k==='PageDown'||k==='PageUp'||k===' ')&&locked())e.preventDefault();
},true);
})()"""

// 페이지를 다시 불러오지 않고 Instagram 내부 링크를 눌러 이동(SPA 이동). 숨겨진 링크도 click()은 동작한다.
private fun clickJs(href: String) =
    "(function(){var a=document.querySelector('a[href=\"$href\"]');if(a){a.click();return 1}return 0})()"

class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var cover: View
    private lateinit var settingsPane: View
    private lateinit var cacheSize: TextView
    private lateinit var tabs: Array<TextView>      // 0 = Stories, 1 = DM, 2 = 설정
    private lateinit var modes: Array<TextView>     // 0 = 라이트, 1 = 다크

    private val prefs by lazy { getSharedPreferences("s", MODE_PRIVATE) }

    private var dark = false
    private var webTab = 1 // 0 = Stories, 1 = DM
    private var storiesMode = false
    private var booting = true
    private var bootRunning = false
    private var bootTries = 0
    private var filePicker: ValueCallback<Array<Uri>>? = null
    private var downX = 0f
    private var downY = 0f

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        dark = prefs.getBoolean("dark", false)
        setTheme(if (dark) R.style.Theme_InstaMini_Dark else R.style.Theme_InstaMini)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyBars(color(R.attr.imBg))

        web = findViewById(R.id.web)
        cover = findViewById(R.id.cover)
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
        setSel(modes, if (dark) 1 else 0)

        findViewById<View>(R.id.cache_clear).setOnClickListener {
            web.clearCache(true)
            Thread {
                clearDir(cacheDir)
                runOnUiThread { refreshCacheSize() }
            }.start()
        }

        webTab = 1
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

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            if (url == null) return
            val u = Uri.parse(url)
            if (!isInstagram(u.host)) return
            val p = pathOf(u)
            if (isBlocked(p) || (p == "/" && !storiesMode)) {
                val l = view.copyBackForwardList()
                val prev = if (l.currentIndex > 0) l.getItemAtIndex(l.currentIndex - 1)?.url else null
                val prevOk = prev != null && !pathOf(Uri.parse(prev)).startsWith("/accounts")
                if (view.canGoBack() && prevOk) view.goBack() else view.loadUrl(DM)
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (url != null && isInstagram(Uri.parse(url).host)) {
                view.evaluateJavascript(HIDE_JS, null)
            }
            if (booting) startBoot() else hideCoverLater()
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            recreate()
            return true
        }
    }

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

    private fun startBoot() {
        if (bootRunning) return
        val p = pathOf(Uri.parse(web.url ?: return))
        if (p != "/") {
            booting = false
            storiesMode = false
            hideCoverLater()
            return
        }
        bootRunning = true
        bootTries = 0
        bootStep()
    }

    private fun bootStep() {
        web.evaluateJavascript(clickJs("/direct/inbox/")) { r ->
            if (r == "1") {
                storiesMode = false
                booting = false
                bootRunning = false
                hideCoverLater()
            } else if (++bootTries < 15) {
                web.postDelayed({ if (booting) bootStep() }, 400)
            } else {
                storiesMode = false
                booting = false
                bootRunning = false
                web.loadUrl(DM)
            }
        }
    }

    private fun hideCoverLater() {
        cover.postDelayed({ cover.visibility = View.GONE }, 350)
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = e.x - downX
                val dy = e.y - downY
                onSwipe(dx, dy)
                onPull(dx, dy)
            }
        }
        return super.dispatchTouchEvent(e)
    }

    private fun onPull(dx: Float, dy: Float) {
        if (settingsPane.visibility == View.VISIBLE || booting) return
        if (dy < web.height * 0.35f || dy < abs(dx) * 2) return
        if (downY > web.height * 0.15f || web.canScrollVertically(-1)) return
        val path = pathOf(Uri.parse(web.url ?: return))
        if (path == "/" || path.startsWith("/direct/inbox")) web.reload()
    }

    private fun onSwipe(dx: Float, dy: Float) {
        if (settingsPane.visibility == View.VISIBLE || booting) return
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
        if (web.url == null) {
            webTab = 1
            storiesMode = true
            booting = true
            web.loadUrl(HOME)
        } else if (i != webTab) {
            webTab = i
            storiesMode = i == 0
            go(i)
        }
    }

    private fun go(target: Int) {
        val l = web.copyBackForwardList()
        val cur = l.currentIndex
        var best = 0
        for (d in 1..maxOf(cur, l.size - 1 - cur)) {
            if (cur - d >= 0 && matches(l.getItemAtIndex(cur - d)?.url, target)) { best = -d; break }
            if (cur + d < l.size && matches(l.getItemAtIndex(cur + d)?.url, target)) { best = d; break }
        }
        if (best != 0) {
            web.goBackOrForward(best)
            return
        }
        val href = if (target == 0) "/" else "/direct/inbox/"
        web.evaluateJavascript(clickJs(href)) { r ->
            if (r != "1") {
                cover.visibility = View.VISIBLE
                web.loadUrl(if (target == 0) HOME else DM)
            }
        }
    }

    private fun matches(url: String?, target: Int): Boolean {
        if (url == null) return false
        val u = Uri.parse(url)
        if (!isInstagram(u.host)) return false
        val p = pathOf(u)
        return if (target == 0) p == "/" else p.startsWith("/direct/inbox")
    }

    private fun setDark(d: Boolean) {
        if (d == dark) return
        prefs.edit().putBoolean("dark", d).apply()
        recreate()
    }

    private fun setSel(views: Array<TextView>, sel: Int) {
        for (k in views.indices) {
            val on = k == sel
            views[k].setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
            views[k].setTextColor(color(if (on) R.attr.imText else R.attr.imGray))
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
        val p = web.url?.let { pathOf(Uri.parse(it)) }
        when {
            settingsPane.visibility == View.VISIBLE -> select(webTab)
            p == "/" || p?.startsWith("/direct/inbox") == true -> super.onBackPressed()
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
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        (web.parent as? ViewGroup)?.removeView(web)
        web.destroy()
        super.onDestroy()
    }
}
