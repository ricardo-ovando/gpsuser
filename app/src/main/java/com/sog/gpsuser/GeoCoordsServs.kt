package com.sog.gpsuser

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GeoCoordsServs : AccessibilityService() {
    private var intervaloejecucion = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latlong = ""
    private var myusuario : String = ""
    private lateinit var wakeLock: PowerManager.WakeLock
    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }
    private val ONGOING_NOTIFICATION_ID = 12345

    val time = Timer()
    val timerTask =  object: TimerTask() {
        override fun run() {
            if(intervaloejecucion == 20) {
                getCoordsUser()
            }
            intervaloejecucion++
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            try {
            }catch (ex:Exception){
            }
        }
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Your app is running in the background")
            .setSmallIcon(R.drawable.logui)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        return START_STICKY
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag")

        // Adquiere el WakeLock si necesitas mantener la CPU despierta para tareas específicas
        wakeLock.acquire()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        time.schedule(timerTask,0,1000)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    fun getCoordsUser(){
        intervaloejecucion = 0
        val MyPreffer = getSharedPreferences("MyPreffergmt", Context.MODE_PRIVATE)
        myusuario = MyPreffer.getString("usuario", "").toString()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if(myusuario.toString() != ""){

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if(location != null){
                        latlong = "${location.latitude},${location.longitude}"
                        MiGeoCoordsServs().execute()
                    }
                }.addOnFailureListener { exeption ->
                    latlong = "0.0,0.0"
                }
            }
        }
    }

    private inner class MiGeoCoordsServs : AsyncTask<Void, Void, Boolean>(){
        override fun doInBackground(vararg params: Void?): Boolean {
            try {
                DriverManager.getConnection("jdbc:mysql://sisadminweb.ddns.net:3306/gmt_main?useSSL=false","gmt_admin","Onifled.01").let {connection ->
                    val stmt: Statement = connection.createStatement()
                    stmt.executeUpdate("update sisadmin_employee set emp_coords = '$latlong' where emp_id = $myusuario")

                    val rs : ResultSet = stmt.executeQuery("select emp_fullname from sisadmin_employee where emp_id = $myusuario")
                    rs.first()
                    var nombreop = rs.getString("emp_fullname").toString()
                    val MyPreffer = getSharedPreferences("MyPreffergmt", Context.MODE_PRIVATE)
                    val editor = MyPreffer.edit()
                    editor.putString("operador", nombreop)
                    editor.apply()
                    return true
                }
            }catch (ex:Exception){
                return false
            }
            return true
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}



