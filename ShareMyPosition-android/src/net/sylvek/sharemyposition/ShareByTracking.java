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
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

    private static final String PARAMETER_POSITION = "pos";

    private static final String PARAMETER_UUID = "uuid";

    private static final String LOG = "ShareByTracking";

    private static final long INTERVAL = 15000L;

    private static final float SMALLEST_DISPLACEMENT = 50;

    private static final int PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private static final String UPDATE_LOCATION = ShareMyPosition.HOST + "service/update";

    static final String UUID = ".uuid";

    private final HttpParams params = new BasicHttpParams();

    private ConnectivityManager cm;

    private PowerManager pm;

    private WakeLock wl;

    protected static final ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {

        @Override
        public void onDisconnected()
        {
        }

        @Override
        public void onConnected(Bundle arg0)
        {
        }
    };

    protected static final OnConnectionFailedListener onConnectionFailedListener = new OnConnectionFailedListener() {

        @Override
        public void onConnectionFailed(ConnectionResult arg0)
        {
        }
    };

    public static class StopTracking extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent)
        {
            final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            final LocationClient locationClient = new LocationClient(context, connectionCallbacks, onConnectionFailedListener);
            locationClient.registerConnectionCallbacks(new ConnectionCallbacks() {

                @Override
                public void onDisconnected()
                {
                    Log.d(LOG, "StopTracking.onDisconnected");
                }

                @Override
                public void onConnected(Bundle arg0)
                {
                    final PendingIntent service = PendingIntent.getService(context, 0,
                            new Intent(context, ShareByTracking.class), PendingIntent.FLAG_UPDATE_CURRENT);
                    locationClient.removeLocationUpdates(service);
                    locationClient.disconnect();
                    notificationManager.cancel(R.string.app_name);
                    Log.d(LOG, "StopTracking.onConnected");
                }
            });

            locationClient.connect();
        }

    }

    public static class StartTracking extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent)
        {
            if (intent.getExtras().containsKey(UUID)) {
                final String uuid = intent.getStringExtra(UUID);
                final LocationClient locationClient = new LocationClient(context, connectionCallbacks, onConnectionFailedListener);
                locationClient.registerConnectionCallbacks(new ConnectionCallbacks() {

                    @Override
                    public void onDisconnected()
                    {
                        Log.d(LOG, "StartTracking.onDisconnected");
                    }

                    @Override
                    public void onConnected(Bundle arg0)
                    {
                        final LocationRequest locationRequest = LocationRequest.create()
                                .setSmallestDisplacement(SMALLEST_DISPLACEMENT)
                                .setInterval(INTERVAL)
                                .setPriority(PRIORITY);
                        final PendingIntent service = PendingIntent.getService(context, 0, new Intent(context,
                                ShareByTracking.class).putExtra(UUID, uuid), PendingIntent.FLAG_UPDATE_CURRENT);
                        locationClient.requestLocationUpdates(locationRequest, service);
                        locationClient.disconnect();
                        Log.d(LOG, "StartTracking.onConnected");
                    }
                });

                locationClient.connect();
            }
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
        Log.d(LOG, "share(" + uuid + "): " + location);
        final String position = location.getLatitude() + "," + location.getLongitude();
        Executors.newCachedThreadPool().execute(new Runnable() {

            @Override
            public void run()
            {
                try {
                    final HttpClient client = new DefaultHttpClient(params);
                    final HttpPost post = new HttpPost(UPDATE_LOCATION);
                    final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                    nameValuePairs.add(new BasicNameValuePair(PARAMETER_UUID, uuid));
                    nameValuePairs.add(new BasicNameValuePair(PARAMETER_POSITION, position));
                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    final HttpResponse response = client.execute(post);
                    if (200 != response.getStatusLine().getStatusCode()) {
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