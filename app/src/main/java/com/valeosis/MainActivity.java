package com.valeosis;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.location.LocationManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.airbnb.lottie.LottieAnimationView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.valeosis.activities.SettingContainerActivity;
import com.valeosis.database.SQLiteDatabaseHandler;
import com.valeosis.database.SharedPreference;
import com.valeosis.helper.Connection;

import com.valeosis.helper.MyKeyboard;
import com.valeosis.pojo.BranchData;
import com.valeosis.pojo.FaceData;
import com.valeosis.pojo.FaceImgData;
import com.valeosis.service.BluetoothService;
import com.valeosis.service.WebService;
import com.valeosis.utility.Utility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    TextView reco_name, preview_info, textAbove_preview, bluetoothTxt;
    ProcessCameraProvider cameraProvider;
    FaceDetector detector;
    CameraSelector cameraSelector;
    Interpreter tfLite;
    int[] intValues;
    public BluetoothDevice mBTDevice;
    String TAG = "MainActivity";
    BluetoothService bluetoothService = null;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    boolean start = true, flipX = false;
    String userName = "";
    int inputSize = 112;  //Input size for model
    boolean isModelQuantized = false;
    float[][] embeedings;
    float IMAGE_MEAN = 128.0f;
    float IMAGE_STD = 128.0f;
    int OUTPUT_SIZE = 192; //Output size of model
    Button setting;
    String modelFile = "mobile_face_net.tflite";
    int cam_face = CameraSelector.LENS_FACING_FRONT;
    PreviewView previewView;
	

    ImageView face_preview;
    FrameLayout frameLayout;
    String username = "";
    String deviceName;
    public static List<FaceData> faceDataList = new ArrayList<>();
    SQLiteDatabaseHandler db;
    public static Handler userTimeHandler = null;
    public static boolean isUserFound = false;
    Button homeBtn;
    Context context;
    EditText editText;
    String[] data = {"FON", ""};
    public BluetoothAdapter mBluetoothAdapter;
    public static MainActivity mainActivity;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_PRIVILEGED,};
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_PRIVILEGED};

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {

            db = new SQLiteDatabaseHandler(this);
            userName = "";
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            context = getApplicationContext();
            mainActivity = this;

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            deviceName = SharedPreference.getBluetoothDeviceName(context);

            MyKeyboard keyboard = (MyKeyboard) findViewById(R.id.keyboard);
            editText = (EditText) findViewById(R.id.editText);
            editText.setRawInputType(InputType.TYPE_CLASS_TEXT);
            editText.setTextIsSelectable(true);
            editText.setOnTouchListener(otl);

            InputConnection ic = editText.onCreateInputConnection(new EditorInfo());
            keyboard.setInputConnection(ic);

            face_preview = findViewById(R.id.imageView);
            homeBtn = findViewById(R.id.homeBtn);
            setting = findViewById(R.id.setting);

            previewView = findViewById(R.id.previewView);

           frameLayout = findViewById(R.id.previewViewFrameLayout);
		   
            reco_name = findViewById(R.id.textView);

            preview_info = findViewById(R.id.textView2);

            textAbove_preview = findViewById(R.id.textAbovePreview);
            bluetoothTxt = findViewById(R.id.bluetoothTxt);

            textAbove_preview.setText("Face Preview: ");

            bluetoothTxt.setText("Bluetooth : Off || Device name : " + deviceName);
            if (mBluetoothAdapter.isEnabled()) {
                Utility.isBlutoothEnabled = true;
                bluetoothTxt.setTextColor(context.getColor(R.color.SuccessColor));
                bluetoothTxt.setText(bluetoothTxt.getText().toString().replaceAll("Off", "On"));
            }

            preview_info.setText("1.Bring Face in view of Camera.\n\n2.Your Face preview will appear here.");

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                    try {
                        Log.d("Editable", s.toString());
                        if (!Utility.isBlutoothEnabled && s.toString().length() == 3) {
                            Utility.showToast(context, "Please turn on bluetooth !!");
                            return;
                        }
                        if (s.toString().length() == 3) {
                            data[0] = "FON";
                            data[1] = "";
                            new AsyncCallForBluetooth().execute(data);
                        }
                        if (s.toString().length() >= 3) {
                            username = s.toString();
                            refreshCamera();
                        } else {
                            start = false;
                            if (cameraProvider != null) {
                                cameraProvider.unbindAll();
                                previewView.setVisibility(View.GONE);
                                frameLayout.setVisibility(View.GONE);
								
							
                                findViewById(R.id.relativeLayout).setVisibility(View.VISIBLE);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            getPermissionsForBluetooth();
            checkBTPermissions();
            //enableDisableBT();
            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);

            tfLite = new Interpreter(Utility.loadModelFile(MainActivity.this, modelFile));

            FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).enableTracking().build();
                detector = FaceDetection.getClient(highAccuracyOpts);



            setting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utility.vibrateWithAnim(v);
                    startActivity(new Intent(getApplicationContext(), SettingContainerActivity.class));
                }
            });

            cameraBind();
            generateQR();

            homeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Utility.vibrateWithAnim(v);
                    editText.setText("");
                    start = false;
                    cameraProvider.unbindAll();
                   /* if (BluetoothCommunication.mBluetoothConnection != null)
                        BluetoothCommunication.mBluetoothConnection.write("TEST".getBytes(Charset.defaultCharset()));
                   */
                }
            });

