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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.Executors;

public class ShareMyPosition extends Activity implements LocationListener {
    
    private static final String LOG = "ShareMyPosition";

    private static final String HOST = "http://sharemyposition.appspot.com/";

    private static final String SHORTY_URI = HOST + "service/create?url=";

    private static final String STATIC_WEB_MAP = HOST + "static.jsp";

    private final static int PROVIDERS_DLG = Menu.FIRST;

    private LocationManager locationManager;

    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        progressDialog = new ProgressDialog(this) {
            {
                this.setTitle(getText(R.string.app_name));
                this.setMessage(getText(R.string.progression_desc));
                this.setCancelable(true);
                this.setOnCancelListener(new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        finish();
                    }
                });
            }
        };
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        List<String> providers = locationManager.getProviders(true);
        if (providerAvailable(providers)) {
            progressDialog.show();

            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                Log.d(LOG, "gps selected");
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 5, this);
            } else {
                for (String provider : providers) {
                    Log.d(LOG, provider + " selected");
                    locationManager.requestLocationUpdates(provider, 0, 5, this);
                }
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        locationManager.removeUpdates(this);
        if (progressDialog.isShowing()) {
            progressDialog.hide();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        progressDialog.dismiss();
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
    protected Dialog onCreateDialog(int id)
    {
        switch (id) {
        default:
            return super.onCreateDialog(id);
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

        Executors.newCachedThreadPool().execute(new Runnable() {

            @Override
            public void run()
            {
                String uri = getLocationUrl(ShareMyPosition.this, location);
                String msg = getString(R.string.body, uri);
                Intent t = new Intent(Intent.ACTION_SEND);
                t.setType("text/plain");
                t.addCategory(Intent.CATEGORY_DEFAULT);
                t.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject));
                t.putExtra(Intent.EXTRA_TEXT, msg);
                Intent share = Intent.createChooser(t, getString(R.string.app_name));
                startActivityForResult(share, 0);
            }

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        finish();
    }

    public static String getLocationUrl(Context context, Location location)
    {
        String url = getCurrentStaticLocationUrl(context, location);
        try {
            url = getTinyLink(url);
        } catch (Throwable e) {
            Log.e(LOG, "tinyLink don't work: " + url);
        }

        return url;
    }

    public static String getCurrentStaticLocationUrl(Context context, Location location)
    {
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();
        try {
            return STATIC_WEB_MAP + "?pos=" + latitude + "," + longitude + "&geocode=" + getAddress(context, latitude, longitude);
        } catch (Throwable e) {
            Log.e(LOG, "unable to parse address");
            return STATIC_WEB_MAP + "?pos=" + latitude + "," + longitude;
        }

    }

    public static String getAddress(Context context, double latitude, double longitude) throws IOException
    {
        Geocoder gc = new Geocoder(context);
        List<Address> address = gc.getFromLocation(latitude, longitude, 1);
        Address a = address.get(0);

        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.getMaxAddressLineIndex(); i++) {
            b.append(a.getAddressLine(i));
            b.append(" ");
        }

        return b.toString();
    }

    public static String getTinyLink(String url) throws ClientProtocolException, IOException, JSONException
    {
        HttpClient client = new DefaultHttpClient();
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
    }

    @Override
    public void onProviderEnabled(String provider)
    {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        List<String> providers = locationManager.getProviders(true);
        if (provider.equals(LocationManager.GPS_PROVIDER) && providers.contains(providers)) {
            if (status == LocationProvider.TEMPORARILY_UNAVAILABLE || status == LocationProvider.OUT_OF_SERVICE) {
                Log.d(LOG, "gps not yet available, changing the provider");
                for (String p : providers) {
                    Log.d(LOG, p + " selected");
                    locationManager.requestLocationUpdates(p, 0, 5, this);
                }
            }
        }
    }
}