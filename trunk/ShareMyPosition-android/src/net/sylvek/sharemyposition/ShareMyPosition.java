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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

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
import org.mapsforge.android.maps.GeoPoint;
import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.MapViewMode;
import org.mapsforge.android.maps.Overlay;
import org.mapsforge.android.maps.Projection;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ShareMyPosition extends MapActivity implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private static final boolean IS_JELLY_BEAN_OR_GREATER = Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN;

    public static final String EXTRA_INTENT = "extra_intent";

    public static final String LOG = "ShareMyPosition";

    public static final String VERSION = "1.2.10";

    private static final int ZOOM_LEVEL = 15;

    public static final String HOST = "http://smp-next.appspot.com/";

    private static final String SHORTY_URI = HOST + "service/create?url=";

    private static final String NATIVE_WEB_MAP = HOST + "native.jsp";

    private static final String STATIC_WEB_MAP = HOST + "static.jsp";

    private final static int PROVIDERS_DLG = Menu.FIRST;

    private final static int PROGRESS_DLG = PROVIDERS_DLG + 1;

    private final static int MAP_DLG = PROGRESS_DLG + 1;

    public static final String PREF_LAT_LON_CHECKED = "net.sylvek.sharemyposition.pref.latlon.checked";

    public static final String PREF_ADDRESS_CHECKED = "net.sylvek.sharemyposition.pref.address.checked";

    public static final String PREF_URL_CHECKED = "net.sylvek.sharemyposition.pref.url.checked";

    public static final String PREF_BODY_DEFAULT = "net.sylvek.sharemyposition.pref.body.default";

    public static final String PREF_TRACK_CHECKED = "net.sylvek.sharemyposition.pref.track.checked";

    public static final String PREF_GMAP_CHECKED = "net.sylvek.sharemyposition.pref.gmap.checked";

    private ConnectivityManager connectivityManager;

    private TelephonyManager telephonyManager;

    private Geocoder gc;

    private LocationClient mLocationClient;

    private final HttpParams params = new BasicHttpParams();

    private Location location;

    private MapView sharedMap;

    private SharedPreferences pref;

    private String[] tips;

    private final Random random = new Random();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        HttpProtocolParams.setUserAgent(params, "Android/" + Build.DISPLAY + "/version:" + VERSION);

        gc = new Geocoder(this);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        tips = getResources().getStringArray(R.array.tips);

        sharedMap = new MapView(ShareMyPosition.this, MapViewMode.MAPNIK_TILE_DOWNLOAD);
        sharedMap.setClickable(true);
        sharedMap.setAlwaysDrawnWithCacheEnabled(true);
        sharedMap.setFocusable(true);
        sharedMap.getOverlays().add(new CenterOverlay(sharedMap));
    }

    private boolean isConnected()
    {
        if (telephonyManager == null || connectivityManager == null) {
            return false;
        }

        final boolean roaming = telephonyManager.isNetworkRoaming();
        final NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        return info != null && info.isConnected() && !roaming;
    }

    public void displayTip()
    {
        int index = random.nextInt(tips.length);
        String tip = tips[index];
        Log.d(LOG, "generate random tips: " + index + "->" + tip);
        Toast.makeText(ShareMyPosition.this, tip, Toast.LENGTH_LONG).show();
    };

    @Override
    protected void onStart()
    {
        super.onStart();
        if (isGooglePLayServicesAvailable()) {
            this.mLocationClient = new LocationClient(this, this, this);
        } else {
            Toast.makeText(this, R.string.no_play_service, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (this.mLocationClient != null && !this.mLocationClient.isConnected()) {
            this.mLocationClient.connect();
            // displayTip();
            showDialog(PROGRESS_DLG);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (this.mLocationClient != null && mLocationClient.isConnected()) {
            this.mLocationClient.removeLocationUpdates(this);
            this.mLocationClient.disconnect();
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id) {
        default:
            super.onPrepareDialog(id, dialog);
            break;
        case MAP_DLG:
            final View optionsLayout = (View) dialog.findViewById(R.id.custom_layout);
            final FrameLayout map = (FrameLayout) dialog.findViewById(R.id.sharedmap);
            /* to catch dismiss event from AlertControler, we need to "override" onClickListener */
            final Button neutral = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
            neutral.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0)
                {
                    if (optionsLayout.getVisibility() == View.GONE) {
                        optionsLayout.setVisibility(View.VISIBLE);
                        map.setVisibility(View.GONE);
                        neutral.setText(R.string.hide);
                    } else {
                        optionsLayout.setVisibility(View.GONE);
                        map.setVisibility(View.VISIBLE);
                        neutral.setText(R.string.options);
                    }
                }
            });
            if (location != null && sharedMap != null) {
                sharedMap.getController().setZoom(ZOOM_LEVEL);
                sharedMap.getController().setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));
            }

            if (!isConnected()) {
                final CheckBox geocodeAddress = (CheckBox) dialog.findViewById(R.id.add_address_location);
                geocodeAddress.setEnabled(false);
                neutral.setEnabled(false);
                map.setVisibility(View.GONE);
                optionsLayout.setVisibility(View.VISIBLE);
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
            final FrameLayout map = (FrameLayout) sharedMapView.findViewById(R.id.sharedmap);
            map.addView(this.sharedMap);
            final CheckBox latlonAddress = (CheckBox) sharedMapView.findViewById(R.id.add_lat_lon_location);
            final CheckBox geocodeAddress = (CheckBox) sharedMapView.findViewById(R.id.add_address_location);
            final RadioButton nourl = (RadioButton) sharedMapView.findViewById(R.id.add_no_url_location);
            final RadioButton url = (RadioButton) sharedMapView.findViewById(R.id.add_url_location);
            final RadioButton gmap = (RadioButton) sharedMapView.findViewById(R.id.add_native_location);
            final EditText body = (EditText) sharedMapView.findViewById(R.id.body);
            final ToggleButton track = (ToggleButton) sharedMapView.findViewById(R.id.add_track_location);

            latlonAddress.setChecked(pref.getBoolean(PREF_LAT_LON_CHECKED, true));
            geocodeAddress.setChecked(pref.getBoolean(PREF_ADDRESS_CHECKED, true));
            final boolean isUrl = pref.getBoolean(PREF_URL_CHECKED, true);
            final boolean isGmap = pref.getBoolean(PREF_GMAP_CHECKED, false);
            url.setChecked(isUrl);
            gmap.setChecked(isGmap);
            nourl.setChecked(!isUrl && !isGmap);
            body.setText(pref.getString(PREF_BODY_DEFAULT, getString(R.string.body)));
            track.setChecked(pref.getBoolean(PREF_TRACK_CHECKED, false));

            if (track.isChecked()) {
                latlonAddress.setEnabled(false);
                latlonAddress.setChecked(false);
                geocodeAddress.setEnabled(false);
                geocodeAddress.setChecked(false);
                url.setEnabled(false);
                url.setChecked(true);
                gmap.setEnabled(false);
                gmap.setChecked(false);
                nourl.setEnabled(false);
                nourl.setChecked(false);
            }

            track.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    latlonAddress.setEnabled(!isChecked);
                    latlonAddress.setChecked(!isChecked);
                    geocodeAddress.setEnabled(!isChecked);
                    geocodeAddress.setChecked(!isChecked);
                    url.setEnabled(!isChecked);
                    url.setChecked(true);
                    gmap.setEnabled(!isChecked);
                    gmap.setChecked(!isChecked);
                    nourl.setEnabled(!isChecked);
                    nourl.setChecked(!isChecked);
                }
            });

            return new AlertDialog.Builder(this).setTitle(R.string.app_name)
                    .setView(sharedMapView)
                    .setOnCancelListener(new OnCancelListener() {

                        @Override
                        public void onCancel(DialogInterface arg0)
                        {
                            finish();
                        }
                    })
                    .setNeutralButton(R.string.options, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1)
                        {
                            /* needed to display neutral button */
                        }
                    })
                    .setPositiveButton(R.string.share_it, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1)
                        {
                            final boolean isLatLong = ((CheckBox) sharedMapView.findViewById(R.id.add_lat_lon_location)).isChecked();
                            final boolean isGeocodeAddress = ((CheckBox) sharedMapView.findViewById(R.id.add_address_location)).isChecked();
                            final boolean isUrl = ((RadioButton) sharedMapView.findViewById(R.id.add_url_location)).isChecked();
                            final boolean isGmap = ((RadioButton) sharedMapView.findViewById(R.id.add_native_location)).isChecked();
                            final EditText body = (EditText) sharedMapView.findViewById(R.id.body);
                            final boolean isTracked = ((ToggleButton) sharedMapView.findViewById(R.id.add_track_location)).isChecked();
                            final String uuid = UUID.randomUUID().toString();

                            pref.edit()
                                    .putBoolean(PREF_LAT_LON_CHECKED, isLatLong)
                                    .putBoolean(PREF_ADDRESS_CHECKED, isGeocodeAddress)
                                    .putBoolean(PREF_URL_CHECKED, isUrl)
                                    .putBoolean(PREF_GMAP_CHECKED, isGmap)
                                    .putString(PREF_BODY_DEFAULT, body.getText().toString())
                                    .putBoolean(PREF_TRACK_CHECKED, isTracked)
                                    .commit();

                            final Intent t = new Intent(Intent.ACTION_SEND);
                            t.setType("text/plain");
                            t.addCategory(Intent.CATEGORY_DEFAULT);
                            final Intent share = Intent.createChooser(t, getString(R.string.app_name));
                            final GeoPoint p = sharedMap.getMapCenter();

                            final String text = body.getText().toString();
                            share(p.getLatitude(), p.getLongitude(), t, share, text, isGeocodeAddress, isUrl, isGmap, isLatLong,
                                    isTracked, uuid);
                        }
                    })
                    .create();
        case PROGRESS_DLG:
            final ProgressDialog dlg = new ProgressDialog(this);
            dlg.setTitle(getText(R.string.app_name));
            dlg.setMessage(getText(R.string.progression_desc));
            dlg.setIndeterminate(true);
            dlg.setCancelable(true);
            dlg.setOnCancelListener(new OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog)
                {
                    finish();
                }
            });
            return dlg;
        case PROVIDERS_DLG:
            return new AlertDialog.Builder(this).setTitle(R.string.app_name)
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_menu_help)
                    .setMessage(R.string.providers_needed)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            finish();
                        }

                    })
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent gpsProperty = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(gpsProperty);
                        }
                    })
                    .create();
        }
    }

    @Override
    public void onLocationChanged(final Location location)
    {
        Log.d(LOG, "location changed: " + location.toString());

        this.location = location;

        final Intent extra = getIntent().getParcelableExtra(EXTRA_INTENT);
        if (extra != null) {
            final Intent b = getIntent();
            boolean isGeocodeAddress = b.getBooleanExtra(ShareMyPosition.PREF_ADDRESS_CHECKED, true);
            boolean isLatLong = b.getBooleanExtra(ShareMyPosition.PREF_LAT_LON_CHECKED, true);
            boolean isUrl = b.getBooleanExtra(ShareMyPosition.PREF_URL_CHECKED, true);
            boolean isGmap = b.getBooleanExtra(ShareMyPosition.PREF_GMAP_CHECKED, false);
            String body = b.getStringExtra(ShareMyPosition.PREF_BODY_DEFAULT);
            final boolean isTracked = b.getBooleanExtra(ShareMyPosition.PREF_TRACK_CHECKED, false);
            final String uuid = UUID.randomUUID().toString();
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            share(latitude, longitude, extra, null, body, isGeocodeAddress, isUrl, isGmap, isLatLong, isTracked, uuid);
        } else {
            showDialog(MAP_DLG);
        }
    }

    /**
     * @param location
     * @param extra
     * @param uuid
     * @param isTracked
     */
    private void share(final double latitude, final double longitude, final Intent extra, final Intent toLaunch,
            final String body, final boolean isGeocodeAddress, final boolean isUrl, final boolean isNative,
            final boolean isLatLong, final boolean isTracked, final String uuid)
    {
        Executors.newCachedThreadPool().execute(new Runnable() {

            @Override
            public void run()
            {
                String msg = getMessage(latitude, longitude, body, isGeocodeAddress, isUrl, isNative, isLatLong, isTracked, uuid);

                if (isTracked) {
                    String url = getCurrentStaticLocationUrl(latitude, longitude, isNative, isTracked, uuid);
                    startTracking(uuid, url, msg);
                }

                extra.addCategory(Intent.CATEGORY_DEFAULT)
                        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject))
                        .putExtra(Intent.EXTRA_TEXT, msg)
                        .putExtra("sms_body", msg);
                startActivity((toLaunch != null) ? toLaunch : extra);
                finish();
            }
        });
    }

    private String getMessage(double latitude, double longitude, String body, boolean isGeocodeAddress, boolean isUrl,
            boolean isNative, boolean isLatLong, boolean isTracked, String uuid)
    {
        final boolean isConnected = isConnected();
        final StringBuilder msg = new StringBuilder(body);
        if (isGeocodeAddress && isConnected) {
            final String address = getAddress(latitude, longitude);
            if (!address.equals("")) {
                if (msg.length() > 0) {
                    msg.append(", ");
                }
                msg.append(address);
            }
        }
        if (isUrl || isNative) {
            if (msg.length() > 0) {
                msg.append(", ");
            }
            msg.append(getLocationUrl(isConnected, isNative, isTracked, latitude, longitude, uuid));
        }
        if (isLatLong) {
            if (msg.length() > 0) {
                msg.append(", ");
            }
            msg.append(getLatLong(latitude, longitude));
        }

        return msg.toString();
    }

    public String getLocationUrl(boolean isConnected, boolean isNative, boolean isTracked, double latitude, double longitude,
            String uuid)
    {
        String url = getCurrentStaticLocationUrl(latitude, longitude, isNative, isTracked, uuid);
        if (isConnected) {
            try {
                url = getTinyLink(url);
            } catch (Exception e) {
                Log.e(LOG, "tinyLink doesn't work: " + url);
            }
        }
        return url;
    }

    public String getCurrentStaticLocationUrl(double latitude, double longitude, boolean isNative, boolean isTracked, String uuid)
    {
        StringBuilder uri = new StringBuilder((isNative) ? NATIVE_WEB_MAP : STATIC_WEB_MAP).append("?pos=")
                .append(latitude)
                .append(",")
                .append(longitude)
                .append("&tracked=")
                .append(isTracked)
                .append("&uuid=")
                .append(uuid);
        return uri.toString();
    }

    public String getAddress(double latitude, double longitude)
    {
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
            if (i < (a.getMaxAddressLineIndex() - 1)) {
                b.append(" ");
            }
        }

        return b.toString();
    }

    public String getLatLong(double latitude, double longitude)
    {
        return "(pos=" + latitude + "," + longitude + ")";
    }

    public String getTinyLink(String url) throws ClientProtocolException, IOException, JSONException
    {
        HttpClient client = new DefaultHttpClient(params);
        HttpGet get = new HttpGet(SHORTY_URI + URLEncoder.encode(url, "UTF8"));
        HttpResponse response = client.execute(get);
        if (response.getStatusLine().getStatusCode() == 200) {
            return EntityUtils.toString(response.getEntity());
        }

        return url;
    }

    /**
     * @param uuid
     */
    private void startTracking(final String uuid, final String url, final String msg)
    {
        final Context context = getApplicationContext();

        // start the tracker
        sendBroadcast(new Intent(context, ShareByTracking.StartTracking.class).putExtra(ShareByTracking.UUID, uuid));

        // generate delete action
        final Intent deleteIntent = new Intent(context, ShareByTracking.StopTracking.class);
        final Intent t = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .addCategory(Intent.CATEGORY_DEFAULT)
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject))
                .putExtra(Intent.EXTRA_TEXT, msg)
                .putExtra("sms_body", msg);
        final PendingIntent delete = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // generate content action
        final Intent contentIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        final PendingIntent intent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // generate share action
        final Intent shareIntent = Intent.createChooser(t, getString(R.string.app_name)).addCategory(Intent.CATEGORY_DEFAULT);
        final PendingIntent share = PendingIntent.getActivity(context, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // show notification
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.notification)
                .setContentTitle(getString(R.string.app_name))
                .setOngoing(IS_JELLY_BEAN_OR_GREATER)
                .setDeleteIntent(delete)
                .addAction(android.R.drawable.ic_menu_share, getString(R.string.share_it), share)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.track_stop_it), delete)
                .setContentText(getString(R.string.track_location_notification))
                .setContentIntent(intent)
                .setUsesChronometer(true);
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(R.string.app_name, mBuilder.build());
    }

    private boolean isGooglePLayServicesAvailable()
    {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        return ConnectionResult.SUCCESS == resultCode;
    }

    public class CenterOverlay extends Overlay {

        private final MapView map;

        private Bitmap pin;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG) {
            {
                this.setAlpha(255);
                this.setStyle(android.graphics.Paint.Style.FILL_AND_STROKE);
            }
        };

        private Point out;

        /**
         * @param fillPaint
         * @param outlinePaint
         */
        public CenterOverlay(MapView map)
        {
            super();
            this.map = map;

            BitmapDrawable b = (BitmapDrawable) getResources().getDrawable(R.drawable.pin);
            this.pin = b.getBitmap();
        }

        @Override
        protected void drawOverlayBitmap(Canvas canvas, Point drawPosition, Projection projection, byte drawZoomLevel)
        {
            if (map != null) {
                final GeoPoint in = map.getMapCenter();
                if (in != null) {
                    out = projection.toPoint(in, out, drawZoomLevel);
                    float x = out.x - drawPosition.x - pin.getWidth() / 2;
                    float y = out.y - drawPosition.y - pin.getHeight();
                    canvas.drawBitmap(pin, x, y, paint);
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        Log.e(LOG, "onConnectionFailed");
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        final LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1);
        this.mLocationClient.requestLocationUpdates(locationRequest, this);
        Log.d(LOG, "onConnected");
    }

    @Override
    public void onDisconnected()
    {
        Log.d(LOG, "onDisconnected");
    }
}