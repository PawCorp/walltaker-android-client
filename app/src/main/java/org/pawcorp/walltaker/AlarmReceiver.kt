package org.pawcorp.walltaker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi


class AlarmReceiver : BroadcastReceiver() {
    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        val `in` = Intent(context, WalltakerService::class.java)
        context.startService(`in`)
        setAlarm(context)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun setAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_MUTABLE)
//        assert(am != null)
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            (System.currentTimeMillis() / 1000L + 10L) * 1000L,
            pi
        ) //Next alarm in 10s
    }
}


//class AlarmReceiver : BroadcastReceiver() {
//    override fun onReceive(p0: Context?, p1: Intent?) {
//        var url = "http://172.16.10.155:8000/links/8.json"
//        val client = OkHttpClient()
//        val request = Request.Builder()
//            .url(url)
//            .addHeader("Android-Client", "v1.0.0")
//            .addHeader("User-Agent", "walltaker-android-client/v1.0.0")
//            .get()
//            .build()
//        client.newCall(request)
//            .enqueue(object: Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    Log.e("GetDataError", "Hmm, something went wrong.")
//                    Log.e("GetDataError", e.toString())
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    val body = response.body?.string()
//                    Log.i("DEBUG", body.toString())
//                }
//
//            })
//
//        Log.d("MyAlarm", "Alarm is just fired")
//        Toast.makeText(p0, "Alarm fired", Toast.LENGTH_SHORT).show() //tmp
//    }
//
//}