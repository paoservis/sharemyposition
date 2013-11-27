/*
 * Copyright (C) 2013  Sylvain Maucourt (smaucourt@gmail.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 *
 */
package net.sylvek.sharemyposition;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.util.concurrent.Executors;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * @author sylvek
 * 
 */
public class ShareByTracking extends IntentService {

    private static final String LOG = "ShareByTracking";

    private static final long INTERVAL = 15000L;

    private static final float SMALLEST_DISPLACEMENT = 50;

    private static final int PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private static final String UPDATE_LOCATION = "http://sharemyposition.appspot.com/update.jsp";

    private static final String UUID = ".uuid";

    private final HttpParams params = new BasicHttpParams();

    private ConnectivityManager cm;

    private PowerManager pm;

    private WakeLock wl;

    public static class StopService extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            context.stopService(new Intent(context, ServiceHandler.class));
        }

    }

    public static class ServiceHandler extends Service implements ConnectionCallbacks, OnConnectionFailedListener {

        private LocationClient mLocationClient;

        private PendingIntent service;

        private String uuid;

        @Override
        public IBinder onBind(Intent arg0)
        {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId)
        {
            mLocationClient = new LocationClient(this, this, this);
            mLocationClient.connect();
            uuid = intent.getStringExtra(UUID);
            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy()
        {
            if (mLocationClient.isConnected()) {
                mLocationClient.removeLocationUpdates(service);
                final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(R.string.app_name);
                mLocationClient.disconnect();
                Log.d(LOG, "disconnect");
            }
            super.onDestroy();
        }

        @Override
        public void onConnectionFailed(ConnectionResult arg0)
        {
            Log.d(LOG, "onConnectionFailed");
        }

        @Override
        public void onConnected(Bundle arg0)
        {
            Log.d(LOG, "onConnected");
            final LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);
            locationRequest.setInterval(INTERVAL);
            locationRequest.setPriority(PRIORITY);
            final ServiceHandler context = ServiceHandler.this;
            final Intent intent = new Intent(context, ShareByTracking.class);
            intent.putExtra(UUID, uuid);
            service = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            mLocationClient.requestLocationUpdates(locationRequest, service);
        }

        @Override
        public void onDisconnected()
        {
            Log.d(LOG, "onDisconnected");
        }
    }

    public ShareByTracking()
    {
        super(LOG);
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        HttpProtocolParams.setUserAgent(params, "Android/" + Build.DISPLAY + "/version:" + ShareMyPosition.VERSION);
    }

    public void share(final Location location, final String uuid)
    {
        Log.d(LOG, "share: " + location);
        final String position = location.getLatitude() + "," + location.getLongitude();
        Executors.newCachedThreadPool().execute(new Runnable() {

            @Override
            public void run()
            {
                try {
                    final HttpClient client = new DefaultHttpClient(params);
                    final HttpGet get = new HttpGet(UPDATE_LOCATION + "?uuid=" + uuid + "&pos=" + position);
                    final HttpResponse response = client.execute(get);
                    if (response.getStatusLine().getStatusCode() != 200) {
                        Log.w(LOG, "status code is not 200");
                    }
                } catch (final Exception e) {
                    Log.e(LOG, e.getMessage());
                }
            }
        });
    }

    @Override
    public void onDestroy()
    {
        Log.d(LOG, "onDestroy");
        synchronized (this.wl) {
            if (this.wl.isHeld()) {
                this.wl.release();
            }
        }
    }

    public static void startService(final Context context, final String uuid)
    {
        final Intent intent = new Intent(context, ServiceHandler.class);
        intent.putExtra(UUID, uuid);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(final Intent intent)
    {
        Log.d(LOG, "onHandleIntent");
        this.wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG);
        synchronized (this.wl) {
            this.wl.setReferenceCounted(false);
            this.wl.acquire();
        }
        if (intent.hasExtra(LocationClient.KEY_LOCATION_CHANGED) && intent.hasExtra(UUID)) {
            final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                final Bundle extras = intent.getExtras();
                final Location location = (Location) (extras.get(LocationClient.KEY_LOCATION_CHANGED));
                share(location, extras.getString(UUID));
            }
        }

    }
}