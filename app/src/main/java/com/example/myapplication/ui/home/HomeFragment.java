package com.example.myapplication.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentHomeBinding;
import com.example.myapplication.models.LocationUpdateRequest;
import com.example.myapplication.models.RouteModel;
import com.example.myapplication.models.UserLocationResponse;
import com.example.myapplication.models.WeatherResponse;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.network.WeatherApiClient;
import com.example.myapplication.network.WeatherService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int REQUEST_CODE = 101;
    private static final long LOCATION_INTERVAL = 30 * 1000;
    private Handler locationHandler = new Handler(Looper.getMainLooper());
    private Runnable locationRunnable;

    private boolean movedToLocationYet = false;
    private List<Marker> nearbyUserMarkers = new ArrayList<>();

    ImageView iv_weather_icon;
    TextView tv_weather_discription_and_temp, tv_humidity, tv_wind_speed, tv_weather_precipitation;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        requireActivity().setTitle("TourTrack");

        tv_weather_discription_and_temp = binding.tvWeatherDiscriptionAndTemp;
        tv_humidity = binding.tvWeatherHumidity;
        tv_wind_speed = binding.tvWeatherWind;
        tv_weather_precipitation = binding.tvWeatherPrecipitation;
        iv_weather_icon = binding.ivWeatherIcon;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                map = googleMap;

                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                    return;
                }

                map.setMyLocationEnabled(true);

                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (!movedToLocationYet) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            movedToLocationYet = true;
                        }
                        map.addMarker(new MarkerOptions().position(latLng).title("You are here"));

                        sendLocationToServer(location);
                        fetchWeather(location.getLatitude(), location.getLongitude());
                        fetchNearbyUsers(location);
                        fetchAndDisplayRoutes();
                    }
                });
            });
        }

        startLocationUpdates();
        return binding.getRoot();
    }

    private void startLocationUpdates() {
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            sendLocationToServer(location);
                            fetchNearbyUsers(location);
                        }
                    });
                }
                locationHandler.postDelayed(this, LOCATION_INTERVAL);
            }
        };
        locationHandler.post(locationRunnable);
    }

    private void fetchNearbyUsers(Location currentLocation) {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId == null) return;

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getNearbyUsers(userId).enqueue(new Callback<List<UserLocationResponse>>() {
            @Override
            public void onResponse(Call<List<UserLocationResponse>> call, Response<List<UserLocationResponse>> response) {
                if (response.isSuccessful() && response.body() != null && map != null) {
                    for (Marker marker : nearbyUserMarkers) {
                        marker.remove();
                    }
                    nearbyUserMarkers.clear();

                    LatLng myLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    map.addMarker(new MarkerOptions().position(myLatLng).title("You are here"));

                    for (UserLocationResponse user : response.body()) {
                        LatLng userLatLng = new LatLng(user.getLat(), user.getLng());
                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(userLatLng)
                                .title("Nearby user")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                        nearbyUserMarkers.add(marker);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<UserLocationResponse>> call, Throwable t) {}
        });
    }

    private void fetchAndDisplayRoutes() {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getAllRoutes().enqueue(new Callback<List<RouteModel>>() {
            @Override
            public void onResponse(Call<List<RouteModel>> call, Response<List<RouteModel>> response) {
                if (response.isSuccessful() && response.body() != null && map != null) {
                    for (RouteModel route : response.body()) {
                        LatLng position = new LatLng(route.latitude, route.longitude);
                        map.addMarker(new MarkerOptions()
                                .position(position)
                                .title(route.name));
                    }
                }
            }

            @Override
            public void onFailure(Call<List<RouteModel>> call, Throwable t) {}
        });
    }

    private void fetchWeather(double lat, double lon) {
        String apiKey = "2ac4b8424a6eec7145c42467fda68ddf";
        String units = "metric";
        String lang = "he";

        WeatherService weatherService = WeatherApiClient.getClient();
        Call<WeatherResponse> call = weatherService.getWeatherByLocation(lat, lon, apiKey, units, lang);
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    String description = weather.getWeather().get(0).getDescription();
                    double temp = weather.getMain().getTemp();
                    int humidity = weather.getMain().getHumidity();
                    double wind = weather.getWind().getSpeed();
                    double rain = (weather.getRain() != null) ? weather.getRain().getOneHour() : 0.0;
                    int conditionId = weather.getWeather().get(0).getId();

                    getWeatherIconResource(conditionId);
                    requireActivity().runOnUiThread(() -> {
                        tv_weather_discription_and_temp.setText(description + " , " + temp + "°C");
                        tv_humidity.setText("Humidity: " + humidity + "%");
                        tv_wind_speed.setText("Wind: " + wind + " Km/h");
                        tv_weather_precipitation.setText("Precipitation: " + rain + "%");
                    });
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {}
        });
    }

    private void getWeatherIconResource(int conditionId) {
        if (conditionId >= 200 && conditionId <= 232) {
            iv_weather_icon.setImageResource(R.drawable.thunder);
        } else if (conditionId >= 300 && conditionId <= 321) {
            iv_weather_icon.setImageResource(R.drawable.drizzle);
        } else if (conditionId >= 500 && conditionId <= 531) {
            iv_weather_icon.setImageResource(R.drawable.rain);
        } else if (conditionId >= 600 && conditionId <= 622) {
            iv_weather_icon.setImageResource(R.drawable.snowing);
        } else if (conditionId >= 701 && conditionId <= 781) {
            iv_weather_icon.setImageResource(R.drawable.fog);
        } else if (conditionId == 800) {
            iv_weather_icon.setImageResource(R.drawable.ic_sun);
        } else if (conditionId >= 801 && conditionId <= 804) {
            iv_weather_icon.setImageResource(R.drawable.cloud);
        } else {
            iv_weather_icon.setImageResource(R.drawable.cloud);
        }
    }

    private void sendLocationToServer(Location location) {
        SharedPreferences prefs = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId == null) return;

        double lat = location.getLatitude();
        double lng = location.getLongitude();

        LocationUpdateRequest request = new LocationUpdateRequest(lat, lng);
        ApiService apiService = RetrofitClient.getApiService();

        apiService.updateLocation(userId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {}

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (map != null && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        locationHandler.removeCallbacks(locationRunnable);
        binding = null;
    }
}
