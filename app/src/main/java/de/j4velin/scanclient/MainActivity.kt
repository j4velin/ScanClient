package de.j4velin.scanclient

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import java.net.URL


class MainActivity : Activity() {

    var currentPage = 1
    var totalPages = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)
        val view = findViewById<EditText>(R.id.ip)
        view.setText(preferences.getString("ip", "192.168.178.24"))
        view.setOnClickListener {
            val currentIp = it as EditText
            val newIp = EditText(this)
            newIp.inputType = InputType.TYPE_CLASS_PHONE
            newIp.text = currentIp.text
            AlertDialog.Builder(this).setTitle("New IP").setView(newIp)
                .setPositiveButton(
                    android.R.string.ok
                ) { dialog, _ ->
                    currentIp.text = newIp.text
                    preferences.edit().putString("ip", newIp.text.toString()).apply()
                    dialog?.dismiss()
                }
                .create().show()
        }
    }

    override fun onResume() {
        super.onResume()
        val pages = findViewById<EditText>(R.id.pages)
        pages.requestFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
            pages,
            InputMethodManager.SHOW_FORCED
        )
        pages.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    scan(findViewById(R.id.scan))
                    return true
                }
                return false
            }
        })
    }

    fun scan(view: View) {
        view.isVisible = false
        val progressDialog: ProgressDialog?
        totalPages = findViewById<EditText>(R.id.pages).text.toString().toInt()
        if (totalPages > 1) {
            progressDialog = null
            currentPage = 0 // first scan happen when clicking on the next button
            findViewById<Button>(R.id.next).apply {
                isVisible = true
                text = "$currentPage / $totalPages"
            }
            Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show()
        } else {
            currentPage = 1
            progressDialog = ProgressDialog.show(this, "Scanning", "Please wait...", true, false)
        }
        Thread {
            val result = try {
                URL("http://${findViewById<EditText>(R.id.ip).text}:8080/${findViewById<EditText>(R.id.pages).text}").readText()
            } catch (e: Exception) {
                e.printStackTrace()
                "Error trying to scan"
            }
            runOnUiThread {
                progressDialog?.dismiss()
                Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                if (currentPage == totalPages) view.isVisible = true
            }
        }.start()
    }

    fun next(view: View) {
        view.isVisible = false
        Toast.makeText(this, "next", Toast.LENGTH_SHORT).show()
        Thread {
            val result = try {
                URL("http://${findViewById<EditText>(R.id.ip).text}:8080/next").readText()
            } catch (e: Exception) {
                e.printStackTrace()
                "Error trying to scan"
            }
            currentPage++
            runOnUiThread {
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
                (view as Button).text = "$currentPage / $totalPages"
                if (currentPage == totalPages) {
                    findViewById<View>(R.id.scan).isVisible = true
                } else {
                    view.isVisible = true
                }
            }
        }.start()
    }
}