/*
            camera_switch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (cam_face == CameraSelector.LENS_FACING_BACK) {
                        cam_face = CameraSelector.LENS_FACING_FRONT;
                        flipX = true;
                    } else {
                        cam_face = CameraSelector.LENS_FACING_BACK;
                        flipX = false;
                    }
                    cameraProvider.unbindAll();
                    cameraBind();
                }
            });
*/
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    private void cameraBind() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        previewView = findViewById(R.id.previewView);

        cameraProviderFuture.addListener(() -> {

            try {

                cameraProvider = cameraProviderFuture.get();

                bindPreview(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {

            }

        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        try {

            Preview preview = new Preview.Builder()

                    .build();

            cameraSelector = new CameraSelector.Builder().requireLensFacing(cam_face).build();

            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(640, 480)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
                    .build();

            Executor executor = Executors.newSingleThreadExecutor();

            imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
                @Override
                public void analyze(@NonNull ImageProxy imageProxy) {

                    try {
                        Thread.sleep(0);  //Camera preview refreshed every 10 millisec(adjust as required)
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    InputImage image = null;

                    @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();

                    if (mediaImage != null) {

                        image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                    }

                    Task<List<Face>> result = detector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                                @Override
                                public void onSuccess(List<Face> faces) {
									
								

                                    if (faces.size() != 0) {

                                        Face face = faces.get(0); //Get first face from detected faces

                                        Bitmap frame_bmp = Utility.toBitmap(mediaImage);

                                        int rot = imageProxy.getImageInfo().getRotationDegrees();

                                        Bitmap frame_bmp1 = Utility.rotateBitmap(frame_bmp, rot, false, false);

                                        RectF boundingBox = new RectF(face.getBoundingBox());

                                        Bitmap cropped_face = Utility.getCropBitmapByCPU(frame_bmp1, boundingBox);

                                        if (flipX)
                                            cropped_face = Utility.rotateBitmap(cropped_face, 0, flipX, false);
                                        Bitmap scaled = Utility.getResizedBitmap(cropped_face, 112, 112);

                                        if (start) {
                                            try {
                                                recognizeImage(scaled, username);
                                            } catch (JsonProcessingException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }



                                    }
                                }
                            })

                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                }
                            })

                            .addOnCompleteListener(new OnCompleteListener<List<Face>>() {
                                @Override
                                public void onComplete(@NonNull Task<List<Face>> task) {

                                    imageProxy.close(); //v.important to acquire next frame for analysis

                                }
                            });

                }
            });

            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);

        } catch (Exception e) {

        }
    }


    @Override
    protected void onResume() {
        try {
            super.onResume();
            generateQR();
            String dname = SharedPreference.getBluetoothDeviceName(context);
            bluetoothTxt.setText(bluetoothTxt.getText().toString().replaceAll(deviceName, dname));
            deviceName = dname;
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, Utility.MY_CAMERA_REQUEST_CODE);
            }
            if (!Connection.isOnline(context))
                Utility.showToast(context, "Please turn on internet connection !!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getBirthOrAnniversaryMsg(JSONObject jsonObject) {
        String birthDate = "";
        String anniversaryDate = "";
        try {
            birthDate = jsonObject.getString("BirthDate");
            anniversaryDate = jsonObject.getString("AnniversaryDate");

            java.sql.Date date1 = new java.sql.Date((new Date()).getTime());

            SimpleDateFormat formatNowDay = new SimpleDateFormat("dd");
            SimpleDateFormat formatNowMonth = new SimpleDateFormat("MM");
            String currentDay = formatNowDay.format(date1);
            String currentMonth = formatNowMonth.format(date1);

            if (birthDate.split("/")[0].equalsIgnoreCase(currentDay) && birthDate.split("/")[1].equalsIgnoreCase(currentMonth)) {
                return "Happy Birthday";
            } else if (anniversaryDate.split("/")[0].equalsIgnoreCase(currentDay) && anniversaryDate.split("/")[1].equalsIgnoreCase(currentMonth)) {
                return "Happy Anniversary";
            }

            Log.d("Date>>>", "dd >>" + currentDay + ">>mm >>" + currentMonth);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void showBirthLayout(final View view, String msg, int dialogTime) {
        TextView birthTxt;
        ImageView anniversaryImg;
        Button cancel;
        LottieAnimationView happyBirthdayAnim;

        try {
            LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(view.getContext().LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.birthaday_annivarsary_dailog, null);
            int width = LinearLayout.LayoutParams.MATCH_PARENT;
            int height = LinearLayout.LayoutParams.WRAP_CONTENT;
            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
            popupWindow.setOnDismissListener(null);

            anniversaryImg = popupView.findViewById(R.id.anniversaryImg);
            happyBirthdayAnim = popupView.findViewById(R.id.happyBirthdayAnim);
            birthTxt = popupView.findViewById(R.id.birthTxt);
            cancel = popupView.findViewById(R.id.cancel);

            Log.d("UserImg", new Gson().toJson(Utility.currentLoginUser));
            if (msg.contains("Birthday")) {
                final MediaPlayer hbd;
                hbd = MediaPlayer.create(this, R.raw.happybirthday);
                hbd.start();

                happyBirthdayAnim.setVisibility(View.VISIBLE);
                anniversaryImg.setVisibility(View.GONE);
            } else if (msg.contains("Anniversary")) {
                anniversaryImg.setVisibility(View.VISIBLE);
                happyBirthdayAnim.setVisibility(View.GONE);
            }

            birthTxt.setText(msg + "\n" + Utility.currentLoginUser.getName() + " !");
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cameraProvider.unbindAll();
                    popupWindow.dismiss();
                }
            });
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    cameraProvider.unbindAll();
                    popupWindow.dismiss();
                }
            }, dialogTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateQR() {

        BranchData branchData = SharedPreference.getBranchDetails(getApplicationContext());

        if (branchData != null) {

            if (branchData != null) {
                String Qr = branchData.getBranchno() + "##" + Utility.getDateInDDMMYY();
                MultiFormatWriter mWriter = new MultiFormatWriter();
                try {
                    ImageView imageCode = findViewById(R.id.qr);
                    BitMatrix mMatrix = mWriter.encode(Qr, BarcodeFormat.QR_CODE, 400, 400);
                    BarcodeEncoder mEncoder = new BarcodeEncoder();
                    Bitmap mBitmap = mEncoder.createBitmap(mMatrix);//creating bitmap of code
                    imageCode.setImageBitmap(mBitmap);//Setting generated QR code to imageView
                    imageCode.setVisibility(View.VISIBLE);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    public void recognizeImage(final Bitmap bitmap, String userName) throws JsonProcessingException {

        face_preview.setImageBitmap(bitmap);

        ByteBuffer imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);

        imgData.order(ByteOrder.nativeOrder());

        intValues = new int[inputSize * inputSize];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();

        for (int i = 0; i < inputSize; ++i) {

            for (int j = 0; j < inputSize; ++j) {

                int pixelValue = intValues[i * inputSize + j];

                if (isModelQuantized) {

                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));

                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));

                    imgData.put((byte) (pixelValue & 0xFF));

                } else { // Float model

                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);

                }
            }
        }

        Object[] inputArray = {imgData};

        Map<Integer, Object> outputMap = new HashMap<>();

        embeedings = new float[1][OUTPUT_SIZE];

        outputMap.put(0, embeedings);

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap); //Run model

        //float distance_local = Float.MAX_VALUE;

        final List<Pair<String, Float>> nearest = findNearest(embeedings[0], userName);  //finding the matching face

        Log.d("nearest", nearest.toString());


        // using nearest(0) for finalizing recognized face, nearest(0) is the nearest as the distance is the shortest
        if (nearest.size() > 0 && nearest.get(0) != null) {

            Pair<String, Float> Recognised_pair = nearest.get(0);
            Log.d("Nearest", "Member ID: " + Recognised_pair.first + ", Distance: " + Recognised_pair.second);

            List<FaceData> FaceRecognisedlist = new ArrayList<FaceData>();
            FaceRecognisedlist = db.getDetailsById(Recognised_pair.first);

            FaceData facerecognised = FaceRecognisedlist.get(0);
            Utility.currentLoginUser = facerecognised;

            Utility.FACE_DISTANCE = SharedPreference.getHyperParameter(context);
            if (Recognised_pair.second < Utility.FACE_DISTANCE) {
                Log.d("hyperparameter", String.valueOf(SharedPreference.getHyperParameter(context)));
                if (Utility.currentLoginUser != null) {
                    final String name = Recognised_pair.first;
                    Log.d("Utility.currentLoginUser >>", Utility.currentLoginUser.toString());
                    checkValidUser(Utility.currentLoginUser.getMemberID(), Utility.currentLoginUser.getMemberID(), getWindow().getDecorView().getRootView());

                }
            }

        }
    }

    private List<Pair<String, Float>> findNearest(float[] emb, String userName) throws JsonProcessingException {
        List<Pair<String, Float>> neighbour_list = new ArrayList<>();

        faceDataList = db.getFaceByName(userName);

        //loop to iterate over all the available members with the String userName to get their member ID and further loop to iterate over their faceimages
        //As FaceImgData class stores image data with their member ID

        for (FaceData faceData : faceDataList) {
            final String memberID = faceData.getMemberID();
            LinkedList<FaceImgData> faceImgDataList = db.getImageDataById(memberID);

            //faceImgData.getIsSelected() gets set to 1 when faceImgDataList is initialised
            for (FaceImgData faceImgData : faceImgDataList) {
                if (faceImgData.getIsSelected() == 1) {
                    Object extraObject = faceImgData.getExtra();

                    final float[] knownEmb = (Utility.convertStringTo2DArray(new Gson().toJson(extraObject)))[0];

                    float distance = 0;
                    //calculating the distance between the known embedding and live embeddings
                    for (int i = 0; i < emb.length; i++) {
                        float diff = emb[i] - knownEmb[i];
                        distance += diff * diff;
                    }
                    distance = (float) Math.sqrt(distance);

                    //Utility.currentLoginUser = faceData;
                    System.out.println("Distance between input face and known face: " + distance);
                    System.out.println("Known face embedding: " + Arrays.toString(knownEmb));
                    System.out.println("Input face embedding: " + Arrays.toString(emb));

                    neighbour_list.add(new Pair<>(memberID, distance));
                }
            }
        }

        //Sorting the list based on the distance using comparator method
        Collections.sort(neighbour_list, new Comparator<Pair<String, Float>>() {
            @Override
            public int compare(Pair<String, Float> o1, Pair<String, Float> o2) {
                return Float.compare(o1.second, o2.second);
            }
        });

        return neighbour_list;
    }




    public void refreshCamera() {
        isUserFound = false;
        isUserFound();

        if (username.length() >= 3) {
            username = editText.getText().toString().trim();
            start = true;
            previewView.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.VISIBLE);
            findViewById(R.id.relativeLayout).setVisibility(View.GONE);
           
            face_preview.setVisibility(View.VISIBLE);
            cameraProvider.unbindAll();
            cameraBind();

        }
    }

    public void openPopupWindow(final View view, JSONObject jsonObject) {

        String memberNo;

        try {

            isUserFound = true;
            start = false;
            cameraProvider.unbindAll();

            int dialogTime = SharedPreference.getDialogTimer(getApplicationContext());
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    String msg = getBirthOrAnniversaryMsg(jsonObject);
                    if (msg.trim().length() > 0)
                        showBirthLayout(view, msg, dialogTime);
                }
            }, 1000);

            if (Utility.currentLoginUser.getType().equalsIgnoreCase("Member")) {
                memberNo = jsonObject.getString("MemberNo");
            } else {
                memberNo = jsonObject.getString("EmpNo");
            }

            //   save attendance and call bluetooth service
            Log.d("memberNo", memberNo);
            if (memberNo.trim().length() > 0) {
                data[0] = "ON";
                data[1] = memberNo;
                new AsyncCallForBluetooth().execute(data);
            }

            TextView nameTxt, accTxt, membershipTxt;

            LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(view.getContext().LAYOUT_INFLATER_SERVICE);

            View popupView = inflater.inflate(R.layout.show_details_layout, null);

            int width = LinearLayout.LayoutParams.MATCH_PARENT;

            int height = LinearLayout.LayoutParams.MATCH_PARENT;

            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

            final MediaPlayer welcome_media;
            welcome_media = MediaPlayer.create(this, R.raw.welcome);

            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

            if (!jsonObject.getString("Active").equalsIgnoreCase("No") || !jsonObject.getString("Membershipstatus").equalsIgnoreCase("Expired")) {
                welcome_media.start();
            }

            Button cancel = popupView.findViewById(R.id.cancel);
            TextView userName = popupView.findViewById(R.id.userName);
            ImageView userImg = popupView.findViewById(R.id.userImg);
            TextView planName = popupView.findViewById(R.id.planName);
            TextView endDate = popupView.findViewById(R.id.endDate);
            TextView ProgramName = popupView.findViewById(R.id.ProgramName);

            LinearLayout empLayout = popupView.findViewById(R.id.empLayout);
            Button inBtn = popupView.findViewById(R.id.inBtn);
            Button outBtn = popupView.findViewById(R.id.outBtn);

            nameTxt = popupView.findViewById(R.id.nameTxt);
            accTxt = popupView.findViewById(R.id.accTxt);
            membershipTxt = popupView.findViewById(R.id.membershipTxt);

            if (Utility.currentLoginUser != null) {
                if (Utility.currentLoginUser.getType().equalsIgnoreCase("Employee")) {
                    empLayout.setVisibility(View.VISIBLE);
                    userName.setText(userName.getText().toString() + "\"" + jsonObject.getString("EmpName") + "\"");

                    nameTxt.setText(jsonObject.getString("EmpName"));
                    accTxt.setText(jsonObject.getString("Active"));
                    membershipTxt.setVisibility(View.GONE);

                    userImg.setImageBitmap(Utility.currentLoginUser.getUserImage());
                    planName.setVisibility(View.GONE);
                    endDate.setVisibility(View.GONE);
                    ProgramName.setVisibility(View.GONE);

                } else {
                    empLayout.setVisibility(View.GONE);
                    userName.setText(userName.getText().toString() + "\"" + jsonObject.getString("MemberName") + "\"");

                    nameTxt.setText(jsonObject.getString("MemberName"));
                    accTxt.setText(jsonObject.getString("Active"));
                    membershipTxt.setText(jsonObject.getString("Membershipstatus"));

                    userImg.setImageBitmap(Utility.currentLoginUser.getUserImage());

                    JSONObject planDetails = WebService.getPlanDetails(jsonObject.getString("MemberNo"), jsonObject.getString("Branchno")).getJSONObject(0);
                    if (planDetails.length() > 0) {
                        planName.setText(planName.getText().toString() + planDetails.getString("PlanName"));
                        endDate.setText(endDate.getText().toString() + planDetails.getString("EndDt"));
                        ProgramName.setText(ProgramName.getText().toString() + planDetails.getString("ProgramName"));
                    }
                    if (Utility.currentLoginUser.getType().equalsIgnoreCase("Member")) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                popupWindow.dismiss();
                            }
                        }, dialogTime);
                    }
                }
            }
            inBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AsyncCallForEmpService().execute(memberNo, "1");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bluetoothService.startBluetoothService("FOFF", "", "");
                        }
                    }, 1000);
                    popupWindow.dismiss();
                }
            });
            outBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AsyncCallForEmpService().execute(memberNo, "0");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bluetoothService.startBluetoothService("FOFF", "", "");
                        }
                    }, 1000);
                    popupWindow.dismiss();
                }
            });
            popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    bluetoothService.startBluetoothService("FOFF", "", "");
                    homeBtn.callOnClick();
                }
            });

            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cameraProvider.unbindAll();
                    popupWindow.dismiss();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean checkValidUser(String userName, String password, View view) {

        try {

            JSONArray jsonArray = WebService.getLoginData(userName, password, Utility.currentLoginUser.getType());

            if (jsonArray.length() > 0) {
                JSONObject jsonObject = jsonArray.getJSONObject(0);

                if (Utility.currentLoginUser.getType().equalsIgnoreCase("Member") && jsonObject != null && !jsonObject.getString("MemberNo").equalsIgnoreCase("No")) {
                    openPopupWindow(view, jsonObject);
                    start = false;
                } else if (Utility.currentLoginUser.getType().equalsIgnoreCase("Employee")) {
                    openPopupWindow(view, jsonObject);
                    start = false;
                }

                if (jsonObject.getString("Active").equalsIgnoreCase("No") || jsonObject.getString("Membershipstatus").equalsIgnoreCase("Expired")) {

                    start = false;
                    //Toast.makeText(this, "your account is not active try to connect with branch !!!", Toast.LENGTH_LONG).show();
                    showErrorWindow(view, jsonObject);

                    return false;
                }
                    /*

                } else if (false) {


                } else {
                    openPopupWindow(view, jsonObject);
                }*/

            }

        } catch (Exception e) {

            e.printStackTrace();
        }
        return true;
    }

    public void showErrorWindow(View view, JSONObject jsonObject) throws InterruptedException {

        final MediaPlayer error_audio, membership_expired_voice;
        error_audio = MediaPlayer.create(this, R.raw.error2);
        membership_expired_voice = MediaPlayer.create(this, R.raw.membershipexpiredfvoice); //error msg
        error_audio.start();
        Thread.sleep(1000);
        membership_expired_voice.start();

        int dialogTime = SharedPreference.getDialogTimer(getApplicationContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        final View customLayout = getLayoutInflater().inflate(R.layout.error_layout, null);
        builder.setView(customLayout);

        Button okButton = (Button) customLayout.findViewById(R.id.OkBtn);

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, dialogTime);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();

         }});



    }


    private void getPermissionsForBluetooth() {
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN);
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
        }
        if (permission2 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION, 1);
        }
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.e(TAG, " >>> Discovering devices !!!" + isGpsEnabled);
        if (!isGpsEnabled) {
            Utility.showToast(context, "Please Enabled location permission to discover devices !!");
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
        }
    }

    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        } else {
            Utility.showToast(context, "Android version is not supported for bluetooth !!");
            Log.d("checkBTPermissions", "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    private View.OnTouchListener otl = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            return true; // the listener has consumed the event
        }
    };

    public void isUserFound() {
        int time = 7000;  //Time camera stays open
        if (userTimeHandler == null) {
            userTimeHandler = new Handler();
        } else {
            userTimeHandler.removeCallbacksAndMessages(null);
        }
        userTimeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isUserFound && start) {
                    bluetoothService.startBluetoothService("FOFF", "", "");
                    Utility.showToast(context, "USER NOT FOUND !! \nPLEASE RETRY AGAIN !!");
                    findViewById(R.id.relativeLayout).setVisibility(View.VISIBLE);
                    homeBtn.callOnClick();
                }
            }
        }, time);

    }

    @SuppressLint("MissingPermission")
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                    bluetoothTxt.setTextColor(context.getColor(R.color.SuccessColor));
                    bluetoothTxt.setText("Bluetooth : On || Device name : " + deviceName);
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

