package com.example.sebastin.bina2;


import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.DashPathEffect;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.nfc.Tag;
import android.os.Environment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.MediaController;
import org.w3c.dom.Text;
import android.widget.MediaController.MediaPlayerControl;


import com.androidplot.util.PixelUtils;
import com.androidplot.xy.CatmullRomInterpolator;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;


public class RecordingsActivity extends Activity{

    public ListView listView;
    public ArrayAdapter arrayAdapter;
    public String []  android_versions = {"1","2","3"};
    public String directory = "/BinaRecordings";
    public String filename = "Grabacion";
    public String format = ".wav";
    public File root = Environment.getExternalStorageDirectory();
    public File dir = new File(root.getAbsolutePath() + directory);
    public String filelist[];
    public String listviewitems[] = {"No hay Grabaciones"};
    public long totalspace = dir.getTotalSpace();
    public int listcontrol = 0;
    public String filetoplay = "file";
    TextView text;
    public mPlayer mP = new mPlayer();
    public Visualizer vs = new Visualizer(1);
    public byte [] visbytes;
    public int captureSizeRange[];
    private static final String TAG = RecordingsActivity.class.getSimpleName();
    public MediaController.MediaPlayerControl player;
    public Visualizer.MeasurementPeakRms measurement = new Visualizer.MeasurementPeakRms();
    public int pk;
    public int rate = 15;
    public Visualizer.OnDataCaptureListener listener;
    public AudioRead aR;
    public byte [] bufar;
    public int bufOffSet = 0;
    public byte [] myData;
    public float rmsValue = 0;
    public long itc = 0;
    public short [] leftBuffer = new short[5000];
    public short [] rightBuffer = new short [5000];
    private XYPlot plot;
    public double leftRms = 0;
    public double rightRms = 0;
    double leftDbfs = 0;
    double rightDbfs = 0;
    public LineGraphSeries<DataPoint> mSeries1;
    GraphView graph;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);
        Bundle bundle = getIntent().getExtras();
        directory = bundle.getString("RecordActivitydirectory");
        filename = "Grabacion";
        format = ".wav";
        root = Environment.getExternalStorageDirectory();
        dir = new File(root.getAbsolutePath() + directory);
        filelist = dir.list();
        text = (TextView)findViewById(R.id.textViewR);
        text.setText(""+totalspace);
        listView = (ListView) findViewById(R.id.listView);
        listviewitems = filelist;
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, listviewitems);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (mP.getState().equals(mPlayer.playerState.STOPPED)){
                   listcontrol = 0;
                }
                if (!filetoplay.equals(dir.toString()+"/"+parent.getItemAtPosition(position).toString()) && (mP.getState().equals(mPlayer.playerState.PLAYING)))
                {
                    //cambio de grabacion si playing
                    reproduccion();
                }
                filetoplay = dir.toString()+"/"+parent.getItemAtPosition(position).toString();
                aR = new AudioRead();
                aR.setAudioRead(filetoplay,100);
                reproduccion();
                if (mP.getState().equals(mPlayer.playerState.PLAYING)){
                    Toast.makeText(getBaseContext(), parent.getItemAtPosition(position) + " "+mPlayer.playerState.PLAYING.toString(), Toast.LENGTH_SHORT).show();
                }
                else if (mP.getState().equals(mPlayer.playerState.STOPPED)){
                    Toast.makeText(getBaseContext(), parent.getItemAtPosition(position) +" "+mPlayer.playerState.STOPPED.toString(), Toast.LENGTH_SHORT).show();
                }
                text.setText("" + pk);
//
            }
        });
    }
    public void reproduccion ()
    {
        if (listcontrol == 0) {
            try {
                mP.startPlayBack(filetoplay);
                new Thread(new Task()).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            listcontrol = 1;
        }
        else if (listcontrol == 1){
            mP.stopPlayback();
            listcontrol = 0;
            bufOffSet = 0;
        }

    }



    public class Task implements Runnable {
        @Override
        public void run() {
            itc = 0;
            while(mP.getState().equals(mPlayer.playerState.PLAYING)) {
                bufar=aR.getbufAudioRead(itc);
                leftBuffer = aR.getLeftData();
                rightBuffer = aR.getRightData();
                leftRms = aR.getLeftRMSvalue();
                rightRms = aR.getRightRMSvalue();
                leftDbfs = Math.round(aR.getLeftDbfsValue());
                rightDbfs = Math.round(aR.getRightDbfsValue());
                final BarGraphSeries<DataPoint> series = new BarGraphSeries<DataPoint>(new DataPoint[] {
                        new DataPoint(1,Math.abs(-80-leftDbfs)),
                        new DataPoint(2,Math.abs(-80-rightDbfs))
                });
                series.setSpacing(50);
//                    mSeries1 = new LineGraphSeries<DataPoint>(generateData());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                    text.post(new Runnable() {
                        @Override
                        public void run() {
                            //graph.addSeries(series);
                            text.setText("" +leftDbfs+ "   " + rightDbfs);
                        }
                    });
                    itc += aR.getNumberSamples();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recordings, menu);
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
    public void convolutionActivity(MenuItem item) {
        Intent intent = new Intent(this,ConvolutionActivity.class);
        startActivity(intent);
    }

}
