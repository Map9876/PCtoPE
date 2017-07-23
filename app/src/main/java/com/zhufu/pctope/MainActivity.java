package com.zhufu.pctope;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.BoolRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.makeText;


public class MainActivity extends AppCompatActivity {

    private List<Textures> texturesList = new ArrayList<>();

    private void Choose(){
        makeText(MainActivity.this, R.string.choosing_alert, Toast.LENGTH_SHORT).show();
        Intent choose = new Intent(Intent.ACTION_GET_CONTENT);
        choose.setType("*/*");
        choose.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(choose, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 1) {
                Uri uri = data.getData();
                String realPath = GetPathFromUri4kitkat.getPath(MainActivity.this,uri);
                Intent intent = new Intent(MainActivity.this,ConversionActivity.class);
                intent.putExtra("filePath",realPath);
                startActivity(intent);
            }
        }
        else{
            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            Snackbar.make(fab,R.string.choosing_none,Snackbar.LENGTH_LONG).show();
        }
    }

    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS, Manifest.permission.READ_EXTERNAL_STORAGE};
    Intent intent = getIntent();
    boolean isgranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //defining
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActivityCollector.addActivity(MainActivity.this);
        Intent intent = getIntent();
        isgranted = intent.getBooleanExtra("isgranted", true);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        Log.d("status", isgranted + "");
        //file choosing
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            Choose();
            }
        });
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent intent = new Intent(MainActivity.this,ConversionActivity.class);
                startActivity(intent);
                return false;
            }
        });

        final RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycle_view);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState>0){
                    fab.hide();
                }
                else {
                    fab.show();
                }
            }
        });

        //for swipe refresh layout
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.google_green),getResources().getColor(R.color.google_blue)
        ,getResources().getColor(R.color.google_red),getResources().getColor(R.color.google_yellow));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final Animation hide = AnimationUtils.loadAnimation(MainActivity.this,R.anim.cards_hide);
                                final Animation show = AnimationUtils.loadAnimation(MainActivity.this,R.anim.cards_show);

                                recyclerView.startAnimation(hide);
                                hide.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        AllInOne();
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                });

                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                    }
                }).start();
            }
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        Intent intent = getIntent();
        isgranted = intent.getBooleanExtra("isgranted", true);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (isgranted){
            AllInOne();
        }
        else {
            Snackbar.make(fab, R.string.permissions_request, Snackbar.LENGTH_LONG)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
                            class WaitTask extends AsyncTask<Void,Integer,Boolean>{
                                @Override
                                protected Boolean doInBackground(Void... params) {
                                    while (ContextCompat.checkSelfPermission(MainActivity.this,permissions[0]) == PackageManager.PERMISSION_DENIED){}
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Boolean result){
                                    AllInOne();
                                }
                            }
                            new WaitTask().execute();
                        }
                    }).show();
        }


    }

    private void AllInOne(){
        loadList();
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recycle_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        TextureItems items = new TextureItems(texturesList);
        recyclerView.setAdapter(items);
    }

    private void loadList(){
        texturesList.clear();
        File packsListDir = new File(Environment.getExternalStorageDirectory()+"/games/com.mojang/resource_packs/")
                ,packsList[] = null;
        if (packsListDir.exists()) packsList = packsListDir.listFiles();
        else packsListDir.mkdir();
        Textures textures[] = new Textures[packsList.length];
        for (int i=0;i<packsList.length;i++){
            if (packsList[i].exists())
                if (packsList[i].isDirectory()){
                    textures[i] = new Textures(packsList[i]);
                    texturesList.add(textures[i]);
                }
        }
    }
}
