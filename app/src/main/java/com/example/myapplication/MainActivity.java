package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.bson.Document;

import java.text.DateFormat;
import java.util.Date;

import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.RealmResultTask;import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

public class MainActivity extends AppCompatActivity {
    String AppID = "application-0-keuuu";
    String assetLocation;
    private App app;
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    TextView textView;
    FusedLocationProviderClient fusedLocationProviderClient;
    private Handler mHandler = new Handler();
    private Runnable trackingTask = new Runnable() {
        @Override
        public void run() {
            getLocation();
            User user = app.currentUser();
            mongoClient = user.getMongoClient("mongodb-atlas");
            mongoDatabase = mongoClient.getDatabase("myFirstDatabase");
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("students");
            String mongoUserId = user.getId();
            String date = DateFormat.getDateTimeInstance().format(new Date());
            Document queryFilter = new Document().append("userid",mongoUserId);
            RealmResultTask<MongoCursor<Document>> findTask = mongoCollection.find(queryFilter).iterator();
            findTask.getAsync(task -> {
                if(task.isSuccess())
                {
                    MongoCursor<Document> results = task.get();
                    if(results.hasNext())
                    {
                        Log.v("Find Function","Found Something");
                        Document result = results.next();
                        String data = assetLocation;
                        result.append("data",data).append("date",date);
                        mongoCollection.updateOne(queryFilter,result).getAsync(result1 -> {
                            if (result1.isSuccess())
                            {
                                Log.v("Update Function","Updated Data");
                                Toast.makeText(MainActivity.this, "Updated Data", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Log.v("Update Function","Error"+result1.getError().toString());
                                Toast.makeText(MainActivity.this, "Error"+result1.getError().toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    else
                    {
                        String data = assetLocation;
                        Log.v("Find Function","Found Nothing");
                        mongoCollection.insertOne(new Document().append("userid",mongoUserId).append("data",data).append("date",date)).getAsync(result -> {
                            if(result.isSuccess())
                            {
                                Log.v("Insert Function","Inserted Data");
                                Toast.makeText(MainActivity.this, "Inserted Data", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Log.v("Insert Function","Error"+result.getError().toString());
                                Toast.makeText(MainActivity.this, "Error"+result.getError().toString(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }else {
                    Log.v("Error",task.getError().toString());
                }
            });
            mHandler.postDelayed(this, 60000);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.findData);
        ToggleButton toggle = (ToggleButton) findViewById(R.id.togglebutton);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(MainActivity.this
                , Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this
                    , new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }

        Realm.init(getApplicationContext());
        app = new App(new AppConfiguration.Builder(AppID).build());
        Credentials credentials = Credentials.emailPassword("a@a","123456");
        app.loginAsync(credentials, new App.Callback<User>() {
            @Override
            public void onResult(App.Result<User> result) {
                if(result.isSuccess())
                {
                    Log.v("User","Successfully authenticated using an email and password.");
                    Toast.makeText(MainActivity.this, "Successfully authenticated using an email and password.", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Log.v("Use","Failed to Login");
                    Toast.makeText(MainActivity.this, "Failed to Login", Toast.LENGTH_SHORT).show();
                }
            }
        });
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    trackingTask.run();
                } else {
                    mHandler.removeCallbacks(trackingTask);
                }
            }
        });
    }
    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {
                    assetLocation = location.getLatitude()+", "+location.getLongitude();
                    textView.setText(assetLocation);
                    Toast.makeText(MainActivity.this, "Location: "+assetLocation, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}