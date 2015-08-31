package com.lewis.locationtest;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Google map 位置定位
 */
public class MainActivity extends ActionBarActivity {

    private TextView positionTextView;
    private LocationManager locationManager;
    private String provider;

    public static final int SHOW_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        positionTextView = (TextView) findViewById(R.id.position_text_view);
        //位置定位服务
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providerList = locationManager.getProviders(true);
        if (providerList.contains(LocationManager.GPS_PROVIDER)){
            provider = LocationManager.GPS_PROVIDER;
        }else if (providerList.contains(LocationManager.NETWORK_PROVIDER)){
            provider = LocationManager.NETWORK_PROVIDER;
        }else {
            //当没有可用的位置提供器时，弹出Toast提示用户
            Toast.makeText(MainActivity.this, "No Location provider to use", Toast.LENGTH_SHORT).show();
            return;
        }
        //获取Location对象,包含了经度、维度、海拔等一些列的位置信息
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null){
            //显示当前设备的位置信息
            showLocation(location);
        }
        //位置发生变化的时候获取到最新的位置信息
        locationManager.requestLocationUpdates(provider,5000,1, locationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null){
            //关闭程序时将监听器移除
            locationManager.removeUpdates(locationListener);
        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //更新当前设备的位置信息
            showLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    /**
     * 显示位置信息
     * @param location 位置信息
     */
    private void showLocation(final Location location){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //组装反向地理编码的接口地址
                    StringBuilder url = new StringBuilder();
                    url.append("http://maps.googleapis.com/maps/api/geocode/json?latlng=");
                    url.append(location.getLatitude()).append(",");
                    url.append(location.getLongitude());
                    url.append("&sensor=false");
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(url.toString());
                    //在请求消息头中指定语言，保证服务器会返回中文数据
                    httpGet.addHeader("Accetp-Language","zh-CN");
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    if (httpResponse.getStatusLine().getStatusCode() == 200){
                        HttpEntity entity = httpResponse.getEntity();
                        String response = EntityUtils.toString(entity, "utf-8");
                        JSONObject jsonObject = new JSONObject(response);
                        //获取results节点下的位置信息
                        JSONArray resultArray = jsonObject.getJSONArray("results");
                        if (resultArray.length() > 0){
                            JSONObject subObject = resultArray.getJSONObject(0);
                            //取出格式化后的位置信息
                            String address = subObject.getString("formatted_address");
                            //使用异步消息机制将
                            Message message = new Message();
                            message.what = SHOW_LOCATION;
                            message.obj = address;
                            handler.sendMessage(message);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
        /*
        String currentPosition = "latitude is "+ location.getLatitude() + "\n" +
                "longitude is "+ location.getLongitude();
        positionTextView.setText(currentPosition);
        */
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg){
            switch (msg.what){
                case SHOW_LOCATION:
                    String currentPosition = (String) msg.obj;
                    positionTextView.setText(currentPosition);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