/*
    public void enableDisableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);

        } else {
            bluetoothTxt.setTextColor(context.getColor(R.color.SuccessColor));
            bluetoothTxt.setText("Bluetooth : On || Device name : " + deviceName);
        }
    }
*/

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE OFF");
                        Utility.isBlutoothEnabled = false;
                        bluetoothTxt.setText("Bluetooth : off || Device name : " + deviceName);
                        bluetoothTxt.setTextColor(context.getColor(R.color.errorColor));
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Utility.isBlutoothEnabled = true;
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        bluetoothTxt.setTextColor(context.getColor(R.color.SuccessColor));
                        bluetoothTxt.setText("Bluetooth : On || Device name : " + deviceName);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private class AsyncCallForEmpService extends AsyncTask<String, Void, Void> {

        JSONArray jsonArray = null;

        @Override
        protected Void doInBackground(String... strings) {
            String memberNo = strings[0];
            String status = strings[1];
            jsonArray = WebService.saveEmpAttendance(memberNo, status);
            Log.d(TAG, "Response >>>" + new Gson().toJson(jsonArray));
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            if (jsonArray != null && jsonArray.length() > 0) {
                Log.d(TAG, ">>>> server response >>>" + jsonArray.toString());
                Utility.showToast(context, "Employee Attendance\nsaved successfully !!");
            }
        }
    }

    private class AsyncCallForBluetooth extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            String message = strings[0];
            String memberNo = strings[1];
            Log.d(TAG, ">>> message >>>" + message + ">>>> member no >>" + memberNo);
            bluetoothService = new BluetoothService(MainActivity.this);
            String type = Utility.currentLoginUser == null ? "" : Utility.currentLoginUser.getType();
            bluetoothService.startBluetoothService(message, memberNo, type);
            return null;
        }
    }

}