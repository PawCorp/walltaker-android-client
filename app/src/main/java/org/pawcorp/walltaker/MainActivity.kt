package org.pawcorp.walltaker

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import okhttp3.*
import okio.IOException

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadData()
        val updateButton = findViewById<Button>(R.id.buttonUpdate)
        updateButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            val linkId = findViewById<EditText>(R.id.editTextTextLinkID)
            val enteredId = linkId.text.toString()
            val editor = sharedPreferences.edit()
            editor.apply {
                putString("USER_ID", enteredId)
                putString("USER_DATA_URL", "https://walltaker.joi.how/links/$enteredId.json")
            }.apply()
            initScreen()
            Toast.makeText(this, "Saved and Started!", Toast.LENGTH_SHORT).show()
            setAlarm(System.currentTimeMillis())
        }
    }

    private fun initScreen() {
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val userDataUrl = sharedPreferences.getString("USER_DATA_URL", "")
        val client = OkHttpClient()
        val request = userDataUrl?.let {
            Request.Builder()
                .url(it)
                .addHeader("Android-Client", "v1.0.0")
                .addHeader("User-Agent", "walltaker-android-client/v1.0.0")
                .get()
                .build()
        }
        if (request != null) {
            client.newCall(request)
                .enqueue(object: Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("GetDataError", "Hmm, something went wrong.")
                        Log.e("GetDataError", e.toString())
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        val gson = GsonBuilder().create()
                        val userData = gson.fromJson(body, UserData::class.java)
                        if (userData.post_url == null) {
                            return
                        }
                        Log.d("WALLTAKER_LOG", userData.post_thumbnail_url)
                        Log.d("WALLTAKER_LOG", userData.post_url)
                        var setBy = ""
                        setBy = userData.set_by ?: "anonymous"
                        Log.d("WALLTAKER_LOG", setBy)
                        val mHandler = Handler(Looper.getMainLooper())
                        mHandler.post(Runnable {
                            val imgPreview = findViewById<ImageView>(R.id.previewImageView)
                            val setByTxt = findViewById<TextView>(R.id.textViewSetByMsg)
                            setByTxt.text = "Set by $setBy"
                            Picasso.get().load(userData.post_thumbnail_url).into(imgPreview)
                            Picasso.get().load(userData.post_url).resize(720, 1280).centerCrop()
                                .onlyScaleDown() // the image will only be resized if it's bigger
                                .into(object : com.squareup.picasso.Target{
                                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                        val wpm : WallpaperManager = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
                                        wpm.setBitmap(bitmap)
                                    }

                                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                        println("whoopsie!")
                                        println(e)
                                    }

                                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                                        println("Setting wallpaper!")
                                    }

                                })
                        })

                    }

                })
        }
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val savedId = sharedPreferences.getString("USER_ID", null)
        val setBy = sharedPreferences.getString("LAST_SET_BY", null)
        val linkId = findViewById<EditText>(R.id.editTextTextLinkID)
        linkId.setText(savedId)
        val setByTxt = findViewById<TextView>(R.id.textViewSetByMsg)
        if (setBy != null) {
            setByTxt.text = "Set by $setBy"
        }
        val lastPostThumbUrl = sharedPreferences.getString("LAST_POST_THUMB_URL", null)
        if (lastPostThumbUrl != null) {
            val mHandler = Handler(Looper.getMainLooper())
            mHandler.post(Runnable {
                val imgPreview = findViewById<ImageView>(R.id.previewImageView)
                Picasso.get().load(lastPostThumbUrl).into(imgPreview)
            })
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setAlarm(currentTimeMillis: Long) {
        // get alarm manager
        val am = getSystemService(ALARM_SERVICE) as AlarmManager
        //creating a new intent specifying the broadcast receiver
        val i = Intent(this, AlarmReceiver::class.java)
        //creating a pending intent using the intent
        val pi = PendingIntent.getBroadcast(this, 0, i, FLAG_MUTABLE)
        //setting the repeating alarm that will be fired every 10 seconds
        assert(am != null)
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            (System.currentTimeMillis() / 1000L + 10L) * 1000L,
            pi
        ) //Next alarm in 10s
    }

    fun setWp(wpUrl: String) {
        Picasso.get().load(wpUrl).resize(720, 1280).centerCrop()
            .onlyScaleDown() // the image will only be resized if it's bigger
            .into(object : com.squareup.picasso.Target{
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    val wpm : WallpaperManager = getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
                    wpm.setBitmap(bitmap)
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    println("whoopsie!")
                    println(e)
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    println("Setting wallpaper!")
                }

            })
    }

    fun refresh(view: View) {
        Toast.makeText(this, "Refresh Clicked", Toast.LENGTH_SHORT).show()
        initScreen()
    }

    fun onClickBtnStop(view: View) {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, FLAG_MUTABLE)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Stopped!", Toast.LENGTH_SHORT).show()
    }

}