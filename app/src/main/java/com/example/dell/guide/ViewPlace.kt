package com.example.dell.guide

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.example.dell.guide.Model.PlaceDetail
import com.example.dell.guide.Retrofit.IGoogleAPIService
import com.example.dell.guide.URL.Common
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_view_place.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewPlace : AppCompatActivity() {

    internal lateinit var mService:IGoogleAPIService
    var mplace:PlaceDetail?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_place)

        mService=Common.googleApiService
        place_name.text=""
        place_address.text=""
        place_open_hour.text=""


        btn_view_direction.setOnClickListener {
            var viewDirections= Intent(this@ViewPlace,MapsActivity::class.java)
            viewDirections.putExtra("Direction",1)
            startActivity(viewDirections)

       }



        //photo
        if(Common.currentResult!!.photos!=null && Common.currentResult!!.photos!!.size>0)
        Picasso.with(this)
                .load(getPhotoOfPlace(Common.currentResult!!.photos!![0].photo_reference!!,1000))
                .into(photo)



        //rating
        if(Common.currentResult!!.rating!=null)
        {rating_bar.rating=Common.currentResult!!.rating.toFloat()}
        else
        {rating_bar.visibility=View.GONE}


        //open hours
        if(Common.currentResult!!.opening_hours!=null)
        {if (Common.currentResult!!.opening_hours!!.open_now==true)
        {place_open_hour.text="Open Now : Yes "}
        else{place_open_hour.text="Open Now :No "}}
        else
        {place_open_hour.visibility=View.GONE}


        //get address and name
        mService.getDetailPlace(getPlaceDetailUrl(Common.currentResult!!.place_id!!))
                .enqueue(object : retrofit2.Callback<PlaceDetail>{
                    override fun onFailure(call: Call<PlaceDetail>?, t: Throwable?) {
                        Toast.makeText(baseContext,""+t!!.message,Toast.LENGTH_SHORT).show()
                    }

                    override fun onResponse(call: Call<PlaceDetail>?, response: Response<PlaceDetail>?) {
                        mplace=response!!.body()
                        place_address.text=mplace!!.result!!.formatted_address
                        place_name.text=mplace!!.result!!.name
                    }

                })
    }

    private fun getPlaceDetailUrl(place_id: String): String {
        val url = StringBuilder("https://maps.googleapis.com/maps/api/place/details/json?")
        url.append("placeid=$place_id")
        url.append("&key=AIzaSyCyuJmNnafNVdx390P07u5X6JwNiZYySnI")
        return url.toString()

    }

    private fun getPhotoOfPlace(photo_reference: String, maxWidth: Int): String {
        val url = StringBuilder("https://maps.googleapis.com/maps/api/place/photo")
        url.append("?maxwidth=$maxWidth")
        url.append("&photoreference=$photo_reference")
        url.append("&key=AIzaSyCyuJmNnafNVdx390P07u5X6JwNiZYySnI")
        return url.toString()

    }
}
