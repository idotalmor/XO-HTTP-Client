package com.example.hackeru.xo;

import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private boolean IsO,AmIO=false;
    byte[][] x;
    byte xscore,oscore;
    TableLayout parent;
    MediaPlayer ding,winning,tie;
    AnimationDrawable xanim,oturn,xturn;
    LinearLayout linearly;
    TextView scorexttl,scoreottl,whoru;
    ImageView scoroimg,scorximg;
    String server = "http://10.100.102.17:999";

    Handler handler;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        parent=(TableLayout)findViewById(R.id.table);//tablelayout
        linearly=(LinearLayout)findViewById(R.id.linearly);//linearlayout - btn on top

        scorexttl=(TextView)findViewById(R.id.scorexttl);//xscore
        scoreottl=(TextView)findViewById(R.id.scoreottl);//oscore
        whoru=(TextView)findViewById(R.id.whoru);//Who R U

        scoroimg=(ImageView)findViewById(R.id.scoroimg);//o bottom img
        oturn=(AnimationDrawable)scoroimg.getDrawable();//o bottom animation

        scorximg=(ImageView)findViewById(R.id.scorximg);//x bottom img
        xturn=(AnimationDrawable)scorximg.getDrawable();//x bottom animation
        handler=new Handler(Looper.getMainLooper());

        ding=MediaPlayer.create(this,R.raw.ding);
        winning=MediaPlayer.create(this,R.raw.winning);
        tie=MediaPlayer.create(this,R.raw.tie);

        Thread t =new Thread(){
            @Override
            public void run() {
                try {
                    //send Http get request to our server
                    final String res = new HttpRequest(server+"/ConnectPlayer").prepare().sendAndReadString();
                    if("O".equals(res.trim())){AmIO=true;}
                    handler.post(new Runnable() {
                        public void run() {
                            whoru.setTextSize(26);
                            whoru.setText("You Are "+res);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        reset();
        addbuttons();
        toggleturn();
        btnmood();
    }


    public void btnmood(){//if user turn - btn are clickable
        boolean b=false;
        if(IsO==AmIO){b=true;}
        for(int i=0;i<parent.getChildCount();i++){//set frame img source to all imageview in table
            TableRow t=(TableRow)parent.getChildAt(i);
            for(int j=0;j<t.getChildCount();j++){
                ImageView imageView=(ImageView)t.getChildAt(j);
                String tag = imageView.getTag().toString();
                String [] position = tag.split(",");
                int row=Integer.parseInt(position[0]),column = Integer.parseInt(position[1]);
                if(x[row][column]==0){
                    imageView.setClickable(b);
                }
            }}
    }

    public void onBtClick(View v){

        String str=v.getTag().toString();
           String []stra= str.split(",");
        int row=Integer.parseInt(stra[0]),column=Integer.parseInt(stra[1]);
            if(!IsO){
                ((ImageView)v).setImageResource(R.drawable.xdra);
                xanim=(AnimationDrawable) ((ImageView)v).getDrawable();
                xanim.setOneShot(true);xanim.start();
                x[row][column]=1;
                okpress(v,row,column);

            }else{
                ((ImageView)v).setImageResource(R.drawable.o);
                x[row][column]=2;
                okpress(v,row,column);
            }

        IsO =!IsO;
        toggleturn();
        btnmood();
    }



    public void okpress(View v,int row,int column){//raise
        ding.start();
        v.setClickable(false);
        final JSONObject jn=new JSONObject();
        try {
            jn.put("Player",AmIO);
            jn.put("Row",row);
            jn.put("Column",column);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final int[] GameStatus = new int[1];
        Thread thread = new Thread(){//send to the server player move
            @Override
            public void run() {
                try {

                    JSONObject jnresponse = new HttpRequest(server+"/Play").prepare(HttpRequest.Method.POST).withData(jn.toString()).sendAndReadJSON();

                    boolean moved = jnresponse.getBoolean("Moved");
                    GameStatus[0] = jnresponse.getInt("GameStatus");
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ifwon(GameStatus[0]);

    }



    public void ifwon(int gamestatus){
        switch (gamestatus){
            case 0:return;
            case 1:{
                if(AmIO){FinishAlert("You Lost!","Fight Again?");oscore++;}
                else {FinishAlert("You Won!","Fight Again?");xscore++;winning.start();}break;
            }
            case 2:{
                if(AmIO){FinishAlert("You Won!","Fight Again?");oscore++;winning.start();}
                else {FinishAlert("You Lost!","Fight Again?");xscore++;}break;
            }
            case 3:{
                FinishAlert("It's A tie!","Fight Again?");
                tie.start();break;
            }
        }
        RefreshScore();
    }

    public void refresh(View view) {
        Thread thread = new Thread(){
            @Override
            public void run() {
                try {
                    JSONObject jnresponse = new HttpRequest(server+"/Refresh?Password=973846").prepare(HttpRequest.Method.GET).sendAndReadJSON();
                    Log.i("jsonfilestring",jnresponse.toString());
                    final int gamestatus = jnresponse.getInt("GameStatus");
                    int xscoreserver = jnresponse.getInt("XScore");
                    int oscoreserver = jnresponse.getInt("OScore");
                    boolean iso = jnresponse.getBoolean("Iso");
                    int row = jnresponse.getInt("Row");
                    int column = jnresponse.getInt("Column");
                    JSONObject resetgame = jnresponse.getJSONObject("resetgame");
                    if(resetgame.length()!=0){
                        String player = resetgame.getString("player");
                        if(player.equals("O")){
                            if(!AmIO){reset();}
                        }
                    }

                    handler.post(new Runnable() {//effect main thread ui
                        @Override
                        public void run() {
                            ifwon(gamestatus);
                        }
                    });

                    if(AmIO==iso&&x[row][column]==0){ding.start();x[row][column]=AmIO?(byte)1:2;
                        final ImageView imageView=(ImageView) parent.findViewWithTag(row+","+column);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(!AmIO){

                                    imageView.setImageResource(R.drawable.o);}
                                else{
                                    imageView.setImageResource(R.drawable.xdra);
                                    xanim=(AnimationDrawable) imageView.getDrawable();
                                    xanim.setOneShot(true);xanim.start();}
                            }
                        });
                    }

                    if(AmIO==iso){handler.post(new Runnable() {
                        @Override
                        public void run() {
                            toggleturn();
                        }
                    });IsO=iso;btnmood();}
                    xscore=(byte)xscoreserver;
                    oscore=(byte)oscoreserver;
                    RefreshScore();
                    Log.i("table",String.valueOf(x[2][0]));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        thread.start();

    }

    public void reset(){ //reset array, table and counter
        x=new byte[3][3];//reset array
        //counter=0;//reset counter
        for(int i=0;i<parent.getChildCount();i++){//set frame img source to all imageview in table
            TableRow t=(TableRow)parent.getChildAt(i);
        for(int j=0;j<t.getChildCount();j++){
            ( (ImageView)t.getChildAt(j)).setImageResource(R.drawable.frame);
            ( (ImageView)t.getChildAt(j)).setClickable(true);
        }}
    }



    public void FinishAlert(String ttl, String body){
        AlertDialog.Builder dialog=new AlertDialog.Builder(this);
        dialog.setCancelable(false);
        dialog.setTitle(ttl).setMessage(body).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for(int i=0;i<parent.getChildCount();i++){//set frame img source to all imageview in table
                    TableRow t=(TableRow)parent.getChildAt(i);
                    for(int j=0;j<t.getChildCount();j++){
                        ( (ImageView)t.getChildAt(j)).setClickable(false);
                    }}
            }
        }).setPositiveButton("Reset Game", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                reset();
            }
        }).show();
    }
    private void addbuttons(){//add top buttons
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1.0f);
        Button RES=(Button) LayoutInflater.from(this).inflate(R.layout.btn,null);//add reset score btn
        RES.setText("Reset Score");
        RES.setLayoutParams(lp);
        RES.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"A Fresh Start!",Toast.LENGTH_LONG).show();

                Thread t =new Thread(){
                    @Override
                    public void run() {
                        try {
                            String response = new HttpRequest(server+"/Play?Action=resetscore").prepare(HttpRequest.Method.GET).sendAndReadString();
                            if("OK".equals(response)){
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this,"The Scores Reset Successfully",Toast.LENGTH_LONG).show();
                                        xscore=0;
                                        oscore=0;
                                        RefreshScore();
                                    }
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();

            }
        });
        linearly.addView(RES);
        Button NEWG=(Button) LayoutInflater.from(this).inflate(R.layout.btnr,null);//add reset score btn
        NEWG.setText("Reset Game");
        NEWG.setLayoutParams(lp);
        NEWG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset();
            }
        });
        linearly.addView(NEWG);


    }

    private void RefreshScore(){
        scorexttl.setText(""+xscore);
        scoreottl.setText(""+oscore);

    }

    private void toggleturn(){
        if(IsO){oturn.start();xturn.stop();}
        else {oturn.stop();xturn.start();}
    }


}
