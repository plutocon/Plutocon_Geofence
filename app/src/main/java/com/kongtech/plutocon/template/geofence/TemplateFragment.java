package com.kongtech.plutocon.template.geofence;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.kongtech.plutocon.sdk.Plutocon;
import com.kongtech.plutocon.template.PlutoconListActivity;
import com.kongtech.plutocon.template.view.AttrItemView;

public class TemplateFragment extends Fragment implements View.OnClickListener, OnMapReadyCallback {

    public static final int REQUEST_PLUTOCON = 1;
    public static final int REQUEST_LOCATION = 2;

    private AttrItemView aivTargetName;
    private AttrItemView aivTargetAddress;
    private AttrItemView aivLatitude;
    private AttrItemView aivLongitude;
    private AttrItemView aivUUID;

    private Plutocon targetPlutocon;

    private GoogleMap map;

    public static Fragment newInstance(Context context) {
        TemplateFragment f = new TemplateFragment();
        return f;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_template, container, false);

        aivTargetName = (AttrItemView) view.findViewById(R.id.aivTargetName);
        aivTargetAddress = (AttrItemView) view.findViewById(R.id.aivTargetAddress);
        aivLatitude = (AttrItemView) view.findViewById(R.id.aivLatitude);
        aivLatitude.setAttrValueColor(0xffff0000);
        aivLongitude = (AttrItemView) view.findViewById(R.id.aivLongitude);
        aivLongitude.setAttrValueColor(0xff0000ff);
        aivUUID = (AttrItemView) view.findViewById(R.id.aivUUID);
        aivUUID.setValueTextSize(12);
        aivTargetName.setOnClickListener(this);
        view.findViewById(R.id.btn).setOnClickListener(this);
        ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.aivTargetName:
                if (checkPermission())
                    startActivityForResult(new Intent(getActivity(), PlutoconListActivity.class), REQUEST_PLUTOCON);
                break;
            case R.id.btn:
                if (targetPlutocon != null) {
                    LatLng latlng = new LatLng(targetPlutocon.getLatitude(), targetPlutocon.getLongitude());
                    LatLngBounds.Builder lngBuilder = new LatLngBounds.Builder().include(latlng);
                    lngBuilder.include(SphericalUtil.computeOffset(latlng, 709, 45));
                    lngBuilder.include(SphericalUtil.computeOffset(latlng, 709, 215));

                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    builder.setLatLngBounds(lngBuilder.build());
                    try {
                        startActivityForResult(builder.build(getActivity()), REQUEST_LOCATION);
                    } catch (GooglePlayServicesRepairableException e) {
                        e.printStackTrace();
                    } catch (GooglePlayServicesNotAvailableException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PLUTOCON:
                if (resultCode == 1) {
                    targetPlutocon = (Plutocon) data.getParcelableExtra("PLUTOCON");
                    aivTargetName.setValue(targetPlutocon.getName());
                    aivTargetAddress.setValue(targetPlutocon.getMacAddress());
                    setLocation(targetPlutocon.getUuid(), targetPlutocon.getLatitude(), targetPlutocon.getLongitude());
                }
                break;
            case REQUEST_LOCATION:
                Place place = PlacePicker.getPlace(getContext(), data);

                GeofenceSetter.Builder builder = new GeofenceSetter.Builder(getContext());
                builder.setLatLng(place.getLatLng())
                        .setPlutocon(targetPlutocon)
                        .setSettingCompletedListener(new GeofenceSetter.SettingCompletedListener() {
                            @Override
                            public void OnCompleted(ParcelUuid uuid, double latitude, double longitude) {
                                setLocation(uuid, latitude, longitude);
                            }
                        })
                        .build()
                        .apply();
                break;
        }
    }

    private void setLocation(final ParcelUuid uuid, final double latitude, final double longitude) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                aivLatitude.setValue(latitude + "");
                aivLongitude.setValue(longitude + "");

                setUUID(uuid.toString());

                LatLng latLng = new LatLng(latitude, longitude);
                map.clear();
                map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(targetPlutocon.getName()));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
            }
        });
    }

    private boolean checkPermission() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        if ((mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return false;
            }

            LocationManager lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            } catch (Exception ex) {
            }

            if (!gps_enabled) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return false;
            }
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (map != null)
            return;

        System.gc();
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.402136, 127.107018), 14));
    }

    public void setUUID(String uuid) {
        SpannableString styledMenuTitle = new SpannableString(uuid);
        styledMenuTitle.setSpan(new ForegroundColorSpan(Color.parseColor("#fff00000")), 9, 9 + 12, 0);
        styledMenuTitle.setSpan(new ForegroundColorSpan(Color.parseColor("#ff0000ff")), 21, 21 + 12, 0);
        aivUUID.setValue(styledMenuTitle);
    }
}

