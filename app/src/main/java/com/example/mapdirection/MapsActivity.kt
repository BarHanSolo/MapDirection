package com.example.mapdirection

import android.Manifest
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.lang.UCharacter.getDirection
import android.location.Geocoder
import android.location.Location
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.webkit.WebStorage
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

class MapsActivity : AppCompatActivity(), OnMapReadyCallback ,GoogleMap.OnMyLocationClickListener {
    override fun onMyLocationClick(location: Location) {
        var latitude=location.latitude
        var longitude=location.longitude
        var nLocation=LatLng(latitude,longitude)
        val geocoder = Geocoder(this)
        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
        val loc=addresses[0].getAddressLine(0)
        mMap.addMarker(MarkerOptions().position(nLocation).title("You are here $loc"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nLocation,15F))
    }
    private lateinit var mMap: GoogleMap
    var delta = LatLng(58.385254, 26.725064)
    var prevLocation = LatLng(58.385254, 26.725064)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            !== PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions( this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1 )
            return
        }
    }

    fun getCurrentLocation(): Location? {
        val location = TrackLocation(this).getCurrentLocation()
        return location
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMyLocationChangeListener(myLocationChangeListener);
        mMap.setMyLocationEnabled(true)
    }

    private val myLocationChangeListener = GoogleMap.OnMyLocationChangeListener { location ->
            val locationCoord = LatLng(location.latitude, location.longitude)
            val URL = getDirectionURL(locationCoord,delta)
            if (locationCoord!=prevLocation){
                mMap.clear()
                GetDirection(URL).execute()
                mMap.addMarker(MarkerOptions().position(locationCoord).title("My GPS Location"))
                mMap.addMarker(MarkerOptions().position(delta).title("Delta Centre"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationCoord, 15F))
                mMap.setMyLocationEnabled(true)
                prevLocation=locationCoord
            }
        }

    fun getDirectionURL(origin: LatLng, destination: LatLng) : String{
        val keyString = getString(R.string.google_maps_key2)
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&sensor=false&mode=driving&key=${keyString}"
    }

    private inner class GetDirection(val url : String) : AsyncTask<Void,Void,List<List<LatLng>>>(){
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            var client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            var data = response.body!!.string()
            Log.d("GoogleMap" , " data : $data")
            val result =  ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data,GoogleMapDTO::class.java)

                val path =  ArrayList<LatLng>()

                for (i in 0..(respObj.routes[0].legs[0].steps.size-1)){
                    val startLatLng = LatLng(respObj.routes[0].legs[0].steps[i].start_location.lat.toDouble()
                            ,respObj.routes[0].legs[0].steps[i].start_location.lng.toDouble())
                    path.add(startLatLng)
                    val endLatLng = LatLng(respObj.routes[0].legs[0].steps[i].end_location.lat.toDouble()
                            ,respObj.routes[0].legs[0].steps[i].end_location.lng.toDouble())
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices){
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.BLUE)
                lineoption.geodesic(true)
            }
            mMap.addPolyline(lineoption)
        }
    }

    public fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        return poly
    }
}
