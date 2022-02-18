package org.pawcorp.walltaker

import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.*
import android.util.Log
import android.widget.Toast
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import okhttp3.*
import okio.IOException

class WalltakerService : Service() {
    // This method run only one time. At the first time of service created and running
    override fun onCreate() {
        Picasso.setSingletonInstance(Picasso.Builder(this).build())
        val thread = HandlerThread(
            "ServiceStartArguments",
            Process.THREAD_PRIORITY_BACKGROUND
        )
        thread.start()
        Log.d("onCreate()", "After service created")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val context: Context = this
        val sharedPreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val userDataUrl = sharedPreferences.getString("USER_DATA_URL", null)
        val lastPostUrl = sharedPreferences.getString("LAST_POST_URL", null)
        val lastPostThumbUrl = sharedPreferences.getString("LAST_POST_THUMB_URL", null)
        val lastSetBy = sharedPreferences.getString("LAST_SET_BY", null)
        assert(userDataUrl != null)
        if (userDataUrl != null) {
            Log.d("WALLTAKER_LOG", userDataUrl)
        }
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
//                        Log.i("DEBUG", body.toString())
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
                            if (userData.post_url != lastPostUrl || setBy != lastSetBy) {
                                val editor = sharedPreferences.edit()
                                editor.apply {
                                    putString("LAST_POST_URL", userData.post_url)
                                    putString("LAST_POST_THUMB_URL", userData.post_thumbnail_url)
                                    putString("LAST_SET_BY", setBy)
                                }.apply()
                                Log.d("WALLTAKER_LOG", "NEW WALLPAPER FOUND")
//                                Toast.makeText(context, "NEW WALLPAPER FOUND", Toast.LENGTH_SHORT).show() //tmp
                                Toast.makeText(context, "$setBy set your wallpaper~", Toast.LENGTH_LONG).show()
                                Picasso.get().load(userData.post_url).resize(720, 1280).centerCrop()
                                    .onlyScaleDown() // the image will only be resized if it's bigger
                                    .into(object : com.squareup.picasso.Target{
                                        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                            val wpm : WallpaperManager = context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
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

                            } else {
                                Log.d("WALLTAKER_LOG", "NO NEW WALLPAPER FOUND")
//                                Toast.makeText(context, "NO NEW WALLPAPER FOUND", Toast.LENGTH_SHORT).show() //tmp
                            }
                        })

                    }

                })
        }

//        Toast.makeText(this, "Alarm fired", Toast.LENGTH_SHORT).show() //tmp
//        return START_STICKY
        return  START_STICKY_COMPATIBILITY // account for null or something
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding
        return null
    }
}

class UserData( val ind: Int, val expires: String, val user_id: Int, val terms: String, val blacklist: String, val post_url: String, val post_thumbnail_url: String, val post_description : String, val created_at : String, val updated_at: String, val set_by: String?, val url: String ) {}