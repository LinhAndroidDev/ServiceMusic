package com.example.serviceandroid.fragment.radio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseFragment
import com.example.serviceandroid.databinding.FragmentRadioBinding
import com.example.serviceandroid.map.GMapV2Direction
import com.example.serviceandroid.map.GetDirectionsAsyncTask
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions


class RadioFragment : BaseFragment<FragmentRadioBinding>(), OnMapReadyCallback {

    private var map: GoogleMap? = null
    private var currentLocation: Location? = null
    private var fusedLocation: FusedLocationProviderClient? = null
    private var AMSTERDAM: LatLng? = null
    private var PARIS: LatLng? = null
    private var newPolyline: Polyline? = null
    private var latlngBounds: LatLngBounds? = null

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
        fusedLocation = LocationServices.getFusedLocationProviderClient(requireActivity())
        getLastLocation()
        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(
            this
        )
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
        this.map = googleMap
        currentLocation?.let {
            val sydney = LatLng(it.latitude, it.longitude)
            map?.addMarker(
                MarkerOptions()
                    .position(sydney)
                    .title("Marker in Sydney")
            )
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15f), 2000, null)
            map?.uiSettings?.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
            }
        }

//        map = googleMap
//
//        // Add a marker in Sydney and move the camera
//
//        // Add a marker in Sydney and move the camera
//        AMSTERDAM = LatLng(21.0169598, 105.8114176)
//        AMSTERDAM?.let {
//            map?.addMarker(MarkerOptions().position(it).title("Marker in Sydney"))
//            map?.moveCamera(CameraUpdateFactory.newLatLng(it))
//
//            PARIS = LatLng(20.9889196, 105.8635858)
//
//            val md = GMapV2Direction()
//            // OnGetDataCompleteListener completeListener = null;
//            // OnGetDataCompleteListener completeListener = null;
//            findDirections(
//                it.latitude, it.longitude,
//                PARIS!!.latitude, PARIS!!.longitude,
//                GMapV2Direction.MODE_DRIVING
//            )
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == LOCATION_PERMISSION_CODE) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                Toast.makeText(requireActivity(), "Location permission is denied, please", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getFragmentBinding(inflater: LayoutInflater) =
        FragmentRadioBinding.inflate(inflater)

    fun handleGetDirectionsResult(directionPoints: ArrayList<LatLng?>) {
        val rectLine = PolylineOptions().width(5f).color(
            Color.RED
        )
        for (i in directionPoints.indices) {
            rectLine.add(directionPoints[i])
        }
        if (newPolyline != null) {
            newPolyline?.remove()
        }
        newPolyline = map?.addPolyline(rectLine)
        latlngBounds = createLatLngBoundsObject(AMSTERDAM, PARIS)
        map?.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                latlngBounds!!, 400, 400, 150
            )
        )
    }

    private fun createLatLngBoundsObject(
        firstLocation: LatLng?,
        secondLocation: LatLng?
    ): LatLngBounds? {
        if (firstLocation != null && secondLocation != null) {
            val builder = LatLngBounds.Builder()
            builder.include(firstLocation).include(secondLocation)
            return builder.build()
        }
        return null
    }

    private fun findDirections(
        fromPositionDoubleLat: Double,
        fromPositionDoubleLong: Double, toPositionDoubleLat: Double,
        toPositionDoubleLong: Double, mode: String
    ) {
        val map: MutableMap<String, String> = HashMap()
        map[GetDirectionsAsyncTask.USER_CURRENT_LAT] = fromPositionDoubleLat.toString()
        map[GetDirectionsAsyncTask.USER_CURRENT_LONG] = fromPositionDoubleLong.toString()
        map[GetDirectionsAsyncTask.DESTINATION_LAT] = toPositionDoubleLat.toString()
        map[GetDirectionsAsyncTask.DESTINATION_LONG] = toPositionDoubleLong.toString()
        map[GetDirectionsAsyncTask.DIRECTIONS_MODE] = mode
        val asyncTask = GetDirectionsAsyncTask(this)
        asyncTask.execute(map)
        // asyncTask.cancel(true);
    }
}