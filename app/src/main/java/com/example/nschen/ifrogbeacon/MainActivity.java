package com.example.nschen.ifrogbeacon;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;


import com.powenko.ifroglab_bt_lib.ifrog;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ifrog.ifrogCallBack, SensorEventListener {
    private ImageButton imageButton1;
    //	private EditText editText1;
    private ListView listView1;

    /* set button */
    Button statusText;//my
    private boolean nextStatus = true;//first is true

    private ifrog mifrog;//運用library

    /* 調整distance */
    private double count = 0;
    private double distanceTotal = 0;
    double tempdis = 0;


    /* compass */
    private float currentDegree = 0f;// record the angle turned
    private SensorManager mSensorManager;// device sensor manager
    // define the display assembly compass picture
    private ImageView image;

    /* calculate direction */
    private float minRSSI = 1000000;
    private float turntoTarget = 0;

    String[] testValues= new String[]{	"Apple","Banana","Orange","Tangerine"};
    String[] testValues2= new String[]{	"Red","Yello","Orange","Yello"};

    String mac = "84:EB:18:7A:5B:80";
    private rowdata adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageButton1=(ImageButton) findViewById(R.id.imageButton1 ); //取得imageView

        statusText = (Button)findViewById(R.id.status);//my

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //image direction
        image = (ImageView) findViewById(R.id.imageViewCompass);


        listView1=(ListView) findViewById(R.id.listView1);   //取得listView1
        //ListAdapter adapter = createAdapter();

        BTinit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);


        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree+turntoTarget,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);



        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }



    private void SetupList(){
        adapter=new rowdata(this,testValues,testValues2);
        listView1.setAdapter(adapter);
        listView1.setOnItemClickListener(new AdapterView.OnItemClickListener(){      //選項按下反應
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String item = testValues[position];      //哪一個列表
                Toast.makeText(MainActivity.this, item + " selected", Toast.LENGTH_LONG).show();       //顯示訊號
            }
        } );

    }

/*

 */



    public void btnStartorStop(View v){
        //mifrog.scanLeDevice(true,10000);
        if(!nextStatus){//now -> start; nextStatus is true
            mifrog.scanLeDevice(nextStatus, 3600000);//按一次找1hr until you stop it
            nextStatus = true;
            statusText.setText("Start");
            //BTSearchFindDevicestatus(!nextStatus);
        }
        else{//now -> stop; nextStatus is false
            mifrog.scanLeDevice(nextStatus, 3600000);//stop it
            nextStatus = false;// = next time will be false
            statusText.setText("Stop");
            //BTSearchFindDevicestatus(!nextStatus);
        }

    }







    public void BTinit(){//初始化動作
        mifrog=new ifrog();
        mifrog.setTheListener(this);//設定監聽->CallBack(當有什麼反應會有callback的動作)->新增SearchFindDevicestatus, onDestroy
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {//查一下手機有沒有藍牙
            Toast.makeText(this,"this Device doean't support Bluetooth BLE", Toast.LENGTH_SHORT).show();
            finish();//沒有藍牙就關閉程式，離開
        }
        //取得藍牙service，並把這個service交給此有藍芽的設備(BLE)。有些人有藍芽的設備不見得有藍芽的軟體。// Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mifrog.InitCheckBT(bluetoothManager) == null) {
            Toast.makeText(this,"this Device doean't support Bluetooth BLE", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //掃描有沒有藍牙設備，（true是把它打開/false是把他關閉, 看要找多久->1000是一秒鐘）
        //mifrog.scanLeDevice(true,3600000);
    }


    public double calculateDistance(int rssi){
        /*   d = 10^((abs(RSSI) - A) / (10 * n))  */
        double result = 0;

        //if(count>15){
//        if(count>15){
//            tempdis = distanceTotal/count;
//            count = 0;
//            distanceTotal = 0;
//
//            myDegree = maxDegree;
//
//
//        }
//        else{
            float txPower = -59;//hard coded power value. Usually ranges between -59 to -65
            if(rssi == 0){
                result = -1.0;
            }
            double ratio = rssi*1.0/txPower;
            if (ratio < 1.0) {
                result =  Math.pow(ratio,10);
            }
            else{
                double distance = (0.89976)*Math.pow(ratio,7.7095) + 0.111;
                result =  distance;
            }

            count ++;
            distanceTotal += result;
//        }

        result = Math.round(result*10);

        /* direction */
        if( minRSSI > Math.abs(rssi)){
            minRSSI = Math.abs(rssi);
            turntoTarget = -currentDegree;//N:0, E:+
        }

        //return tempdis;
        return result;

    }



    ArrayList<String> Names = new ArrayList<String>();
    ArrayList<String> Address = new ArrayList<String>();


    @Override
    public void BTSearchFindDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        String t_address= device.getAddress();//有找到裝置的話先抓Address
        int index=0;
        boolean t_NewDevice=true;
        for(int i=0;i<Address.size();i++){
            String t_Address2=Address.get(i);
            if(t_Address2.compareTo(t_address)==0){//如果address和列表中的address一模一樣
                t_NewDevice=false;//登記說他不是新的device
                index=i;//把index記起來
                break;
            }
        }
        if(device.getName() != null){
            if(t_NewDevice==true){//如果是新的advice
                Address.add(t_address);
                //null can appear
                Names.add(device.getName()+" RSSI="+Integer.toString(rssi)+" d="+calculateDistance(rssi)+"cm"+" myD ="+Float.toString(turntoTarget));//抓名字然後放進列表
                testValues = Names.toArray(new String[Names.size()]);
                testValues2 =Address.toArray(new String[Address.size()]);

            }else{//如果不是新的device
                Names.set(index,device.getName()+" RSSI="+Integer.toString(rssi)+" d="+calculateDistance(rssi)+"cm"+" myD ="+Float.toString(turntoTarget));//更改device名字，RSSI:藍芽4.0裡面可以知道訊號強度
                testValues = Names.toArray(new String[Names.size()]);//放進array
            }
        }


        SetupList();//更新畫面
    }

    @Override
    public void BTSearchFindDevicestatus(boolean arg0) {//arg0:true/false，代表有沒有在找
        if(arg0==false){
            Toast.makeText(getBaseContext(),"Stop Search", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getBaseContext(),"Start Search",  Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {//當程式離開了就把service關掉，不然service一直跑會浪費電。
        super.onDestroy();
        mifrog.BTSearchStop();
    }



}
