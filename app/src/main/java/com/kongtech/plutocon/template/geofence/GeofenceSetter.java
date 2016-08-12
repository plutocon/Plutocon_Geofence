package com.kongtech.plutocon.template.geofence;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.ParcelUuid;

import com.google.android.gms.maps.model.LatLng;
import com.kongtech.plutocon.sdk.Plutocon;
import com.kongtech.plutocon.sdk.connection.PlutoconConnection;
import com.kongtech.plutocon.sdk.connection.PlutoconOperator;

import java.util.Set;

public class GeofenceSetter {

    private Context context;
    private LatLng latLng;
    private Plutocon plutocon;
    private SettingCompletedListener settingCompletedListener;

    public void apply() {

        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("기기 연결 중 입니다.");
        progressDialog.show();

        final PlutoconConnection plutoconConnection = new PlutoconConnection(context, plutocon);

        plutoconConnection.connect(new PlutoconConnection.OnConnectionStateChangeCallback() {
            @Override
            public void onConnectionStateDisconnected() {

            }

            @Override
            public void onConnectionStateConnected() {

                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.setMessage("정보 변경 중 입니다.");
                    }
                });

                plutoconConnection.editor()
                        .setGeofence(latLng.latitude, latLng.longitude)
                        .setOnOperationCompleteCallback(new PlutoconOperator.OnOperationCompleteCallback() {
                            @Override
                            public void onOperationComplete(BluetoothGattCharacteristic characteristic, boolean isLast) {
                                if (settingCompletedListener != null) {
                                    settingCompletedListener
                                            .OnCompleted(plutoconConnection.getUuid(), plutoconConnection.getLatitude(), plutoconConnection.getLongitude());
                                }
                                progressDialog.dismiss();
                                plutoconConnection.disconnect();
                            }
                        })
                        .commit();
            }
        });
    }

    public static class Builder {

        private Context context;
        private LatLng latLng;
        private Plutocon plutocon;
        private SettingCompletedListener settingCompletedListener;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setLatLng(LatLng latLng) {
            this.latLng = latLng;
            return this;
        }

        public Builder setPlutocon(Plutocon plutocon) {
            this.plutocon = plutocon;
            return this;
        }

        public Builder setSettingCompletedListener(SettingCompletedListener settingCompletedListener) {
            this.settingCompletedListener = settingCompletedListener;
            return this;
        }

        public GeofenceSetter build() {
            GeofenceSetter setter = new GeofenceSetter();
            setter.context = context;
            setter.latLng = latLng;
            setter.plutocon = plutocon;
            setter.settingCompletedListener = settingCompletedListener;
            return setter;
        }
    }

    public interface SettingCompletedListener {
        void OnCompleted(ParcelUuid uuid, double latitude, double longitude);
    }
}
