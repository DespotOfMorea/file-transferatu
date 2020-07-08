package org.vnuk.filetransferatu;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final int PERMISSIONS_REQUEST = 7652;
    private final int REQUEST_CODE_SD_PATH = 4517;
    private final int REQUEST_CODE_FOLDER_PATH = 4518;
    private static final String ROOT_PATH = "/storage";
    private static final String INTERNAL_COMPARE = "primary";

    private final int PORT = 2121;
    private final String USER = "admin";
    private final String PASS = "admin";
    private final String FILE_NAME = "users.properties";

    private ToggleButton tbStart;
    private TextView tvTitle;
    private TextView tvAddress;
    private ImageView ivInternal;
    private ImageView ivSDCard;
    private ImageView ivDir;

    private String userName;
    private String password;
    private String path;

    private FtpServerFactory serverFactory = new FtpServerFactory();
    private ListenerFactory listenerFactory = new ListenerFactory();
    private PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
    private FtpServer mServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();
        mServer = serverFactory.createServer();
        userName = USER;
        password = PASS;
        path = Environment.getExternalStorageDirectory().getPath();

        tbStart = findViewById(R.id.tb_server);
        tbStart.setOnCheckedChangeListener(createStartButtonListener());
        tvTitle = findViewById(R.id.tv_server_title);
        tvAddress = findViewById(R.id.tv_server_address);
        setupButtons();
    }

    @Override
    protected void onDestroy() {
        try {
            mServer.stop();
        } catch (Exception e) {
            Log.e(TAG, "Server stopping error: " + e.getMessage());
        }
        super.onDestroy();
    }

    private void setupButtons() {
        ImageView ivWiFi = findViewById(R.id.iv_wifi);
        ImageView ivLogin = findViewById(R.id.iv_login);
        ImageView ivSettings = findViewById(R.id.iv_settings);
        ImageView ivCopy = findViewById(R.id.iv_copy);
        ImageView ivShare = findViewById(R.id.iv_share);
        ivInternal = findViewById(R.id.iv_internal);
        ivInternal.setEnabled(false);
        ivSDCard = findViewById(R.id.iv_sd_card);
        ivDir = findViewById(R.id.iv_dir);

        ivWiFi.setOnClickListener(createWiFiButtonListener());
        ivLogin.setOnClickListener(createLoginButtonListener());
        ivSettings.setOnClickListener(createSettingsButtonListener());
        ivCopy.setOnClickListener(createCopyButtonListener());
        ivShare.setOnClickListener(createShareButtonListener());
        ivInternal.setOnClickListener(createInternalButtonListener());
        ivSDCard.setOnClickListener(createSDCardButtonListener());
        ivDir.setOnClickListener(createDirButtonListener());
    }

    private View.OnClickListener createWiFiButtonListener() {
        return v -> {
            Log.v(TAG, "Opening device Wi-Fi settings.");
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent);
        };
    }

    private View.OnClickListener createLoginButtonListener() {
        return v -> {
            Log.v(TAG, "Change Login dialog started.");

            Dialog loginDialog = new Dialog(this);
            loginDialog.setContentView(R.layout.login_layout);

            Button btnCancel = loginDialog.findViewById(R.id.btn_login_cancel);
            btnCancel.setOnClickListener(v1 -> loginDialog.dismiss());
            Button btnSave = loginDialog.findViewById(R.id.btn_login_save);
            btnSave.setOnClickListener(v1 -> saveNewLogin(loginDialog));

            loginDialog.show();
        };
    }

    private View.OnClickListener createSettingsButtonListener() {
        return v -> {
            //Toast.makeText(getBaseContext(), getText(R.string.address_copied), Toast.LENGTH_LONG).show();
            //Log.v(TAG, "Server address copied to clipboard.");
        };
    }

    private View.OnClickListener createCopyButtonListener() {
        return v -> {
            String msg = String.valueOf(tvAddress.getText());
            ClipboardManager clipboard = (ClipboardManager) getBaseContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("simple text", msg);
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);

            Toast.makeText(getBaseContext(), getText(R.string.address_copied), Toast.LENGTH_LONG).show();
            Log.v(TAG, "Server address copied to clipboard.");
        };
    }

    private View.OnClickListener createShareButtonListener() {
        return v -> {
            Log.v(TAG, "Server address sharing started.");

            String msg = String.valueOf(tvAddress.getText());

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        };
    }

    private View.OnClickListener createInternalButtonListener() {
        return v -> {
            Log.v(TAG, "Set path for internal storage.");
            ivInternal.setAlpha(0.25f);
            ivSDCard.setAlpha(1f);
            ivDir.setAlpha(1f);
            ivInternal.setEnabled(false);
            ivSDCard.setEnabled(true);
            ivDir.setEnabled(true);
            path = Environment.getExternalStorageDirectory().getPath();
            configChange();
        };
    }

    private View.OnClickListener createSDCardButtonListener() {
        return v -> {
            Log.v(TAG, "Set path for SD card.");
            ivInternal.setAlpha(1f);
            ivSDCard.setAlpha(0.25f);
            ivDir.setAlpha(1f);
            ivInternal.setEnabled(true);
            ivSDCard.setEnabled(false);
            ivDir.setEnabled(true);

            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), REQUEST_CODE_SD_PATH);
        };
    }

    private View.OnClickListener createDirButtonListener() {
        return v -> {
            Log.v(TAG, "Set path for specific directory.");
            ivInternal.setAlpha(1f);
            ivSDCard.setAlpha(1f);
            ivDir.setAlpha(0.25f);
            ivInternal.setEnabled(true);
            ivSDCard.setEnabled(true);
            ivDir.setEnabled(false);

            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(Intent.createChooser(i, "Choose directory"), REQUEST_CODE_FOLDER_PATH);
        };
    }

    private CompoundButton.OnCheckedChangeListener createStartButtonListener() {
        return (buttonView, isChecked) -> {
            Log.v(TAG, "Server start/stop button activated.");
            try {
                if (checkWiFiState(this) || checkHotSpotState(this)) {
                    Log.i(TAG, "Wi-Fi or HotSPot is ON, starting server control.");
                    startServerControl();
                } else {
                    Log.i(TAG, "Wi-Fi or HotSpot is OFF.");
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.dialog_no_wifi_message).setTitle(R.string.dialog_no_wifi_title);
                    builder.setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
                    builder.show();
                    tbStart.setChecked(false);
                }
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        };
    }

    void startServerControl() {
        if (mServer.isStopped()) {
            Log.i(TAG, "Server is stopped, starting server...");
            startingServer();
        } else if (mServer.isSuspended()) {
            Log.i(TAG, "Server is suspended, resuming server...");
            resumingServer();
        } else {
            Log.i(TAG, "Server is working, suspending server...");
            suspendingServer();
        }
    }

    private void startingServer() {
        String userValue = userName;
        String passValue = password;

        String userName = (userValue.equals("")) ? USER : userValue;
        String password = (passValue.equals("")) ? PASS : passValue;
        //String path = "";
        setupServer(userName, password, path);
        startSever();
    }

    private void setupServer(String userName, String password, String path) {
        listenerFactory.setPort(PORT);
        serverFactory.addListener("default", listenerFactory.createListener());

        File files = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + FILE_NAME);
        if (!files.exists()) {
            try {
                files.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Users file creation error: " + e.getMessage());
            }
        }

        userManagerFactory.setFile(files);
        userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());
        UserManager userManager = userManagerFactory.createUserManager();
        addNewUser(userName, password, path, userManager);

        Map<String, Ftplet> m = new HashMap<>();
        m.put("miaFtplet", new Ftplet() {
            @Override
            public void init(FtpletContext ftpletContext) throws FtpException {

            }

            @Override
            public void destroy() {

            }

            @Override
            public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
                return FtpletResult.DEFAULT;
            }

            @Override
            public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException, IOException {
                return FtpletResult.DEFAULT;
            }

            @Override
            public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
                return FtpletResult.DEFAULT;
            }

            @Override
            public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
                return FtpletResult.DEFAULT;
            }
        });
        serverFactory.setFtplets(m);
    }

    private void saveNewLogin(Dialog dialog) {
        EditText etUser = dialog.findViewById(R.id.tiet_username);
        EditText etPass = dialog.findViewById(R.id.tiet_password);

        userName = String.valueOf(etUser.getText());
        password = String.valueOf(etPass.getText());
        dialog.dismiss();

        configChange();
    }

    private void addNewUser(String userName, String password, String path, UserManager userManager) {
        BaseUser newUser = new BaseUser();
        newUser.setName(userName);
        newUser.setPassword(password);
        newUser.setHomeDirectory(path);

        List<Authority> authorities = new ArrayList<>();
        Authority auth = new WritePermission();
        authorities.add(auth);
        newUser.setAuthorities(authorities);

        try {
            userManager.save(newUser);
        } catch (FtpException e) {
            Log.e(TAG, "Saving User error: " + e.getMessage());
        }

        serverFactory.setUserManager(userManager);
    }

    private void startSever() {
        try {
            mServer.start();
            setUI(true);
        } catch (FtpException e) {
            e.printStackTrace();
        }
    }

    private void resumingServer() {
        mServer.resume();
        setUI(true);
    }

    private void suspendingServer() {
        mServer.suspend();
        setUI(false);
    }

    private void setUI(boolean isServerON) {
        if (isServerON) {
            tvTitle.setText(getText(R.string.server_online));
            tvTitle.setTextColor(getResources().getColor(R.color.colorServerON, null));
            tvAddress.setText(String.format("ftp://%s:%s", wiFiIpAddress(this), PORT));
            System.out.println("ADDRESS: " + String.format("ftp://%s:%s", wiFiIpAddress(this), PORT));
        } else {
            tvTitle.setText(getText(R.string.server_offline));
            tvTitle.setTextColor(getResources().getColor(R.color.colorServerOFF, null));
            tvAddress.setText("");
        }
    }

    private void configChange() {
        UserManager userManager = serverFactory.getUserManager();
        addNewUser(userName, password, path, userManager);
        tbStart.setChecked(false);
    }

    private boolean checkWiFiState(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager == null) return false;

        if (wifiManager.isWifiEnabled()) {
            //The networkId may be -1 if there is no currently connected network or
            // if the caller has insufficient permissions to access the network ID.
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Log.v(TAG, "Wi-Fi is ON and NetworkID is " + wifiInfo.getNetworkId() + ".");
            //From Android 10/Q WifiInfo.getNetworkId() needs location permission and location mode turned on
            //or will return -1.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return true;
            } else {
                return wifiInfo.getNetworkId() != -1;
            }
        } else {
            Log.v(TAG, "Wi-Fi is OFF.");
            return false;
        }
    }

    private boolean checkHotSpotState(Context context) throws InvocationTargetException, IllegalAccessException {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null)
            return false;
        Method method;
        try {
            method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true); //in the case of visibility change in future APIs
            return (Boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String wiFiIpAddress(Context context) {
        try {
            if (checkHotSpotState(context)) {
                return "192.168.43.1";
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return getIPAddress(true);
    }

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        // boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.dialog_storage_message).setTitle(R.string.dialog_storage_title);
                builder.setPositiveButton("OK", (dialog, id) -> {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SD_PATH) {
            if (data != null) {
                Uri uri = data.getData();
                String dirName = uri.getLastPathSegment().split(":")[0];
                path = ROOT_PATH + File.separator + dirName;
                configChange();
            }
        } else if (requestCode == REQUEST_CODE_FOLDER_PATH) {
            if (data != null) {
                Uri uri = data.getData();
                String root = uri.getLastPathSegment().split(":")[0];
                String dirName = uri.getLastPathSegment().split(":")[1];
                if (root.equals(INTERNAL_COMPARE)) {
                    path = Environment.getExternalStorageDirectory().getPath() + File.separator + dirName;
                } else {
                    path = ROOT_PATH + File.separator + root + File.separator + dirName;
                }
                configChange();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.dialog_storage_denied_message).setTitle(R.string.dialog_storage_denied_title);
                builder.setPositiveButton("OK", (dialog, id) -> {
                    dialog.dismiss();
                    finish();
                });
                builder.show();
            }
        }
    }
}