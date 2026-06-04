package com.example.speechtotext // đổi thành package name của bạn

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.Manifest

class MainActivity : AppCompatActivity() {

    // Khai báo các biến tương ứng với widget trong XML
    private lateinit var btnSpeak: Button
    private lateinit var tvOriginal: TextView
    private lateinit var tvTranslated: TextView
    private lateinit var spinnerLanguage: Spinner

    // Mã request permission (đặt số bất kỳ, dùng để nhận kết quả xin quyền)
    private val REQUEST_RECORD_AUDIO = 101
    // Mã request speech (dùng để nhận kết quả từ màn hình speech recognition)
    private val REQUEST_SPEECH = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kết nối biến với widget trong XML thông qua ID
        btnSpeak = findViewById(R.id.btnSpeak)
        tvOriginal = findViewById(R.id.tvOriginal)
        tvTranslated = findViewById(R.id.tvTranslated)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)

        // Gắn danh sách ngôn ngữ vào Spinner
        val adapter = ArrayAdapter.createFromResource(
                this,
                R.array.languages,          // array tên đẹp
                android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter

        // Xử lý khi nhấn nút ghi âm
        btnSpeak.setOnClickListener {
            checkPermissionAndSpeak()
        }
    }

    // Kiểm tra permission trước khi ghi âm
    // Bản chất: Android 6+ yêu cầu xin quyền lúc runtime, không chỉ trong manifest
    private fun checkPermissionAndSpeak() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Chưa có quyền → xin quyền, kết quả trả về onRequestPermissionsResult
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO
            )
        } else {
            // Đã có quyền → bắt đầu nghe
            startSpeechRecognition()
        }
    }

    // Kết quả xin quyền trả về đây
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO &&
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, "Cần quyền microphone!", Toast.LENGTH_SHORT).show()
        }
    }

    // Bắt đầu nhận diện giọng nói
    // Bản chất: Android có sẵn SpeechRecognizer, ta dùng Intent để gọi nó
    // Giống như "nhờ" app khác làm việc rồi trả kết quả về
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN") // nhận tiếng Việt
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói gì đó...")
        }
        startActivityForResult(intent, REQUEST_SPEECH)
    }

    // Nhận kết quả từ Speech Recognition trả về
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: return  // lấy kết quả đầu tiên (độ chính xác cao nhất)

                    tvOriginal.text = spokenText  // hiển thị text gốc
            translateText(spokenText)     // gọi API dịch
        }
    }

    // Gọi API dịch (dùng LibreTranslate public instance)
    // Bản chất: gửi HTTP POST request với text và ngôn ngữ đích, nhận về bản dịch
    private fun translateText(text: String) {
        val langCodes = resources.getStringArray(R.array.language_codes)
        val targetLang = langCodes[spinnerLanguage.selectedItemPosition]

        tvTranslated.text = "Đang dịch..."

        val client = OkHttpClient()

        // MyMemory dùng GET, encode text để tránh lỗi ký tự đặc biệt
        val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
        val url = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=vi|$targetLang"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    tvTranslated.text = "Lỗi kết nối: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                runOnUiThread {
                    try {
                        // MyMemory trả về: {"responseData": {"translatedText": "..."}, ...}
                        val json = JSONObject(body ?: "")
                        val translatedText = json
                            .getJSONObject("responseData")
                            .getString("translatedText")
                        tvTranslated.text = translatedText
                    } catch (e: Exception) {
                        tvTranslated.text = "Lỗi: ${e.message}"
                    }
                }
            }
        })
    }
}