package com.instamini.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.format.Formatter
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import java.io.File
import kotlin.math.abs

private const val TAG = "InstaMini"
private const val HOME = "https://www.instagram.com/"
private const val DM = "https://www.instagram.com/direct/inbox/"
private const val REQ_FILE = 1

// JS 인터페이스 없이 evaluateJavascript로만 주입한다. (Instagram 화면 구조는 직접 확인하지 못해 추정 기반)
// - 경로(home / inbox / story / other)가 바뀌는 즉시(pushState·popstate·Navigation API) 클래스와 감시 상태를 갱신한다.
// - MutationObserver는 home/inbox에서만 켜고, 그 외(Stories 상세·대화방·릴스)에서는 끈다.
// - home: 게시물(article)·로딩 스피너·"추천 게시물"과 그 뒤 요소(하단 메뉴 포함)를 숨겨 더 불러오기 감시 요소까지 화면 밖으로 뺀다.
//   Stories 상세(/stories)에서도 이미 표시한 피드 숨김은 유지한다(뒤에서 피드가 다시 로딩되지 않도록).
// - inbox: 맨 위 헤더 줄 숨김. 릴스 스크롤 잠금용 touchmove 핸들러는 '그 외' 경로(대화방·릴스)에서만 등록한다.
// - 디버그 빌드에서만 window.__imDbg=1 이 주입되어 [im] 로그를 남긴다.
private const val HIDE_JS = """(function(){
if(window.__im)return;window.__im=1;
var d=document,H=d.documentElement,DBG=!!window.__imDbg;
function L(m){if(DBG)console.log('[im] '+m)}
var st=d.createElement('style');
st.textContent='a[href^="/reels"],a[href^="/explore"],article{display:none!important}'
+'html.im-home [data-imh],html.im-story [data-imh],html.im-inbox [data-imi]{display:none!important}'
+'html.im-home svg[aria-label="Loading..."],html.im-home svg[aria-label="로드 중..."],html.im-home [role="progressbar"]{display:none!important}';
(d.head||H).appendChild(st);
function P(){return location.pathname}
function kind(){var p=P();return p==='/'?'home':p.indexOf('/direct/inbox')===0?'inbox':p.indexOf('/stories')===0?'story':'other'}
function isReelUrl(){return P().split('/').indexOf('reel')>=0}
function bigVideo(){var a=d.querySelectorAll('video'),h=innerHeight*0.6;for(var i=0;i<a.length;i++){var r=a[i].getBoundingClientRect();if(r.height>h&&r.bottom>0&&r.top<innerHeight)return true}return false}
function locked(){return kind()!=='story'&&(isReelUrl()||bigVideo())}
function kill(v){try{v.pause();v.removeAttribute('src');v.preload='none';v.load()}catch(e){}}
function mark(n){if(n&&n.setAttribute&&!n.hasAttribute('data-imh')){n.setAttribute('data-imh','1');return 1}return 0}
function after(n){var c=0,k=0;while(n&&n.parentElement&&n.parentElement!==d.body&&c<12){var x=n.nextElementSibling;while(x){k+=mark(x);x=x.nextElementSibling}n=n.parentElement;c++}return k}
function blockOf(n){var c=0;while(n.parentElement&&n.parentElement!==d.body&&!n.parentElement.querySelector('img,canvas')&&c<12){n=n.parentElement;c++}return n}
var ft=null,fts=0;
function findText(){
if(ft&&ft.isConnected)return ft;
if(!d.body)return null;
var now=Date.now();if(now-fts<2000)return null;fts=now;
var w=d.createTreeWalker(d.body,NodeFilter.SHOW_TEXT),n;
while((n=w.nextNode())){if(/추천 게시물|Suggested [Pp]osts/.test(n.nodeValue)){ft=n.parentElement;return ft}}
return null}
var nav=null;
function feed(){
var a=[],t=findText(),n=0,i,j;
if(t)a.push(t);
var q=d.querySelectorAll('article,svg[aria-label="Loading..."],svg[aria-label="로드 중..."],[role="progressbar"]');
for(i=0;i<q.length&&i<8;i++)a.push(q[i]);
for(j=0;j<a.length;j++){var b=blockOf(a[j]);n+=mark(b)+after(b)}
if(!(nav&&nav.isConnected)){nav=null;var l=d.querySelectorAll('a[href="/direct/inbox/"]');
for(i=0;i<l.length&&!nav;i++){var e=l[i],c=0;while(e&&e!==d.body&&c<10){if(getComputedStyle(e).position==='fixed'&&e.getBoundingClientRect().bottom>innerHeight*0.7){n+=mark(e);nav=e;break}e=e.parentElement;c++}}}
var v=d.querySelectorAll('video:not([data-imk])');
for(i=0;i<v.length;i++){v[i].setAttribute('data-imk','1');kill(v[i])}
if(n||v.length)L('feed anchors='+a.length+' marked='+n+' nav='+(nav?1:0)+' videos='+v.length)
}
var hd=null;
function inboxHeader(){
if(hd&&hd.isConnected)return;
if(!d.elementsFromPoint)return;
var w=innerWidth,all=d.elementsFromPoint(w/2,30),e,r;
for(var i=0;i<all.length;i++){e=all[i];if(e===H||e===d.body)continue;r=e.getBoundingClientRect();
if(r.top>-2&&r.top<24&&r.height>=32&&r.height<=96&&r.width>=w*0.9&&e.querySelectorAll('svg').length>=2){hd=e;e.setAttribute('data-imi','1');L('inbox header hidden');return}}
}
var ob=null,tm=0,last='';
function run(){tm=0;var k=kind();if(k==='home')feed();else if(k==='inbox')inboxHeader()}
function sched(){if(!tm)tm=setTimeout(run,250)}
function watch(on){
if(on&&!ob){ob=new MutationObserver(sched);ob.observe(H,{childList:true,subtree:true});L('observer on')}
else if(!on&&ob){ob.disconnect();ob=null;if(tm){clearTimeout(tm);tm=0}L('observer off')}
}
var lk=false,lkOn=false;
function ts(){lk=locked()}
function tmv(e){if(lk)e.preventDefault()}
function wh(e){if(locked())e.preventDefault()}
function kd(e){var k=e.key;if((k==='ArrowDown'||k==='ArrowUp'||k==='PageDown'||k==='PageUp'||k===' ')&&locked())e.preventDefault()}
function lockOn(on){
if(on===lkOn)return;lkOn=on;
var f=on?'addEventListener':'removeEventListener';
d[f]('touchstart',ts,{passive:true,capture:true});
d[f]('touchmove',tmv,{passive:false,capture:true});
d[f]('wheel',wh,{passive:false,capture:true});
d[f]('keydown',kd,true);
L('reel lock '+(on?'on':'off'))
}
function route(){
var k=kind();
if(k===last)return;
last=k;
H.classList.toggle('im-home',k==='home');
H.classList.toggle('im-story',k==='story');
H.classList.toggle('im-inbox',k==='inbox');
watch(k==='home'||k==='inbox');
lockOn(k==='other');
L('route '+k);
if(k==='home'||k==='inbox')run()
}
['pushState','replaceState'].forEach(function(n){var o=history[n];history[n]=function(){var r=o.apply(this,arguments);route();return r}});
addEventListener('popstate',route);
if(window.navigation&&window.navigation.addEventListener)window.navigation.addEventListener('currententrychange',route);
route();
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

    // 디버그 빌드에서만 로그/원격 디버깅을 켠다. 배포용(release)에서는 모두 꺼진다.
    private val dbg by lazy { applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    private fun log(m: String) {
        if (dbg) Log.d(TAG, m)
    }

    private var dark = false
    private var webTab = 1 // 0 = Stories, 1 = DM
    private var storiesMode = false
    private var booting = true      // 시작 시 홈을 한 번 불러온 뒤 같은 페이지 안에서 DM으로 이동하는 중
    private var bootRunning = false
    private var bootTries = 0
    private var filePicker: ValueCallback<Array<Uri>>? = null
    private var lockUntil = 0L      // 전환이 진행되는 동안(경로가 아직 안 바뀐 구간) 새 스와이프를 받지 않는다

    // ---- 스와이프 제스처 상태 ----
    private enum class G { NONE, UNDECIDED, HORIZONTAL, VERTICAL, IGNORED }
    private var g = G.NONE
    private var gPointer = -1
    private var gX0 = 0f
    private var gY0 = 0f
    private var gRelY = 0f          // WebView 기준 시작 y (창 좌표와 구분)
    private var gRelH = 1
    private var gEligible = 0       // +1: 오른쪽 스와이프 허용(DM→Stories), -1: 왼쪽 허용(Stories→DM), 0: 불가
    private var gCommitted = false
    private var vt: VelocityTracker? = null
    private val loc = IntArray(2)
    private val slop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    private val minFling by lazy { ViewConfiguration.get(this).scaledMinimumFlingVelocity }
    private val maxFling by lazy { ViewConfiguration.get(this).scaledMaximumFlingVelocity }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        dark = prefs.getBoolean("dark", false)
        setTheme(if (dark) R.style.Theme_InstaMini_Dark else R.style.Theme_InstaMini)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyBars(color(R.attr.imBg))

        // 원격 디버깅(chrome://inspect)은 디버그 빌드에서만 켠다. 배포용(release) APK에서는 꺼져 있다.
        if (dbg) WebView.setWebContentsDebuggingEnabled(true)
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
            // 임시 캐시 전부 삭제: HTTP 캐시 + cacheDir. 쿠키/Local Storage/DOM Storage는 dataDir에 있어 영향 없음.
            web.clearCache(true)
            Thread {
                clearDir(cacheDir)
                runOnUiThread { refreshCacheSize() }
            }.start()
        }

        // 시작: 홈을 한 번 불러온 뒤 같은 페이지 안에서 DM으로 이동한다.
        // 이렇게 해야 이후 Stories↔DM 전환이 페이지 재로딩 없이(history 이동으로) 이뤄진다. 그동안은 cover로 가린다.
        webTab = 1
        select(1)
    }

    private open inner class AppClient : WebViewClient() {

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
            log("history -> ${kind(p)} tab=$webTab")
            if (isBlocked(p) || (p == "/" && !storiesMode)) {
                // 로그인 직후처럼 이전 기록이 로그인 화면이면 되돌아가지 않고 DM을 연다.
                val l = view.copyBackForwardList()
                val prev = if (l.currentIndex > 0) l.getItemAtIndex(l.currentIndex - 1)?.url else null
                val prevOk = prev != null && !pathOf(Uri.parse(prev)).startsWith("/accounts")
                if (view.canGoBack() && prevOk) view.goBack() else view.loadUrl(DM)
            }
        }

        // 첫 화면이 보이는 즉시 주입해 피드가 그려지기 전에 숨김 규칙을 적용한다. (window.__im 로 중복 실행 방지)
        override fun onPageCommitVisible(view: WebView, url: String?) {
            inject(view, url)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            inject(view, url)
            if (booting) startBoot() else hideCoverLater()
        }

        // 렌더러 프로세스가 죽으면(저메모리 등) 앱 크래시 대신 액티비티를 새로 만든다.
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            recreate()
            return true
        }
    }

    // 디버그 빌드에서만 네트워크 요청의 경로(숫자 ID는 가림)와 GraphQL 이름 헤더를 기록해 피드 요청을 확인할 수 있게 한다.
    // 요청 본문, 쿠키, 쿼리 문자열은 기록하지 않는다. 배포용에서는 이 재정의 자체가 없다.
    private val client: WebViewClient by lazy {
        if (dbg) object : AppClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val u = request.url
                val p = u.path.orEmpty()
                if (isInstagram(u.host) && (p.startsWith("/api") || p.startsWith("/graphql"))) {
                    val h = request.requestHeaders
                    val fn = h["x-fb-friendly-name"] ?: h["X-FB-Friendly-Name"] ?: "-"
                    Log.d(TAG, "net ${request.method} ${p.replace(Regex("\\\\d{5,}"), "#")} fn=$fn")
                }
                return null
            }
        } else AppClient()
    }

    private fun inject(view: WebView, url: String?) {
        if (url != null && isInstagram(Uri.parse(url).host)) {
            view.evaluateJavascript(if (dbg) "window.__imDbg=1;$HIDE_JS" else HIDE_JS, null)
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

        // 디버그 빌드: 주입 JS의 "[im]" 로그만 Logcat으로 전달한다(페이지 내용은 전달하지 않음).
        override fun onConsoleMessage(m: ConsoleMessage): Boolean {
            if (dbg && m.message().startsWith("[im]")) {
                Log.d(TAG, m.message())
                return true
            }
            return false
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

    // 시작 시퀀스: 홈이 렌더링되면 DM 링크를 눌러 같은 페이지 안에서 DM으로 이동한다.
    private fun startBoot() {
        if (bootRunning) return
        // 홈이 아닌 곳(로그인 화면 등)이면 이동하지 않고 그대로 보여준다.
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
                // 링크를 못 찾으면 기존 방식(전체 로드)으로 대체
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

    // ------------------------------------------------------------------------------------
    // 스와이프: 오른쪽 스와이프 = Stories(DM 목록에서), 왼쪽 스와이프 = DM(Stories 홈에서).
    //
    // 터치는 기본적으로 항상 WebView에 그대로 전달한다. 다음이 모두 확인된 경우에만 가로채 전환한다.
    //  - 시작 지점이 WebView 아래쪽 64%(스토리 트레이·메모 행의 가로 스크롤과 겹치지 않도록)
    //  - 터치 슬롭을 넘긴 뒤 가로 이동이 세로 이동의 1.5배 이상으로 우세
    //  - 허용된 방향이고, 충분히 멀리(화면 너비 30%) 끌었거나 충분히 빠른(최소 플링 속도의 6배) 데다 12% 이상 이동
    // 확정되면 WebView에 ACTION_CANCEL을 보내고 그 제스처의 나머지 이벤트를 소비한다(한 제스처에 한 번만 전환).
    // 멀티터치, 취소, 손 뗌 때는 상태를 항상 초기화한다.
    // ------------------------------------------------------------------------------------
    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> gestureDown(e)
            MotionEvent.ACTION_POINTER_DOWN -> if (g != G.NONE) g = G.IGNORED // 핀치 등 멀티터치
            MotionEvent.ACTION_MOVE -> if (gestureMove(e)) return true
            MotionEvent.ACTION_UP -> if (gestureUp(e)) return true
            MotionEvent.ACTION_CANCEL -> {
                if (g != G.NONE) log("gesture cancel state=$g")
                endGesture()
            }
        }
        return super.dispatchTouchEvent(e)
    }

    private fun gestureDown(e: MotionEvent) {
        endGesture() // 이전 제스처 상태가 남아 있지 않도록 항상 먼저 초기화
        gPointer = e.getPointerId(0)
        gX0 = e.x
        gY0 = e.y
        web.getLocationOnScreen(loc)
        gRelY = e.rawY - loc[1]
        gRelH = maxOf(web.height, 1)
        vt = VelocityTracker.obtain().also { it.addMovement(e) }
        g = if (settingsPane.visibility == View.VISIBLE || booting) G.IGNORED else G.UNDECIDED
        gEligible = if (gRelY < gRelH * 0.36f || SystemClock.uptimeMillis() < lockUntil) 0 else swipeDirection()
        log("down relY=${gRelY.toInt()}/$gRelH state=$g eligible=$gEligible tab=$webTab kind=${kind(currentPath())}")
    }

    private fun gestureMove(e: MotionEvent): Boolean {
        if (gCommitted) return true // 확정된 제스처의 나머지 이벤트는 소비
        if (g != G.UNDECIDED && g != G.HORIZONTAL) return false
        val idx = e.findPointerIndex(gPointer)
        if (idx < 0) {
            g = G.IGNORED
            return false
        }
        vt?.addMovement(e)
        val dx = e.getX(idx) - gX0
        val dy = e.getY(idx) - gY0
        if (g == G.UNDECIDED) {
            if (abs(dx) < slop && abs(dy) < slop) return false
            g = if (abs(dx) > abs(dy) * 1.5f) G.HORIZONTAL else G.VERTICAL
            log("direction=$g dx=${dx.toInt()} dy=${dy.toInt()}")
            if (g == G.VERTICAL) return false
        }
        if (gEligible != 0 && swipeOk(dx, currentVx())) {
            commitSwipe(e, dx, cancelWeb = true)
            return true
        }
        return false
    }

    private fun gestureUp(e: MotionEvent): Boolean {
        if (gCommitted) {
            endGesture()
            return true
        }
        val idx = e.findPointerIndex(gPointer)
        if (idx >= 0 && (g == G.HORIZONTAL || g == G.VERTICAL)) {
            vt?.addMovement(e)
            val dx = e.getX(idx) - gX0
            val dy = e.getY(idx) - gY0
            if (g == G.HORIZONTAL) {
                val vx = currentVx()
                log("up horizontal dx=${dx.toInt()} vx=${vx.toInt()} eligible=$gEligible")
                if (gEligible != 0 && swipeOk(dx, vx)) commitSwipe(e, dx, cancelWeb = false)
            } else {
                onPull(dx, dy)
            }
        }
        endGesture()
        return false
    }

    private fun currentVx(): Float {
        val t = vt ?: return 0f
        t.computeCurrentVelocity(1000, maxFling.toFloat())
        return t.xVelocity
    }

    // 방향이 허용되고, 멀리 끌었거나 빠르게 쓸었을 때만 true. 기준은 기기의 터치 설정과 화면 너비에서 계산한다.
    private fun swipeOk(dx: Float, vx: Float): Boolean {
        val dirOk = (dx > 0 && gEligible == 1) || (dx < 0 && gEligible == -1)
        if (!dirOk) return false
        val w = maxOf(web.width, 1)
        val far = abs(dx) >= w * 0.30f
        val fast = abs(vx) >= minFling * 6f && abs(dx) >= w * 0.12f
        return far || fast
    }

    private fun commitSwipe(e: MotionEvent, dx: Float, cancelWeb: Boolean) {
        gCommitted = true
        val target = if (dx > 0) 0 else 1
        log("swipe COMMIT dx=${dx.toInt()} -> tab $target (cancelWeb=$cancelWeb)")
        if (cancelWeb) {
            val c = MotionEvent.obtain(e)
            c.action = MotionEvent.ACTION_CANCEL
            super.dispatchTouchEvent(c) // WebView가 진행 중이던 스크롤/탭을 취소하게 한다
            c.recycle()
        }
        select(target)
    }

    private fun endGesture() {
        vt?.recycle()
        vt = null
        g = G.NONE
        gPointer = -1
        gCommitted = false
        gEligible = 0
    }

    // 지금 화면에서 허용되는 스와이프 방향. DM 목록에서만 오른쪽(→Stories), Stories 홈에서만 왼쪽(→DM).
    // 대화방, Stories 상세, 그 외 화면에서는 0이라 터치를 전혀 건드리지 않는다.
    private fun swipeDirection(): Int {
        val path = currentPath() ?: return 0
        return when {
            webTab == 1 && path.startsWith("/direct/inbox") -> 1
            webTab == 0 && path == "/" -> -1
            else -> 0
        }
    }

    // 당겨서 새로고침: 화면 맨 위 15%에서 시작해 35% 이상 아래로 끈 세로 제스처. DM 목록과 Stories 홈에서만 동작.
    private fun onPull(dx: Float, dy: Float) {
        if (settingsPane.visibility == View.VISIBLE || booting) return
        if (dy < gRelH * 0.35f || dy < abs(dx) * 2) return
        if (gRelY > gRelH * 0.15f || web.canScrollVertically(-1)) return
        val path = currentPath() ?: return
        if (path == "/" || path.startsWith("/direct/inbox")) {
            log("pull refresh")
            web.reload()
        }
    }

    private fun currentPath(): String? = web.url?.let { pathOf(Uri.parse(it)) }

    // 화면 종류만 로그에 남긴다(주소의 대화방 ID 등 식별 정보는 기록하지 않음).
    private fun kind(p: String?): String = when {
        p == null -> "none"
        p == "/" -> "home"
        p.startsWith("/direct/inbox") -> "inbox"
        p.startsWith("/direct/") -> "thread"
        p.startsWith("/stories") -> "story"
        p.startsWith("/accounts") -> "login"
        else -> "other"
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
        if (web.url == null) {
            // 최초 시작: 홈을 먼저 불러온다(startBoot가 DM으로 이동시킴)
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

    /**
     * Stories(0)/DM(1) 전환. 페이지를 다시 불러오지 않는다.
     * 1) 기록(history)에 목적지가 있으면 그 위치로 이동 (같은 페이지 안의 이동이라 즉시 전환)
     * 2) 없으면 Instagram 내부 링크 클릭
     * 3) 그래도 안 되면 전체 로드 (이때만 cover로 Instagram 로고 화면을 가림)
     * 1), 2)는 이동 뒤 실제 경로를 확인해서, 안 바뀌었으면 3)으로 대체한다.
     */
    private fun go(target: Int) {
        lockUntil = SystemClock.uptimeMillis() + 450
        val l = web.copyBackForwardList()
        val cur = l.currentIndex
        var best = 0
        for (d in 1..maxOf(cur, l.size - 1 - cur)) {
            if (cur - d >= 0 && matches(l.getItemAtIndex(cur - d)?.url, target)) { best = -d; break }
            if (cur + d < l.size && matches(l.getItemAtIndex(cur + d)?.url, target)) { best = d; break }
        }
        log("go target=$target history=$best from=${kind(currentPath())}")
        if (best != 0) {
            web.goBackOrForward(best)
            verifyLater(target)
            return
        }
        val href = if (target == 0) "/" else "/direct/inbox/"
        web.evaluateJavascript(clickJs(href)) { r ->
            if (r == "1") {
                log("go via link click")
                verifyLater(target)
            } else {
                log("go FALLBACK full load")
                cover.visibility = View.VISIBLE
                web.loadUrl(if (target == 0) HOME else DM)
            }
        }
    }

    // 전환 성공 확인. 같은 탭을 아직 가리키고 있는데 경로가 목적지가 아니면 전체 로드로 대체한다.
    private fun verifyLater(target: Int) {
        web.postDelayed({
            if (booting || webTab != target) return@postDelayed
            val p = currentPath()
            val ok = p != null && (if (target == 0) p == "/" else p.startsWith("/direct/inbox"))
            log("switch verify target=$target ok=$ok kind=${kind(p)}")
            if (!ok) {
                cover.visibility = View.VISIBLE
                web.loadUrl(if (target == 0) HOME else DM)
            }
        }, 900)
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
        recreate() // 테마 적용을 위해 한 번 재생성. 로그인(쿠키)은 유지됨.
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
        val p = currentPath()
        when {
            settingsPane.visibility == View.VISIBLE -> select(webTab)
            // Stories 홈/DM 목록에서 뒤로가기는 앱 종료. (기록 이동을 하면 탭 표시와 어긋나고 페이지를 다시 불러온다)
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
        CookieManager.getInstance().flush() // 로그인 쿠키를 디스크에 확정 저장
        super.onPause()
    }

    override fun onDestroy() {
        endGesture()
        (web.parent as? ViewGroup)?.removeView(web)
        web.destroy()
        super.onDestroy()
    }
}
