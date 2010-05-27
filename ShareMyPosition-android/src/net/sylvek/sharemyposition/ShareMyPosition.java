/*
 * Copyright (C) 2010  Sylvain Maucourt (smaucourt@gmail.com)
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Executors;

public class ShareMyPosition extends Activity implements LocationListener {

    private static final String VERSION = "1.0.6";

    private static final String LOG = "ShareMyPosition";

    private static final String HOST = "http://sharemyposition.appspot.com/";

    private static final String SHORTY_URI = HOST + "service/create?url=";

    private static final String STATIC_WEB_MAP = HOST + "static.jsp";

    private static final String SHARED_WEP_MAP = HOST + "sharedmap.jsp?pos=";

    private final static int PROVIDERS_DLG = Menu.FIRST;

    private final static int PROGRESS_DLG = PROVIDERS_DLG + 1;

    private final static int MAP_DLG = PROGRESS_DLG + 1;

    private LocationManager locationManager;

    private Geocoder gc;

    private HttpParams params = new BasicHttpParams();

    private WakeLock lock;

    private ToggleButton insideMode;

    private TextView progressText;

    private Location location;

    private WebView sharedMap;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        HttpProtocolParams.setUserAgent(params, "Android/" + Build.DISPLAY + "/version:" + VERSION);

        gc = new Geocoder(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        initWakeLock();
    }

    private void initWakeLock()
    {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        lock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "sharemyposition.lock");
    }

    private void performLocation(boolean forceNetwork)
    {
        locationManager.removeUpdates(ShareMyPosition.this);
        List<String> providers = locationManager.getProviders(true);
        if (providerAvailable(providers)) {
            showDialog(PROGRESS_DLG);
            progressText.setText(R.string.progression_desc);

            boolean containsGPS = providers.contains(LocationManager.GPS_PROVIDER);
            boolean containsNetwork = providers.contains(LocationManager.NETWORK_PROVIDER);

            if ((containsGPS && !forceNetwork) || (containsGPS && !containsNetwork)) {
                Log.d(LOG, "gps selected");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, this);
                insideMode.setEnabled(containsNetwork);
            } else if (containsNetwork) {
                Log.d(LOG, "network selected");
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 5, this);
                insideMode.setEnabled(false);
            } else {
                Log.w(LOG, "no provided found (GPS or NETWORK)");
                finish();
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        lock.acquire();
        performLocation(false);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        lock.release();
        locationManager.removeUpdates(this);
    }

    private boolean providerAvailable(List<String> providers)
    {
        if (providers.isEmpty()) {
            showDialog(PROVIDERS_DLG);
            return false;
        }

        return true;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id) {
        default:
            super.onPrepareDialog(id, dialog);
            break;
        case MAP_DLG:
            if (location != null) {
                final StringBuilder pos = new StringBuilder().append(location.getLatitude()).append(",").append(
                        location.getLongitude()).append("&size=300x155");
                Log.d("preview.url", pos.toString());
                sharedMap.loadUrl(SHARED_WEP_MAP + pos.toString());
            } else {
                Log.w("preview.url", "location is null");
            }
            break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id) {
        default:
            return super.onCreateDialog(id);
        case MAP_DLG:
            final View sharedMapView = LayoutInflater.from(this).inflate(R.layout.sharedmap, null);
            this.sharedMap = (WebView) sharedMapView.findViewById(R.id.sharedmap_webview);
            sharedMap.setAlwaysDrawnWithCacheEnabled(false);
            sharedMap.setKeepScreenOn(true);
            return new AlertDialog.Builder(this).setTitle(R.string.app_name).setView(sharedMapView).setOnCancelListener(
                    new OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface arg0)
                        {
                            finish();
                        }
                    }).setNeutralButton(R.string.exit, new OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1)
                {
                    finish();
                }
            }).setPositiveButton(R.string.share_it, new OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1)
                {
                    final boolean isGeocodeAddress = ((CheckBox) sharedMapView.findViewById(R.id.add_address_location)).isChecked();
                    final boolean isUrlShortening = ((CheckBox) sharedMapView.findViewById(R.id.add_url_location)).isChecked();
                    final EditText body = (EditText) sharedMapView.findViewById(R.id.body);

                    Executors.newCachedThreadPool().execute(new Runnable() {

                        @Override
                        public void run()
                        {
                            final StringBuilder msg = new StringBuilder(body.getText().toString());
                            if (isGeocodeAddress) {
                                final String address = getAddress(location);
                                if (!address.equals("")) {
                                    msg.append(", ").append(address);
                                }
                            }
                            if (isUrlShortening) {
                                msg.append(", ").append(getLocationUrl(location));
                            }
                            Intent t = new Intent(Intent.ACTION_SEND);
                            t.setType("text/plain");
                            t.addCategory(Intent.CATEGORY_DEFAULT);
                            t.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject));
                            t.putExtra(Intent.EXTRA_TEXT, msg.toString());
                            Intent share = Intent.createChooser(t, getString(R.string.app_name));
                            startActivity(share);
                            finish();
                        }

                    });
                }
            })
                    .setNegativeButton(R.string.retry, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1)
                        {
                            performLocation(false);
                        }
                    })
                    .create();
        case PROGRESS_DLG:
            final View progress = LayoutInflater.from(this).inflate(R.layout.progress, null);

            progressText = (TextView) progress.findViewById(R.id.progress);

            insideMode = (ToggleButton) progress.findViewById(R.id.inside_mode);

            insideMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    performLocation(isChecked);
                }
            });

            return new AlertDialog.Builder(this).setTitle(getText(R.string.app_name))
                    .setView(progress)
                    .setCancelable(true)
                    .setOnCancelListener(new OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface dialog)
                        {
                            finish();
                        }
                    })
                    .create();
        case PROVIDERS_DLG:
            return new AlertDialog.Builder(this).setTitle(R.string.app_name).setCancelable(false).setIcon(
                    android.R.drawable.ic_menu_help).setMessage(R.string.providers_needed).setNegativeButton(android.R.string.no,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            finish();
                        }

                    }).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Intent gpsProperty = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(gpsProperty);
                }
            }).create();
        }
    }

    @Override
    public void onLocationChanged(final Location location)
    {
        Log.d(LOG, "location changed: " + location.toString());
        locationManager.removeUpdates(this);

        this.location = location;

        progressText.setText(R.string.location_changed);

        showDialog(MAP_DLG);
    }

    public String getLocationUrl(Location location)
    {
        String url = getCurrentStaticLocationUrl(location);
        try {
            url = getTinyLink(url);
        } catch (Exception e) {
            Log.e(LOG, "tinyLink don't work: " + url);
        }

        return url;
    }

    public String getCurrentStaticLocationUrl(Location location)
    {
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();

        StringBuilder uri = new StringBuilder(STATIC_WEB_MAP).append("?pos=").append(latitude).append(",").append(longitude);
        return uri.toString();
    }

    public String getAddress(Location location)
    {
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();

        List<Address> address = null;
        try {
            address = gc.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            Log.e(LOG, "unable to get address", e);
            return "";
        }

        if (address == null || address.size() == 0) {
            Log.w(LOG, "unable to parse address");
            return "";
        }

        Address a = address.get(0);

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.getMaxAddressLineIndex(); i++) {
            b.append(a.getAddressLine(i));
            if (i < a.getMaxAddressLineIndex()) {
                b.append(" ");
            }
        }

        return b.toString();
    }

    public String getTinyLink(String url) throws ClientProtocolException, IOException, JSONException
    {
        HttpClient client = new DefaultHttpClient(params);
        HttpGet get = new HttpGet(SHORTY_URI + URLEncoder.encode(url));
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            return EntityUtils.toString(response.getEntity());
        }

        return url;
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        performLocation(false);
    }

    @Override
    public void onProviderEnabled(String provider)
    {
        performLocation(false);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        performLocation(false);
    }
}