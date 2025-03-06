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
import android.widget.EditText
import android.widget.Toast
import java.net.URL


class MainActivity : Activity() {

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
        val progressDialog = ProgressDialog.show(this, "Scanning", "Please wait...", true, false)
        Thread {
            val result = try {
                URL("http://${findViewById<EditText>(R.id.ip).text}:8080/${findViewById<EditText>(R.id.pages).text}").readText()
            } catch (e: Exception) {
                e.printStackTrace()
                "Error trying to scan"
            }
            runOnUiThread {
                progressDialog.dismiss()
                Toast.makeText(this, result, Toast.LENGTH_LONG).show()
            }
        }.start()
    }
}