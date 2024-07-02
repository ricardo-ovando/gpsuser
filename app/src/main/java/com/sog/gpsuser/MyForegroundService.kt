package com.sog.gpsuser

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.TimeUnit
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.util.Timer
import java.util.TimerTask

class MyForegroundService : Service() {
    private var intervaloejecucion = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latlong = ""
    private var myusuario : String = ""
    private val time = Timer()

    private val timerTask = object : TimerTask(){
        override fun run() {
            if(intervaloejecucion == 20){
                getCoordsUser()
            }
            intervaloejecucion++
        }
    }
    private lateinit var notificationManager: NotificationManager
    private var notificationId = 1

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(NotificationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, createNotification(""))

        // Realiza tu tarea en segundo plano aquí
        time.schedule(timerTask,0,1000)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GRUASGMT - GPS USER")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.logui)
            .setContentIntent(pendingIntent)
            .build()
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
                DriverManager.getConnection("jdbc:mysql://sisadminweb.ddns.net:3306/gmt_main?useSSL=false","gmt_admin","Onifled.01").let { connection ->
                    val stmt: Statement = connection.createStatement()
                    stmt.executeUpdate("update sisadmin_employee set emp_coords = '$latlong' where emp_id = $myusuario")

                    val rs : ResultSet = stmt.executeQuery("select emp_fullname from sisadmin_employee where emp_id = $myusuario")
                    rs.first()
                    if(rs.getRow() > 0) {
                        var nombreop = rs.getString("emp_fullname").toString()
                        val MyPreffer = getSharedPreferences("MyPreffergmt", Context.MODE_PRIVATE)
                        val editor = MyPreffer.edit()
                        editor.putString("operador", nombreop)
                        editor.apply()
                    }

                    val rs1 : ResultSet = stmt.executeQuery("select date(entry_contact_time) as entry_contact_time from sisadmin_entry where entry_operador_id = $myusuario and st < 2")
                    rs1.first()
                    if(rs1.getRow() > 0) {
                        if(rs1.getString("entry_contact_time") == "1900-01-01") {
                            val updatedContent = "TIENES SERVICIOS ASIGNADOS"
                            val updatedNotification = createNotification(updatedContent)
                            notificationManager.notify(notificationId, updatedNotification)
                        }
                    }
                    return true
                }
            }catch (ex:Exception){
                return false
            }
            return true
        }

    }

}
