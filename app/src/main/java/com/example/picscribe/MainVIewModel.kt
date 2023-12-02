package com.example.picscribe

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.SocketTimeoutException

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _bitmaps = MutableStateFlow<List<Bitmap>>(emptyList())

    private val _caption = MutableStateFlow<String>("")
    val caption = _caption.asStateFlow()
    private val _error = MutableStateFlow<String>("")
    val error = _error.asStateFlow()

    fun onTakePhoto(bitmap: Bitmap) {
        _bitmaps.value += bitmap

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val client = OkHttpClient()
        val requestBody: RequestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), byteArray)
        val request: Request = Request.Builder()
            .url("https://api-inference.huggingface.co/models/nlpconnect/vit-gpt2-image-captioning")
            .addHeader("Authorization", "Bearer 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")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                if (e is SocketTimeoutException) {
                    viewModelScope.launch(Dispatchers.Main) {
                        _error.value = "Timeout error. Please wait...!"
                    }
                }
                Log.e("Camera", "Couldn't send photo: ", e)
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    val responseString = response.body?.string()
                    val jsonResponseArray = JSONArray(responseString)
                    val firstObject = jsonResponseArray.getJSONObject(0)
                    val caption = firstObject.getString("generated_text")
                    _caption.value = caption
                }
            }
        })
    }

}
