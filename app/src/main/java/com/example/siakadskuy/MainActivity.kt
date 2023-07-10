package com.example.siakadskuy

import android.content.Intent
import org.json.JSONObject
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.view.MotionEvent
import android.content.Context
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.inputmethod.InputMethodManager
import android.view.Menu
import android.widget.PopupMenu
import android.text.InputType
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.text.Editable
import android.util.Log

import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_NAME = "productName"
        const val EXTRA_PRODUCT_PASS = "productPass"
    }

    private lateinit var nameText: EditText
    private lateinit var passText: EditText
    private lateinit var codeText: EditText
    private lateinit var gambarsend: ImageButton
    private lateinit var sharedPreferences: SharedPreferences
    private var productName: String? = null
    private var productPass: String? = null
    private lateinit var webView: WebView
    private var loginnCodeExecuted: Boolean = false

    private fun clearWebViewData() {
        webView.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearWebViewData()
    }

    private fun fetchData() {

        // Mendapatkan nilai dari EditText
        val name = nameText.text.toString()
        val password = passText.text.toString()

        // Load bookmarks from SharedPreferences and update the bookmark list and status map
        val bookmarkedList = loadBookmarks()
        val bookmarkStatusMap = bookmarkedList.associateBy({ it.name }, { true }).toMutableMap()

        val bookmarkedProduct = BookmarkedProduct(name, password)
        bookmarkedList.add(bookmarkedProduct)
        bookmarkStatusMap[name] = true

        // Save the updated bookmarked items to SharedPreferences
        saveBookmarks(bookmarkedList)

        clearWebViewData()

        // Load the login page
        webView.loadUrl("https://akademik.its.ac.id/data_nilaipersem.php")

        // Wait for the page to finish loading
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {

                    // Enter NRP
                    webView.evaluateJavascript(
                        "document.getElementById('username').value = '$name';",
                        null
                    )
                    // Click continue button
                    webView.evaluateJavascript(
                        "document.getElementById('continue').click();",
                        null
                    )
                    // Enter Password
                    webView.evaluateJavascript(
                        "document.getElementById('password').value = '$password';",
                        null
                    )
                    // Submit the form by clicking the login button
                    webView.evaluateJavascript(
                        "document.getElementById('login').click();",
                        null
                    )
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                if (!loginnCodeExecuted) {
                    loginnCodeExecuted = true
                    // Get cookies
                    val cookieManager = CookieManager.getInstance()
                    val cookies = cookieManager.getCookie(url)

                    // Save cookies to SharedPreferences
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putString("cookies", cookies).apply()

                    // Redirect to desired URL
                    webView.loadUrl("https://akademik.its.ac.id/data_nilaipersem.php")
                    clearWebViewData()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar1)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.setVerticalScrollBarEnabled(true)
        webView.setHorizontalScrollBarEnabled(true)
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        webView.setScrollbarFadingEnabled(false)
        WebView.setWebContentsDebuggingEnabled(true)

        // Initialize the SharedPreferences object
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        nameText = findViewById(R.id.nameText)
        passText = findViewById(R.id.passText)
        gambarsend = findViewById(R.id.gambarsend)

        // Set the input type to password
        passText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Get the root view of the activity
        val rootView = findViewById<View>(android.R.id.content)

        // Set an OnTouchListener on the root view
        rootView.setOnTouchListener { v, event ->
            // Check if the user tapped outside of the EditText
            if (event.action == MotionEvent.ACTION_DOWN &&
                currentFocus is EditText &&
                !v.equals(currentFocus)
            ) {
                // Hide the keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            }
            false
        }

        // Retrieve product details from intent extras if available
        if (intent.hasExtra(EXTRA_PRODUCT_NAME) && intent.hasExtra(EXTRA_PRODUCT_PASS)) {
            productName = intent.getStringExtra(EXTRA_PRODUCT_NAME)
            productPass = intent.getStringExtra(EXTRA_PRODUCT_PASS)
        }

        // Set product details to EditText if the values are not null
        if (productName != null) {
            nameText.setText(productName)
        }
        if (productPass != null) {
            passText.setText(productPass)
        }

        gambarsend.setOnClickListener {
            fetchData()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_dropdown -> {
                val anchorView = findViewById<View>(R.id.menu_item_dropdown)

                val popupMenu = PopupMenu(this, anchorView)
                popupMenu.menuInflater.inflate(R.menu.dropdown_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {

                        R.id.menu_item_2 -> {
                            startActivity(Intent(this, BookmarksActivity::class.java))
                            true
                        }

                        else -> false
                    }
                }
                popupMenu.show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    private fun loadBookmarks(): ArrayList<BookmarkedProduct> {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val bookmarksJson = sharedPreferences.getString("bookmarks", null)
        return if (bookmarksJson != null) {
            Gson().fromJson(bookmarksJson, object : TypeToken<ArrayList<BookmarkedProduct>>() {}.type)
        } else {
            ArrayList()
        }
    }

    private fun saveBookmarks(bookmarkedList: MutableList<BookmarkedProduct>) {
        val jsonString = Gson().toJson(bookmarkedList)

        // Load the existing bookmarks from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val existingJsonString = sharedPreferences.getString("bookmarks", null)

        // If there are existing bookmarks, update them with any new ones
        if (existingJsonString != null) {
            val existingBookmarks = Gson().fromJson(existingJsonString, Array<BookmarkedProduct>::class.java).toMutableList()
            for (newBookmark in bookmarkedList) {
                val existingBookmarkIndex = existingBookmarks.indexOfFirst { it.name == newBookmark.name }
                if (existingBookmarkIndex >= 0) {
                    val existingBookmark = existingBookmarks[existingBookmarkIndex]
                    val updatedBookmark = BookmarkedProduct(
                        existingBookmark.name,
                        newBookmark.pass
                    )
                    existingBookmarks[existingBookmarkIndex] = updatedBookmark
                } else {
                    existingBookmarks.add(newBookmark)
                }
            }
            val updatedJsonString = Gson().toJson(existingBookmarks)
            sharedPreferences.edit().putString("bookmarks", updatedJsonString).apply()
        } else {
            // Otherwise, just save the new bookmarks to SharedPreferences
            sharedPreferences.edit().putString("bookmarks", jsonString).apply()
        }
    }
}