package com.aliberkaygediko.anbean;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.aliberkaygediko.anbean.Fragments.HomeFragment;
import com.aliberkaygediko.anbean.Fragments.NotificationFragment;
import com.aliberkaygediko.anbean.Fragments.ProfileFragment;
import com.aliberkaygediko.anbean.Fragments.SearchFragment;
import com.aliberkaygediko.anbean.Service.ApiClient;
import com.aliberkaygediko.anbean.Service.ApiInterface;
import com.aliberkaygediko.anbean.Service.Example;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CHECK_SETTINGS = 111;
    TextView locationTV, temperature, textweather, textdesc;
    String cityName;
    Button hava;


    AppBarLayout top_bar;
    BottomNavigationView bottom_navigation;
    Fragment selectedfragment = null;
    private FirebaseAuth auth;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        bottom_navigation = findViewById(R.id.bottom_navigation);
        bottom_navigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);

        top_bar = findViewById(R.id.top_bar);
        locationTV = findViewById(R.id.location);

        temperature = findViewById(R.id.temp);
        textweather = findViewById(R.id.weatherDetail);
        textdesc = findViewById(R.id.weatherDescription);
        hava = findViewById(R.id.havaNe);


        Bundle intent = getIntent().getExtras();
        if (intent != null) {
            String publisher = intent.getString("publisherid");

            SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
            editor.putString("profileid", publisher);
            editor.apply();

            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new ProfileFragment()).commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new HomeFragment()).commit();
        }


    }


    public void showSettingAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("GPS setting!");
        alertDialog.setMessage("GPS is not enabled, Do you want to go to settings menu? ");
        alertDialog.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
                SettingsClient client = LocationServices.getSettingsClient(getApplicationContext());
                Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
                task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            havaNedirr();

                        }
                    }
                });

            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialog.show();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                    switch (item.getItemId()) {
                        case R.id.nav_home:
                            top_bar.setVisibility(View.VISIBLE);
                            selectedfragment = new HomeFragment();
                            break;
                        case R.id.nav_search:
                            top_bar.setVisibility(View.GONE);
                            selectedfragment = new SearchFragment();
                            break;
                        case R.id.nav_add:
                            top_bar.setVisibility(View.GONE);
                            selectedfragment = null;

                            startActivity(new Intent(MainActivity.this, PostActivity.class));

                            break;
                        case R.id.nav_heart:
                            top_bar.setVisibility(View.GONE);
                            selectedfragment = new NotificationFragment();
                            break;
                        case R.id.nav_profile:
                            top_bar.setVisibility(View.GONE);
                            SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
                            editor.putString("profileid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                            editor.apply();
                            selectedfragment = new ProfileFragment();
                            break;
                    }
                    if (selectedfragment != null) {
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                selectedfragment).commit();
                    }

                    return true;
                }
            };


    public String hereLocation(double lat, double lon) {

        String cityName = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(lat, lon, 10);
            if (addresses.size() > 0) {
                cityName = addresses.get(addresses.size() - 2).getAdminArea();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1000: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                    @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    try {

                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            showSettingAlert();
                        }
                        String city = hereLocation(location.getLatitude(), location.getLongitude());
                        locationTV.setVisibility(View.INVISIBLE);
                        locationTV.setText(city);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Not Found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Permisson not granted", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void getWeatherData(String name) {

        ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);

        Call<Example> call = apiInterface.getWeatherData(name);

        call.enqueue(new Callback<Example>() {
            @Override
            public void onResponse(Call<Example> call, Response<Example> response) {


                temperature.setText(response.body().getMain().getTemp() + "°C");
                textweather.setText(response.body().getWeatherList().get(0).getwMain());
                textdesc.setText(response.body().getWeatherList().get(0).getwDesc());

            }

            @Override
            public void onFailure(Call<Example> call, Throwable t) {

            }
        });

    }

    public void havaNedir(View view) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
            } else {
                showSettingAlert();
            }


        } else {

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                try {
                    locationTV.setVisibility(View.VISIBLE);
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    String city = hereLocation(location.getLatitude(), location.getLongitude());
                    //city="Turkey";
                    locationTV.setText(city);

                    if (city=="") {
                        locationTV.setText("Turkey");
                    }

                    double asd = location.getLatitude();
                    double asd2 = location.getLongitude();



                    getWeatherData(locationTV.getText().toString().trim());
                    hava.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Not Found GPS", Toast.LENGTH_SHORT).show();
                }
            } else {
                showSettingAlert();


            }


        }

    }

    public void havaNedirr() {


        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            try {
                locationTV.setVisibility(View.VISIBLE);
                @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                String city = hereLocation(location.getLatitude(), location.getLongitude());
                locationTV.setText(city);

                getWeatherData(locationTV.getText().toString().trim());
                hava.setVisibility(View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Not Found GPS2", Toast.LENGTH_SHORT).show();
            }
        }


        //hava.setVisibility(View.GONE);


    }

}