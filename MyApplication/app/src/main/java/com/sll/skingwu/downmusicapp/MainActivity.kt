package com.sll.skingwu.downmusicapp

import android.app.DownloadManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.EditText
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebBackForwardList
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import android.support.v4.content.ContextCompat.startActivity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.widget.Toast
import com.tencent.smtt.utils.v
import android.os.Environment.getExternalStorageDirectory
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.requestPermissions
import java.io.File
import android.Manifest
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build.ID
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.text.TextUtils
import android.view.Gravity
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.Toast.makeText
import kotlinx.android.synthetic.main.activity_main.*
import org.jsoup.select.Evaluator

class MainActivity : AppCompatActivity(), View.OnClickListener {

    var webView: WebView? = null
    var ed_url: EditText? = null
    private var webSettings: WebSettings? = null
    var context: Context? = null;
    private val APP_CACAHE_DIRNAME: String = "loadDataDir"
    var handler: Handler = Myhandler();
    var popupWindow: PopupWindow? = null;
    var musicUrl: String = ""
    var ed_name:EditText? = null
    private var downloadId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        innitView()
        innitData()

    }

    private fun innitView() {
        ed_url = findViewById(R.id.ed_url)
        var view: View = layoutInflater.inflate(R.layout.poppuwindow, null)
        popupWindow = PopupWindow(view,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        view.findViewById<Button>(R.id.bt_sure).setOnClickListener(this)
        view.findViewById<Button>(R.id.bt_cancel).setOnClickListener(this)
        ed_name = view.findViewById(R.id.ed_name)

    }

    private fun innitData() {

        handlePermisson()
        context = this
        innitWebView()
    }

    override fun onResume() {
        super.onResume()
        webSettings!!.setJavaScriptEnabled(true);
        var clipboardmanager: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        try {
            var clipboardText = clipboardmanager.primaryClip.getItemAt(0).uri.toString();
            Log.i("Mainactivity", "clipboardText = " + clipboardText)
            if (TextUtils.isEmpty(clipboardText)) {
                return
            }
            ed_url?.setText(clipboardText)
        } catch (e: Exception) {

        }
    }

    override fun onStop() {
        super.onStop()
        webSettings!!.setJavaScriptEnabled(false);
    }

    private fun loadWeb(view: View) {
        var ulr: String = ed_url?.text.toString();
        if (TextUtils.isEmpty(ulr.trim())) {
            Snackbar.make(window.decorView, "输入的网址不正确，请重试！", Toast.LENGTH_LONG).show()
            return
        }
        webView!!.loadUrl(ulr)

    }

    private fun loadWebByIntent(url: String) {
        webView!!.loadUrl(url)
    }

    /**
     * 动态申请权限
     */
    fun handlePermisson() {

        // 需要动态申请的权限
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        //查看是否已有权限
        val checkSelfPermission = context?.let { ActivityCompat.checkSelfPermission(it, permission) }

        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {


        } else {
            myRequestPermission()
        }
    }

    private fun myRequestPermission() {
        //可以添加多个权限申请
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE)
        if (android.os.Build.VERSION.SDK_INT > 23) {
            requestPermissions(permissions, 1)
        }

    }


    private fun innitWebView() {
        webView = WebView(context)
        //声明WebSettings子类
        webSettings = webView!!.getSettings();

        webSettings!!.setJavaScriptEnabled(true);
        webSettings!!.setPluginsEnabled(true);
        webSettings!!.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings!!.setLoadWithOverviewMode(true); // 缩放至屏幕的大小

        webSettings!!.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSettings!!.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings!!.setDisplayZoomControls(false); //隐藏原生的缩放控件

        webSettings!!.setCacheMode(WebSettings.LOAD_NO_CACHE); //打开webview中缓存
        webSettings!!.setAllowFileAccess(true); //设置可以访问文件
        webSettings!!.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings!!.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings!!.setDefaultTextEncodingName("utf-8");//设置编码格式

        webSettings!!.setDomStorageEnabled(true); // 开启 DOM storage API 功能
        webSettings!!.setDatabaseEnabled(true);   //开启 database storage API 功能
        webSettings!!.setAppCacheEnabled(true);//开启 Application Caches 功能

        var cacheDirPath: String = getFilesDir().getAbsolutePath() + APP_CACAHE_DIRNAME;
        webSettings!!.setAppCachePath(cacheDirPath); //设置  Application Caches 缓存目录

        webView!!.setWebViewClient(myWebViewClient())
    }


    inner class Myhandler : Handler() {
        override fun handleMessage(msg: Message?) {
            var url: String = msg?.obj.toString()
            Log.i("Mainactivity", "handleMessage() url = " + url)
            if(musicUrl.trim().equals(url)){
                return
            }
            musicUrl = url;
            webView?.stopLoading()
            popupWindow?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#06000000")));
            popupWindow?.setFocusable(true);
            popupWindow?.setOutsideTouchable(true);
            popupWindow?.showAtLocation(window.decorView.rootView,0,0,(Gravity.CENTER))
        }

    }

    inner class myWebViewClient : WebViewClient() {

        override fun onPageStarted(p0: WebView?, p1: String?, p2: Bitmap?) {
            super.onPageStarted(p0, p1, p2)
            bt_analys.setText("开始分析数据")
        }
        override fun shouldInterceptRequest(p0: WebView?, p1: String?): WebResourceResponse? {
            Log.i("Mainactivity", "shouldInterceptRequest() url = " + p1)
            if (!p1?.contains(".mp3")!!) {
                return null
            }
            var message: Message = Message();
            message.obj = p1;
            handler.sendMessage(message)
            return null
        }

        override fun onPageFinished(p0: WebView?, p1: String?) {
            super.onPageFinished(p0, p1)
//            if(popupWindow==null||!popupWindow?.isShowing!!){
//                Snackbar.make(window.decorView, "资源已经搜索完成！没有找到音乐文件！", Toast.LENGTH_LONG).show()
//            }
            bt_analys.setText("分析下载")
            webView?.goBack()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.bt_sure -> downLoadMusic(ed_name?.text.toString().trim())
            R.id.bt_cancel -> popupWindow?.dismiss()
            R.id.bt_analys ->loadWeb(v)
            R.id.bt_share ->  shareMusic()
        }
    }


    private  var saveFile: File? = null

    private fun downLoadMusic(name: String) {
        if (TextUtils.isEmpty(name)) {
            ed_url?.setError("请输入名称！")
            return
        }
        popupWindow?.dismiss()
        Snackbar.make(window.decorView,"开始下载音乐了！",Snackbar.LENGTH_LONG).show()
        bt_analys.setText("音乐正在下载中。。。")
        var request: DownloadManager.Request = DownloadManager.Request(Uri.parse(musicUrl))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setTitle("下载音乐中...");
        saveFile = File(getExternalStorageDirectory(), name + ".mp3")
        Log.i("MainActivity", "saveFile = " + saveFile?.path)
        request.setDestinationUri(Uri.fromFile(saveFile))
        val manager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = manager.enqueue(request)
        lisenterDownload()
    }

    private lateinit var broadcastReceiver: myDownloadBroadcast

    private fun lisenterDownload(){
        // 注册广播监听系统的下载完成事件。
        var intentFilter:IntentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        broadcastReceiver = myDownloadBroadcast()
        registerReceiver(broadcastReceiver, intentFilter);
    }

    inner class myDownloadBroadcast: BroadcastReceiver() {

        override fun onReceive(context:Context , intent:Intent ) {
            var ID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (ID == downloadId) {
                bt_analys.setText("分析下载")
                shareMusic()
            }
        }
    }

    private fun shareMusic() {
        if (saveFile==null||!saveFile?.exists()!!){
            Snackbar.make(window.decorView,"还没有出解析音乐文件！",Snackbar.LENGTH_LONG).show()
            return
        }
        var uri:Uri  = getURL(saveFile!!)
        var  shareintent:Intent = Intent()
        shareintent.setAction(Intent.ACTION_SEND);
        shareintent.setType("*/*");
//        shareintent.putExtra(Intent.EXTRA_STREAM, getURL(saveFile!!));
        val manager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        shareintent.putExtra(Intent.EXTRA_STREAM,manager.getUriForDownloadedFile(downloadId));
//        Log.i("Mainactivity","shareMusic URL = "+getURL(saveFile!!))

        startActivity(shareintent);
    }

    private fun getURL(saveFile: File): Uri {
        if (Build.VERSION.SDK_INT >= 24){
            return FileProvider.getUriForFile(context?.getApplicationContext(), "com.sll.skingwu.downmusicapp.fileprovider", saveFile);
        }else{
            return Uri.fromFile(saveFile)
        }

    }
    ;

    override fun onDestroy() {
        super.onDestroy()
        if (broadcastReceiver!=null)unregisterReceiver(broadcastReceiver)
    }




}
