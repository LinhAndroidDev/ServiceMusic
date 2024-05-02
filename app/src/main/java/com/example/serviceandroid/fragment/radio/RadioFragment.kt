package com.example.serviceandroid.fragment.radio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentRadioBinding
import com.example.serviceandroid.map.MapData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request


@Suppress("DEPRECATION")
class RadioFragment : BaseFragment<FragmentRadioBinding>(), OnMapReadyCallback {

    private var map: GoogleMap? = null
    private var currentLocation: Location? = null
    private var fusedLocation: FusedLocationProviderClient? = null

    // GeeksforGeeks coordinates
    private var originLatitude: Double = 28.5021359
    private var originLongitude: Double = 77.4054901

    // Coordinates of a park nearby
    private var destinationLatitude: Double = 28.5151087
    private var destinationLongitude: Double = 77.3932163

    companion object {
        const val LOCATION_PERMISSION_CODE = 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.title.text = "Radio"
        initMapView()
    }

    @SuppressLint("ServiceCast")
    private fun initMapView() {

        // Fetching API_KEY which we wrapped
        val ai: ApplicationInfo = requireActivity().packageManager
            .getApplicationInfo(requireActivity().packageName, PackageManager.GET_META_DATA)
        val value = ai.metaData["com.google.android.geo.API_KEY"]
        val apiKey = value.toString()

        // Initializing the Places API with the help of our API_KEY
        if (!Places.isInitialized()) {
            Places.initialize(requireActivity(), apiKey)
        }

//        fusedLocation = LocationServices.getFusedLocationProviderClient(requireActivity())
//        getLastLocation()
        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(
            this
        )

        binding.findLocal.setOnClickListener {
            (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync {
                map = it
                val originLocation = LatLng(originLatitude, originLongitude)
                map?.addMarker(MarkerOptions().position(originLocation))
                val destinationLocation = LatLng(destinationLatitude, destinationLongitude)
                map?.addMarker(MarkerOptions().position(destinationLocation))
                val urll = getDirectionURL(originLocation, destinationLocation, apiKey)
                GetDirection(urll).execute()
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(originLocation, 14F))
            }
        }
    }

    private fun getLastLocation() {
        activity?.let {
            if (ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_CODE
                )
                return
            }
            val task = fusedLocation?.lastLocation
            task?.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(
                        this
                    )
                }
            }
        }
    }

    /**
     * 21.001762, 105.822424
     */
    override fun onMapReady(googleMap: GoogleMap) {
//        this.map = googleMap
//        currentLocation?.let {
//            val sydney = LatLng(it.latitude, it.longitude)
//            map?.addMarker(
//                MarkerOptions()
//                    .position(sydney)
//                    .title("Marker in Sydney")
//            )
//            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15f), 2000, null)
//            map?.uiSettings?.apply {
//                isZoomControlsEnabled = true
//                isCompassEnabled = true
//            }
//        }

        map = googleMap
        val originLocation = LatLng(originLatitude, originLongitude)
        map?.clear()
        map?.addMarker(MarkerOptions().position(originLocation))
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(originLocation, 18F))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == LOCATION_PERMISSION_CODE) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                getLastLocation()
            } else {
                Toast.makeText(requireActivity(), "Location permission is denied, please", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDirectionURL(origin:LatLng, dest:LatLng, secret: String) : String{
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url : String) : AsyncTask<Void, Void, List<List<LatLng>>>(){
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()

            val result =  ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data,MapData::class.java)
                val path =  ArrayList<LatLng>()
                for (i in 0 until respObj.routes[0].legs[0].steps.size){
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            }catch (e:Exception){
                Log.e("Error", e.message.toString())
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices){
                lineoption.addAll(result[i])
                lineoption.width(30f)
                lineoption.color(Color.GREEN)
                lineoption.geodesic(true)
            }
            map?.addPolyline(lineoption)
        }
    }

    fun decodePolyline(encoded: String): List<LatLng> {
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
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
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

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentRadioBinding.inflate(inflater)
}