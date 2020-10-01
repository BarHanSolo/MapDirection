package com.example.mapdirection

import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat


class TrackLocation(var mContext: Context) : LocationListener {
    // Flag for GPS status
    var isGPSEnabled = false
    // Flag for network status
    var isNetworkEnabled = false
    // Flag for GPS status
    var canGetLocation = false
    var location: Location? = null  // Location
    private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 10 // 10 meters
    // The minimum time between updates in milliseconds
    private val MIN_TIME_BW_UPDATES = (1000 * 60 * 1).toLong() // 1 minute
    // Declaring a Location Manager
    protected lateinit var locationManager: LocationManager

    // init is run when object instance is created
    init {
        locationManager = mContext.getSystemService(LOCATION_SERVICE) as LocationManager
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    fun getCurrentLocation(): Location? {

        //check permission
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("TAG ","No permission Granted")
            Toast.makeText(mContext, "Missing location permissions", Toast.LENGTH_LONG).show()
        } else {

            if (isNetworkEnabled) {

                this.canGetLocation = true
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                )
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (isGPSEnabled) {

                this.canGetLocation = true
                // If GPS enabled, get latitude/longitude using GPS Services
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                )

                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            }
        }

        return location
    }

    fun canWeGetLocation(): Boolean {
        return this.canGetLocation
    }

    override fun onLocationChanged(location: Location) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

}