package com.sog.gpsuser

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class GeoCoordsServs : AccessibilityService() {
    private var intervaloejecucion = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latlong = ""
    private var myusuario : String = ""

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

    override fun onServiceConnected() {
        super.onServiceConnected()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            Handler(Looper.getMainLooper()).post({
                if(intervaloejecucion == 20) {
                    getCoordsUser()
                }
                intervaloejecucion++
            })
        },0,1,TimeUnit.SECONDS)
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
}