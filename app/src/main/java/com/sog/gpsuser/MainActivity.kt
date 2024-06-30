package com.sog.gpsuser

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if(!isAccessibilityServiceEnabled()){
            var intentAccesibiliti = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intentAccesibiliti)
        }

        var intent = Intent(this, GeoCoordsServs::class.java)
        startService(intent)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION), 1020)
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            Handler(Looper.getMainLooper()).post({
                UsuarioApp()
            })
        },0,1,TimeUnit.SECONDS)
    }

    fun UsuarioApp(){
        try {
            val MyPreffer = getSharedPreferences("MyPreffergmt", Context.MODE_PRIVATE)
            var usuarioapp = MyPreffer.getString("operador","")
            var usuarioappN = MyPreffer.getString("usuario","")
            val operado: TextView = findViewById(R.id.operador)
            val usuario : EditText = findViewById(R.id.Usuario)
            operado.setText("Oprador: ${usuarioapp.toString()}")
            if(usuarioappN.toString() != "")usuario.setText(usuarioappN.toString())
        }catch (ex:Exception){}

    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains("com.gmt.gpsuser/com.gmt.user.GeoCoordsServs") ?: false
    }

    fun GuardarGeoCoords(view: View) {
        try {
            val usuario : EditText = findViewById(R.id.Usuario)
            var mUsuario: String = usuario.text.toString()
            if(mUsuario != ""){
                val MyPreffer = getSharedPreferences("MyPreffergmt", Context.MODE_PRIVATE)
                val editor = MyPreffer.edit()
                editor.putString("usuario",mUsuario)
                editor.apply()
            }
        }catch(ex: Exception){}

    }
}