package com.test.sdk;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Poi;
import com.amap.api.navi.AmapNaviPage;
import com.amap.api.navi.AmapNaviParams;
import com.amap.api.navi.AmapNaviType;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

public class MainActivity extends AppCompatActivity implements AMapLocationListener, PoiSearch.OnPoiSearchListener, AMap.OnMarkerClickListener, AMap.OnInfoWindowClickListener, AMap.InfoWindowAdapter {
    MapView mMapView = null;
    private AMap aMap;
    private AMapLocationClientOption mLocationOption;
    private AMapLocationClient mLocationClient;
    private AMapLocation mCurrentLocation;
    private Marker mLocationMarker;
    private Circle mLocationCircle;
    private PoiSearch mPoiSearch;
    private PoiOverlay mPoiOverlay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = findViewById(R.id.mapView);
        aMap = mMapView.getMap();
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        aMap.setOnMarkerClickListener(this);
        aMap.setOnInfoWindowClickListener(this);
        aMap.setInfoWindowAdapter(this);

        initLocation();

    }


    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationOption.setOnceLocation(true);
        mLocationClient = new AMapLocationClient(this.getApplicationContext());
        mLocationClient.setLocationListener(this);
        mLocationClient.startLocation();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.unRegisterLocationListener(this);
            mLocationClient.onDestroy();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation == null || aMapLocation.getErrorCode() != AMapLocation.LOCATION_SUCCESS) {
            Toast.makeText(this, aMapLocation.getErrorInfo() + "  " + aMapLocation.getErrorCode(), Toast.LENGTH_LONG).show();
            return;
        }
        mCurrentLocation = aMapLocation;
        LatLng curLatLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
        if (mLocationMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(curLatLng);
            markerOptions.anchor(0.5f, 0.5f);
//            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher));
            mLocationMarker = aMap.addMarker(markerOptions);
        }
        if (mLocationCircle == null) {
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(curLatLng);
            circleOptions.radius(aMapLocation.getAccuracy());
            circleOptions.strokeWidth(2);
            circleOptions.strokeColor(getResources().getColor(R.color.stroke));
            circleOptions.fillColor(getResources().getColor(R.color.fill));
            mLocationCircle = aMap.addCircle(circleOptions);
        }
        initPoiSearch(aMapLocation.getLatitude(), aMapLocation.getLongitude());
    }

    /**
     * 进行poi搜索
     *
     * @param lat
     * @param lon
     */
    private void initPoiSearch(double lat, double lon) {
        if (mPoiSearch == null) {
            PoiSearch.Query poiQuery = new PoiSearch.Query("", "充电站");
            LatLonPoint centerPoint = new LatLonPoint(lat, lon);
            PoiSearch.SearchBound searchBound = new PoiSearch.SearchBound(centerPoint, 5000);
            mPoiSearch = new PoiSearch(this.getApplicationContext(), poiQuery);
            mPoiSearch.setBound(searchBound);
            mPoiSearch.setOnPoiSearchListener(this);
            mPoiSearch.searchPOIAsyn();
        }
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        if (i != AMapException.CODE_AMAP_SUCCESS || poiResult == null) {
            return;
        }
        if (mPoiOverlay != null) {
            mPoiOverlay.removeFromMap();
        }
        mPoiOverlay = new PoiOverlay(aMap, poiResult.getPois());
        mPoiOverlay.addToMap();
        mPoiOverlay.zoomToSpan();
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }


    /**
     * 自定义marker点击弹窗内容
     *
     * @param marker
     * @return
     */
    @Override
    public View getInfoWindow(final Marker marker) {
        View view = getLayoutInflater().inflate(R.layout.poikeywordsearch_uri,
                null);
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(marker.getTitle());

        TextView snippet = (TextView) view.findViewById(R.id.snippet);
        int index = mPoiOverlay.getPoiIndex(marker);
        float distance = mPoiOverlay.getDistance(index);
        String showDistance = Utils.getFriendlyDistance((int) distance);
        snippet.setText("距当前位置" + showDistance);
        ImageButton button = (ImageButton) view
                .findViewById(R.id.start_amap_app);
        // 调起导航
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAMapNavi(marker);
            }
        });
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 点击一键导航按钮跳转到导航页面
     *
     * @param marker
     */
    private void startAMapNavi(final Marker marker) {
        if (mCurrentLocation == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(new String[]{"模拟导航", "组件导航"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    Intent intent = new Intent(MainActivity.this, RouteNaviActivity.class);
                    intent.putExtra("gps", false);
                    intent.putExtra("start", new NaviLatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                    intent.putExtra("end", new NaviLatLng(marker.getPosition().latitude, marker.getPosition().longitude));
                    startActivity(intent);
                }else{
                    LatLng start = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                    LatLng p2 = marker.getPosition();
                    AmapNaviParams params = new AmapNaviParams(new Poi("当前位置", start, ""), null, new Poi(marker.getTitle(), p2, ""), AmapNaviType.DRIVER);
                    params.setUseInnerVoice(true);
                    AmapNaviPage.getInstance().showRouteActivity(getApplicationContext(), params, null);
                }
            }
        });
        builder.create().show();


    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (mLocationMarker == marker) {
            return false;
        }

        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }
}
