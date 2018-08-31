package com.example.dell.guide


import `in`.galaxyofandroid.spinerdialog.OnSpinerItemClick
import `in`.galaxyofandroid.spinerdialog.SpinnerDialog
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.example.dell.guide.URL.Common
import com.example.dell.guide.Retrofit.IGoogleAPIService
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import android.graphics.Color
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import com.example.dell.guide.Helper.DirectionJSONParser
import com.example.dell.guide.Model.*
import com.example.dell.guide.marker.PicassoMarker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import dmax.dialog.SpotsDialog
import org.json.JSONException
import org.json.JSONObject
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter
import org.sufficientlysecure.htmltextview.HtmlTextView
import java.util.ArrayList


class MapsActivity  : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap
    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()
    private lateinit var mLastLocation: Location
    private var mMarker: Marker? = null
    private var marker:PicassoMarker?=null
    lateinit var mCurrentMarker: Marker
    var polyLine:Polyline?=null
    var list_of_items = arrayListOf("accounting","airport", "amusement_park", "aquarium", "art_gallery", "atm", "bakery", "bank", "bar", "beauty_salon", "bicycle_store", "book_store",
            "bowling_alley", "bus_station", "cafe", "campground", "car_dealer", "car_rental", "car_repair", "car_wash", "casino","cemetery", "church", "city_hall", "clothing_store",
            "convenience_store", "courthouse", "dentist", "department_store", "doctor", "electrician", "electronics_store", "embassy", "fire_station", "florist", "funeral_home",
            "furniture_store", "gas_station", "gym", "hair_care", "hardware_store", "hindu_temple", "home_goods_store", "hospital", "insurance_agency", "jewelry_store", "laundry", "lawyer",
            "library", "liquor_store", "local_government_office", "locksmith", "lodging", "meal_delivery", "meal_takeaway", "mosque", "movie_rental", "movie_theater", "moving_company", "museum",
            "night_club", "painter", "park", "parking", "pet_store", "pharmacy", "physiotherapist", "plumber", "police", "post_office", "real_estate_agency", "restaurant", "roofing_contractor", "school",
            "shopping_mall", "spa", "stadium", "storage", "store", "subway_station", "supermarket", "synagogue", "taxi_stand", "train_station", "transit_station", "travel_agency","shoe_store", "veterinary_care",
            "zoo")
    var spinnerdialog:SpinnerDialog?=null


    //loaction
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    companion object {
        private const val MY_PERMISSION_CODE: Int = 1000
    }

    lateinit var mService: IGoogleAPIService
    lateinit var mService1: IGoogleAPIService
    var myRoutes:MyRoutes?=null




     var currentPlace: MyPlaces?=null
    internal lateinit var MarkerPoints: ArrayList<LatLng>

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        spinnerdialog = SpinnerDialog(this@MapsActivity, list_of_items, "Select or Search Service", R.style.DialogAnimations_SmileWindow, "Close")
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        MarkerPoints = ArrayList<LatLng>()
        //Init Service
        mService = Common.googleApiService
        mService1 = Common.googleApiServiceScalars
        btn_text_directions.setOnClickListener {
             val builder=AlertDialog.Builder(this@MapsActivity)
            val inflater = LayoutInflater.from(this@MapsActivity)
            val step_view= inflater.inflate(R.layout.step_layout,null)
            var txt_routes = step_view.findViewById(R.id.txt_routes) as HtmlTextView


            if(myRoutes != null)
            {


                    for (step in  myRoutes!!.routes!![0].legs!![0].steps!!)
              {

                  var stringBuilder = StringBuilder(step.html_instructions)
                  txt_routes.setHtml(stringBuilder.toString(), HtmlHttpImageGetter(txt_routes))
                  builder.setView(step_view)

              }
                builder.setNegativeButton("OK", DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()

                })

                builder.show()

            }



        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationPermission()) {
                buildLocationRequest();
                buildLocationCallback()
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            }
        } else {
            buildLocationRequest();
            buildLocationCallback()
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
        spinnerdialog!!.bindOnSpinerListener(OnSpinerItemClick { item, position ->
            Toast.makeText(this@MapsActivity,"showing "+list_of_items[position],Toast.LENGTH_SHORT).show()
            nearByPlace(list_of_items[position])

        })

        btnShow.setOnClickListener {
            spinnerdialog!!.showSpinerDialog()

        }

    }



    private fun nearByPlace(typePlace: String) {
        mMap.clear()

        //build url request mil location
        val url = getUrl(latitude, longitude, typePlace)

        mService.getNearbyPlaces(url)
                .enqueue(object : Callback<MyPlaces> {

                    override fun onResponse(call: Call<MyPlaces>?, response: Response<MyPlaces>?) {
                        currentPlace = response!!.body()!!

                        if (response!!.isSuccessful) {

                            for (i in 0 until response!!.body()!!.results!!.size) {
                                val googlePlace =response!!.body()!!.results!![i]
                                val markerOptions = MarkerOptions()
                                val lat = googlePlace.geometry!!.location!!.lat
                                val lng = googlePlace.geometry!!.location!!.lng
                                val latLng = LatLng(lat, lng)
                                val name = googlePlace.name
                                val url1=googlePlace.icon
                                markerOptions.position(latLng)
                                markerOptions.title(name)
                                markerOptions.snippet(i.toString())
                                marker= PicassoMarker(mMap.addMarker(markerOptions))
                                Picasso.with(this@MapsActivity)
                                        .load(url1)
                                        .into(marker)



                                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))

                            }
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(latitude, longitude)))
                            mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))

                        }


                    }

                    override fun onFailure(call: Call<MyPlaces>?, t: Throwable?) {
                        Toast.makeText(baseContext, "" + t!!.message, Toast.LENGTH_SHORT).show()

                    }

                })


    }



    private fun getUrl(latitude: Double, longitude: Double, typePlace: String): String {
        val googlePlaceUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?")
        googlePlaceUrl.append("location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=10000")
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&key=AIzaSyCyuJmNnafNVdx390P07u5X6JwNiZYySnI")
        Log.d("URL_DEBUG", googlePlaceUrl.toString())
        return googlePlaceUrl.toString()

    }


    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                mMap.clear()
                mLastLocation = p0!!.lastLocation
                if (mMarker != null) {
                    mMarker!!.remove()
                }
                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude
                val latLng = LatLng(latitude, longitude)

                mMarker = mMap.addMarker(MarkerOptions().position(latLng).title(getAddress(latLng)))


                //move camera
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(12f))



            }

        }
    }
    private fun buildLocationCallback1() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                mMap.clear()
                mLastLocation = p0!!.lastLocation
            }
        }
    }


    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f


    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ), MapsActivity.MY_PERMISSION_CODE)
            else
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ),MapsActivity.MY_PERMISSION_CODE)
            return false
        } else
            return true
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MapsActivity.MY_PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        if (checkLocationPermission()) {
                            buildLocationRequest();
                            buildLocationCallback()
                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                            mMap!!.isMyLocationEnabled = true
                        }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

        }


    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    private fun getAddress(latLng: LatLng): String {

        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        var addressText = ""

        try {

            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            addressText = addresses.get(0).locality

        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return addressText
    }


    private fun drawPath(mLastLocation: EndLocation?, location: com.example.dell.guide.Model.Location) {
        if(polyLine !=null)
            polyLine!!.remove() //delete old path

        val origin = StringBuilder(mLastLocation!!.lat.toString())
                .append(",")
                .append(mLastLocation!!.lng.toString())
                .toString()

        val destination = StringBuilder(location.lat.toString())
                .append(",")
                .append(location.lng.toString())
                .toString()

        mService1.getDirections(origin,destination )
                .enqueue(object:Callback<String>{
                    override fun onFailure(call: Call<String>?, t: Throwable?) {
                        Log.d("",t!!.message)
                    }

                    override fun onResponse(call: Call<String>?, response: Response<String>?) {
                        ParserTask().execute(response!!.body()!!.toString())

                    }

                })

    }
    private fun drawPath1(mLastLocation: Location?, location: com.example.dell.guide.Model.Location) {
        if(polyLine !=null)
            polyLine!!.remove() //delete old path

        val origin = StringBuilder(mLastLocation!!.latitude.toString())
                .append(",")
                .append(mLastLocation!!.longitude.toString())
                .toString()

        val destination = StringBuilder(location.lat.toString())
                .append(",")
                .append(location.lng.toString())
                .toString()

        mService1.getDirections(origin,destination )
                .enqueue(object:Callback<String>{
                    override fun onFailure(call: Call<String>?, t: Throwable?) {
                        Log.d("",t!!.message)
                    }

                    override fun onResponse(call: Call<String>?, response: Response<String>?) {
                        myRoutes = Gson().fromJson(response!!.body()!!.toString(), object:TypeToken<MyRoutes>() {}.getType())
                       //Log.e("json",response!!.body()!!.toString())
                        ParserTask().execute(response!!.body()!!.toString())

                    }

                })

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)

                mMap!!.isMyLocationEnabled = true
        } else
            mMap!!.isMyLocationEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true


        mMap.setOnMarkerClickListener { marker ->

        if(marker.snippet!=null)
        {Common.currentResult = currentPlace!!.results!![Integer.parseInt(marker.snippet)]
            startActivity(Intent(this@MapsActivity, ViewPlace::class.java))}

            true
        }
        var dir:Int=intent.getIntExtra("Direction",0)
       // var rout:Int=intent.getIntExtra("Route",0)
        if (dir == 1  ) {

            mMap.clear()
            btn_text_directions.visibility = View.VISIBLE


            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                mLastLocation = location



                var markerOptions = MarkerOptions()
                        .position(LatLng(mLastLocation.latitude, mLastLocation.longitude))
                        .title("You're here")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))

                mCurrentMarker = mMap!!.addMarker(markerOptions)
                //move camera
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(mLastLocation.latitude, mLastLocation.longitude)))
                //mMap!!.moveCamera(CameraUpdateFactory.newLatLng(LatLng(step.startlocation!!.lat, step.startlocation!!.lng)))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(12f))

                //add destination marker
                var destinationLatlng = LatLng(Common.currentResult!!.geometry!!.location!!.lat.toDouble(), Common.currentResult!!.geometry!!.location!!.lng.toDouble())
                mMap.addMarker(MarkerOptions().position(destinationLatlng)
                        .title(Common.currentResult!!.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                drawPath1(mLastLocation, Common.currentResult!!.geometry!!.location!!)
                if (myRoutes != null ) {
                    for (step in myRoutes!!.routes!![0].legs!![0].steps!!) {
                         buildLocationCallback1()
                        if (mLastLocation.latitude==step.end_location!!.lat && mLastLocation.longitude==step.end_location!!.lng )
                        //get path
                        {drawPath(step.end_location, Common.currentResult!!.geometry!!.location!!)}


                    }

                }
            }
        }



    }
    inner class ParserTask:AsyncTask<String,Int,List<List<HashMap<String,String>>>>()  {
        internal val waitingDialog: AlertDialog = SpotsDialog(this@MapsActivity)
        override fun onPreExecute() {
            super.onPreExecute()
            waitingDialog.show()
            waitingDialog.setMessage("Waiting..")
        }

        override fun doInBackground(vararg params: String?): List<List<HashMap<String, String>>> {
            val jsonObject : JSONObject
            var routes: List<List<HashMap<String, String>>>?=null
            try {
                jsonObject= JSONObject(params[0])
                val parser = DirectionJSONParser()
                routes=parser.parse(jsonObject)
            } catch (e: JSONException)
            {
                e.printStackTrace()
            }
            return routes!!

        }

        override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
            super.onPostExecute(result)
            var points:ArrayList<LatLng>
            var polylineoptions:PolylineOptions?=null
            for (i in result!!.indices)
            {
                points= ArrayList()
                polylineoptions= PolylineOptions()
                val path=result[i]
                for (j in path.indices)
                {
                    val point = path[j]
                    val lat=point["lat"]!!.toDouble()
                    val lng=point["lng"]!!.toDouble()
                    val position = LatLng(lat,lng)
                    points.add(position)
                }
                polylineoptions.addAll(points)
                polylineoptions.width(12f)
                polylineoptions.color(Color.RED)
                polylineoptions.geodesic(true)


            }
            if(polylineoptions!=null)
                polyLine = mMap!!.addPolyline(polylineoptions)
            waitingDialog.dismiss()
        }

    }



}


