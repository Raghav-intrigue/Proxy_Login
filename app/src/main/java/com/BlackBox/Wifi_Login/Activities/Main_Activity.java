package com.BlackBox.Wifi_Login.Activities;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.BlackBox.Wifi_Login.Classes.Connection_Detector;
import com.BlackBox.Wifi_Login.Classes.Login_Task;
import com.BlackBox.Wifi_Login.Classes.User_Cred;
import com.BlackBox.Wifi_Login.R;
import com.BlackBox.Wifi_Login.Services.BackgroundService;
import com.android.volley.toolbox.Volley;

//import android.util.Log;

public class Main_Activity extends AppCompatActivity {

    //final public String TAG = Main_Activity.class.getSimpleName() + " YOYO";
    final public static String URL = "https://hanuman.iiti.ac.in:8003/index.php?zone=lan_iiti";
    //final public static String URL = "http://httpbin.org/post";

    private TextInputEditText eT_UserName;
    private TextInputEditText eT_Password;
    private CheckBox cB_saveCred;
    private CheckBox cB_startService;
    private User_Cred user;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        Button btn_Login = (Button) findViewById(R.id.btn_Login);
        eT_UserName = (TextInputEditText) findViewById(R.id.eT_UserName);
        eT_Password = (TextInputEditText) findViewById(R.id.eT_Password);
        cB_saveCred = (CheckBox) findViewById(R.id.cB_saveCred);
        cB_startService = (CheckBox) findViewById(R.id.cB_startService);
        context = getApplicationContext();

        user = new User_Cred();
        if (user.load_Cred(context)) //if cred are saved
        {
            eT_UserName.setText(user.getID());
            eT_Password.setText(user.getpwd());
            cB_saveCred.setChecked(true);
        }

        btn_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Login();
            }
        });

        cB_startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cB_startService.isChecked()) {
                    if (!cB_saveCred.isChecked()) {
                        Toast.makeText(context, "You need to remember password to automatically login.", Toast.LENGTH_SHORT).show();
                        cB_saveCred.setChecked(true);
                    }
                }
            }
        });
    }

    private void Login() {
        //Log.i(TAG, "Login");

        Connection_Detector cd = new Connection_Detector(context);

        user.setID(eT_UserName.getText().toString().trim());
        user.setpwd(eT_Password.getText().toString().trim());

        if (user.getID().equals(""))
        {
            eT_UserName.setError("Field Empty");
        }
        else if (user.getpwd().equals(""))
        {
            eT_Password.setError("Field Empty");
        }
        else
        {
            int Con_Status = cd.isConnectedToWifi();
            String Alert_Status = "No connectivity";
            switch (Con_Status) {
                case 1:
                    Alert_Status = "Please turn on Wifi.";
                    break;
                case 2:
                    Alert_Status = "Please turn off Mobile data.";
                    break;
                case 3:
                    Alert_Status = "You are not on an IITI network.";
                    break;
            }
            if(Con_Status != 4) {
                showAlertDialog(Main_Activity.this,
                        "Error",
                        Alert_Status);
            }
            else
            {
                if (cB_saveCred.isChecked()) {
                    user.save_cred(context);
                } else {
                    user.clear_cred(context);
                }

                Toast.makeText(context, "Logging in...", Toast.LENGTH_LONG).show();

                Login_Task login_task = new Login_Task(user);

                login_task.Login(URL, context, Volley.newRequestQueue(context));

                IntentFilter intentFilter = new IntentFilter(Login_Task.ACTION_RESULT);
                registerReceiver(new MyOtherBroadcastReceiver(), intentFilter);

            }
        }
    }

    private void showAlertDialog(Context context, String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        //alertDialog.setIcon(R.mipmap.ic_app);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    class MyOtherBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean resultStatus = intent.getBooleanExtra("resultStatus", false);
            //Log.i("resultStatus: ", String.valueOf(resultStatus));

            if (resultStatus && cB_startService.isChecked()) {
                if (cB_saveCred.isChecked()) {
                    // Log.i(TAG, "Starting Service..");
                    Intent i = new Intent(context, BackgroundService.class);
                    startService(i);
                } else {
                    Toast.makeText(context, "You need to remember password to automatically login.", Toast.LENGTH_SHORT).show();
                    cB_startService.setChecked(false);
                }
            }

            unregisterReceiver(this);
        }
    }

}