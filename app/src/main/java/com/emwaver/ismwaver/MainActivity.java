package com.emwaver.ismwaver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.emwaver.ismwaver.databinding.ActivityMainBinding;
import com.emwaver.ismwaver.ui.console.ConsoleViewModel;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private USBService USBService; // Hold a reference to the bound service
    private boolean isBound = false; // Track binding state

    private ServiceConnection usbServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            USBService.LocalBinder binder = (USBService.LocalBinder) iBinder;
            USBService = binder.getService();
            isBound = true;
            //Log.i("usbServiceConnection", "onServiceConnected");
            USBService.checkForConnectedDevices();
            // You can now directly call service methods if needed
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_console, R.id.navigation_packetmode, R.id.navigation_rawmode, R.id.navigation_overview, R.id.navigation_flash)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);


        Intent serviceIntent = new Intent(this, USBService.class);
        startService(serviceIntent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.i("onResume", "registerReceiver");
        if (isBound) {
            // Check for already present devices & connect
            USBService.checkForConnectedDevices();
            //Log.i("onResume", "checkForConnectedDevices");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, USBService.class);
        bindService(intent, usbServiceConnection, Context.BIND_AUTO_CREATE);
        //Log.i("onStart", "bindService");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(usbServiceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


}