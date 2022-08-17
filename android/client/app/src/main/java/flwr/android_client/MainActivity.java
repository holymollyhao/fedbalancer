package flwr.android_client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import  flwr.android_client.FlowerServiceGrpc.FlowerServiceBlockingStub;
import  flwr.android_client.FlowerServiceGrpc.FlowerServiceStub;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private EditText numOfEpoch;
    private EditText numOfThread;
    private String latency_sampling_port;

    private boolean is_latency_sampling;

    private Button runThreadButton;
    private Button killThreadButton;
    private Button loadDataButton;
    private Button connectButton;
    private Button connectSampleLatencyButton;
    private Button trainButton;
    private Button initConfigButton;
    private Button logDataButton;
    private Button stopLogButton;

    private TextView resultText;
    private EditText device_id;
    private Spinner dataset_spinner;

    private ManagedChannel channel;
    public FlowerClient fc;
    private static String TAG = "Flower";
    public String dataset;

    private indefiniteThread runningThread;
    private volatile boolean isRunning[];
    private volatile boolean isLogging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FedBalancerSingleton.resetFedBalancerSingleton();

        try {
            Intent intent = this.getIntent();
            String experimentID = intent.getStringExtra("id");
            Log.e(TAG, experimentID);

            if (experimentID.equals("0")) {
                is_latency_sampling = false;
            } else {
                is_latency_sampling = true;
            }
        } catch (Exception e) {
            is_latency_sampling = false;
            e.printStackTrace();
        }

        resultText = (TextView) findViewById(R.id.grpc_response_text);
        resultText.setMovementMethod(new ScrollingMovementMethod());
        device_id = (EditText) findViewById(R.id.device_id_edit_text);

        numOfEpoch = (EditText) findViewById(R.id.num_of_epoch);
        numOfThread = (EditText) findViewById(R.id.num_of_threads);

        initConfigButton = (Button) findViewById(R.id.initialize_config);
        loadDataButton = (Button) findViewById(R.id.load_data);
        trainButton = (Button) findViewById(R.id.start_training);
        runThreadButton = (Button) findViewById(R.id.run_thread);
        killThreadButton = (Button) findViewById(R.id.kill_thread);
        logDataButton = (Button) findViewById(R.id.log_data);
        stopLogButton = (Button) findViewById(R.id.stop_log_data);

        loadDataButton.setEnabled(false);
        stopLogButton.setEnabled(false);
        isLogging = false;

        dataset_spinner = (Spinner) findViewById(R.id.datasets_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.dataset_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataset_spinner.setAdapter(adapter);

        Log.e(TAG, Build.MODEL);
        Util.killBackgroundProcess(this);

        // ForegroundService.startService(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        Util.killBackgroundProcess(this);
        IntentFilter completeFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//        registerReceiver(onCompleteHandler, completeFilter);
    }

    @Override
    public void onPause(){
        super.onPause();
//        unregisterReceiver(onCompleteHandler);
    }

    @Override
    public void onDestroy() {
        // For killing logging thread
        super.onDestroy();
        isLogging = false;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    public void setResultText(String text) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String time = dateFormat.format(new Date());
        runOnUiThread(new Runnable(){
            @Override public void run() {
                resultText.append("\n" + time + "   " + text);
            }
        });
    }

    public void downloadData(View view){
        dataset = dataset_spinner.getSelectedItem().toString();
        try {
            System.out.println(dataset);
            String urlString = String.format("http://143.248.36.213:8998/%s/", dataset);
            new DownloadAsyncTask(this).execute(urlString).get();
            new UnzipDataAsyncTask(this).execute(getFilesDir().getPath() + "/").get();
//            new UnzipDataAsyncTask(this).execute(getFilesDir().getPath() + "/","data.zip").get();
//            new UnzipDataAsyncTask(this).execute(getFilesDir().getPath() + "/","model.zip").get();

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void logData(View view){
        isLogging = true;

        // Running logging thread
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMddHHmm");
        String time = dateFormat.format(new Date());
        String txtFileName = time + "_logfile.txt";
        logCpuInfo logCPUThread = new logCpuInfo(getFilesDir().getPath()+"/", txtFileName, this);
        logCPUThread.start();

        logDataButton.setEnabled(false);
        stopLogButton.setEnabled(true);
    }

    public void stopLog(View view){
        isLogging = false;

        logDataButton.setEnabled(true);
        stopLogButton.setEnabled(false);

    }

    class indefiniteThread extends Thread{
//        https://stackoverflow.com/questions/16712404/how-can-i-simulate-different-types-of-load-in-an-android-device
        private final int threadId;

        indefiniteThread(int thread_num) {
            this.threadId = thread_num;
        }

        @Override
        public void run(){
            setResultText( String.valueOf(threadId) + "th child thread is starting");
            Random rd = new Random();
            float a, b;
            int x, y;
            @SuppressWarnings("unused") double r;
            while(isRunning[threadId]) {
                x = rd.nextInt(20) + 20;
                y = rd.nextInt(20) + 20;
                a = rd.nextFloat();
                b = rd.nextFloat();
                //noinspection UnusedAssignment
                r = Math.pow(a + x, b + y) / Math.tan((double) (a + x) / (b + y));

            }
            setResultText( String.valueOf(threadId) + "th child thread is closing");
        }
    }


    public static double getCpuUsageEstimate(){
        long currentSum = 0;
        long maximumSum = 0;
        long minimumSum = 0;

        CPU cpu = new CPU();
        List<Long> currentFreq = cpu.getCurrentFrequencies();
        List<Long> maximumFreq = cpu.getMaximumFrequencies();
        List<Long> minimumFreq = cpu.getMinimumFrequencies();
        int cores = Math.min(Math.min(currentFreq.size(), maximumFreq.size()), minimumFreq.size());
        for(int i=0; i < cores; i++){
            currentSum += currentFreq.get(i);
            maximumSum += maximumFreq.get(i);
            minimumSum += minimumFreq.get(i);
        }
        return maximumSum <= 0 ? 0.0f : (currentSum-minimumSum)/(float)(maximumSum-minimumSum);
    }

    public class logCpuInfo extends Thread{
        private final String dirName;
        private final String txtFileName;
        private final Context mContext;

        logCpuInfo(String dirName, String txtFileName, Context mContext) {
            this.dirName = dirName;
            this.txtFileName = txtFileName;
            this.mContext = mContext;
        }

        @Override
        public void run(){
            setResultText("Running CPU usage logging function");
            while(isLogging){
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM:dd:HH:mm:ss");
                String time = dateFormat.format(new Date());

                String LoggingString = time + " " + String.valueOf(getCpuUsageEstimate()) + "\n";
                setResultText(LoggingString);
                setResultText(Util.listOfBackgroundProcess(mContext));
                try {
                    File file = new File(dirName + txtFileName);
                    FileWriter fr = new FileWriter(file, true);
                    fr.write(LoggingString);
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            setResultText("Killing CPU usage logging function");
        }
    }

    public void runThread(View view){
        setResultText("Running Thread start");
        // Initializing thread boolean array
        isRunning = new boolean[Integer.parseInt(numOfThread.getText().toString())];
        for(int i=0; i<Integer.parseInt(numOfThread.getText().toString()); i++){
            isRunning[i] = false;
        }

        // Running CPU-centric threads
        for(int i=0; i<Integer.parseInt(numOfThread.getText().toString()); i++){
            isRunning[i] = true;
            runningThread = new indefiniteThread(i);
            runningThread.start();
        }


        // Set Button enable accordingly
        runThreadButton.setEnabled(false);
        killThreadButton.setEnabled(true);
    }

    public void killThread(View view){
        setResultText("Killing Thread start");

        // For killing thread
        for(int i=0; i<Integer.parseInt(numOfThread.getText().toString()); i++){
            isRunning[i] = false;
        }

        // Set Button enable accordingly`
        runThreadButton.setEnabled(true);
        killThreadButton.setEnabled(false);
    }

    public void loadData(View view){
        fc = new FlowerClient(this, dataset);
        if (TextUtils.isEmpty(device_id.getText().toString())) {
            Toast.makeText(this, "Please enter a client partition ID between 1 and 21 (inclusive)", Toast.LENGTH_LONG).show();
        }
        else if (Integer.parseInt(device_id.getText().toString()) > 21 ||  Integer.parseInt(device_id.getText().toString()) < 1)
        {
            Toast.makeText(this, "Please enter a client partition ID between 1 and 21 (inclusive)", Toast.LENGTH_LONG).show();
        }
        else{
            hideKeyboard(this);
            setResultText("Loading the local training dataset in memory. It will take several seconds.");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
//                    download_data_from_chris();
                    if ((Integer.parseInt(device_id.getText().toString()) == 13) || (Integer.parseInt(device_id.getText().toString()) == 16) || (Integer.parseInt(device_id.getText().toString()) == 17) || (Integer.parseInt(device_id.getText().toString()) == 21)) {
                        FedBalancerSingleton.getInstance().setIsBigClient(true);
                    }

                    String datapath = getFilesDir().getPath();
                    fc.loadData(Integer.parseInt(device_id.getText().toString()), datapath, dataset);

                    // log results
                    Log.e(TAG, "Currently number of samples loaded is : " + FedBalancerSingleton.getSamplesCount());
                    setResultText("Currently number of samples loaded is : " + FedBalancerSingleton.getSamplesCount());
                    setResultText("Training dataset is loaded in memory.");

                    // set button availability accordingly
                    loadDataButton.setEnabled(false);
                    trainButton.setEnabled(true);
                }
            }, 1000);
        }
    }

    public void initializeConfigs(){
        try {
            String urlString = String.format("http://143.248.36.213:8998/%s/", dataset);
            new DownloadAsyncTask(this).execute(urlString).get();
            new UnzipDataAsyncTask(this).execute(getFilesDir().getPath() + "/","data.zip").get();
            new UnzipDataAsyncTask(this).execute(getFilesDir().getPath() + "/","model.zip").get();

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startTraining(View view){
        new TrainAsyncTask(this).execute();
    }

    public class TrainAsyncTask extends AsyncTask<String, Integer, String> {

        private final MainActivity mContext;

        public TrainAsyncTask(MainActivity context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... Args) {
            int num_of_epoch = 10000;

            if (!TextUtils.isEmpty(numOfEpoch.getText().toString())) {
                num_of_epoch = Integer.parseInt(numOfEpoch.getText().toString());
            }
            List<Integer> res = new ArrayList<Integer>();

            fc.fit(fc.getWeights(), num_of_epoch, res);
            return null;
        }
    }


    private static class InitConfigGrpcTask extends AsyncTask<Void, Void, String> {
        private final GrpcRunnable grpcRunnable;
        private final ManagedChannel channel;
        private final MainActivity activityReference;

        InitConfigGrpcTask(GrpcRunnable grpcRunnable, ManagedChannel channel, MainActivity activity) {
            this.grpcRunnable = grpcRunnable;
            this.channel = channel;
            this.activityReference = activity;
        }

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                grpcRunnable.run(FlowerServiceGrpc.newBlockingStub(channel), FlowerServiceGrpc.newStub(channel), this.activityReference);
                return "Connection to the FL server for initialization successful \n";
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return "Failed to connect to the FL server \n" + sw;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = activityReference;
            if (activity == null) {
                return;
            }
            activity.setResultText(result);
        }
    }


    public class UnzipDataAsyncTask extends AsyncTask<String, Integer, String> {

        private final MainActivity mContext;

        public UnzipDataAsyncTask(MainActivity context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // https://stackoverflow.com/questions/3382996/how-to-unzip-files-programmatically-in-android
        @Override
        protected String doInBackground(String... Args) {
            if (Looper.myLooper()==null)
                Looper.prepare();
            String path = Args[0];
            String zipname;

            // create list
            List<String> strlist = new ArrayList<String>();

            // add 4 different values to list
            strlist.add("data.zip");
            strlist.add("model.zip");

            for (int i = 0; i < strlist.size(); i++) {
//                mContext.setResultText("currently trying to unzip : " + strlist.get(i));
//                try {
//                    mContext.unzip(path + strlist.get(i), path);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                zipname = strlist.get(i);
                mContext.setResultText("UnzipDataAsyncTask Start at path : " + path  + zipname);
                InputStream is;
                ZipInputStream zis;
                try
                {
                    String filename;
                    is = new FileInputStream(path + zipname);
                    zis = new ZipInputStream(new BufferedInputStream(is));
                    ZipEntry ze;
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((ze = zis.getNextEntry()) != null)
                    {
                        filename = ze.getName();
                        if (ze.isDirectory()) {
                            File fmd = new File(path + filename);
                            fmd.mkdirs();
                            continue;
                        }else {
                            mContext.setResultText(path + filename);
                            File model = new File(path + "model");
                            model.mkdirs();
                            File fmd = new File(path + filename);
//                            fmd.mkdirs();
                            fmd.createNewFile();
                        }
                        mContext.setResultText("in progress");
                        mContext.setResultText(path + filename);
                        FileOutputStream fout = new FileOutputStream(path + filename);
                        while ((count = zis.read(buffer)) != -1)
                        {
                            fout.write(buffer, 0, count);
                        }
                        fout.close();
                        zis.closeEntry();
                    }
                    zis.close();
                    mContext.setResultText("done!");
                }
                catch(IOException e)
                {
                    mContext.setResultText("error has occured");
                    System.out.println("=======================================================");
                    e.printStackTrace();
                    System.out.println("=======================================================");

                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
//            mContext.fc = new FlowerClient(mContext);
            mContext.initConfigButton.setEnabled(false);
            mContext.loadDataButton.setEnabled(true);
            mContext.setResultText("UnzipDataAsyncTask Complete!");
            super.onPostExecute(s);

        }
    }

    public class DownloadAsyncTask extends AsyncTask<String, Integer, String> {

        private final MainActivity mContext;

        public DownloadAsyncTask(MainActivity context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(String... Args) {
            String urlString = Args[0];

            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                List<String> strlist = new ArrayList<String>();
                strlist.add("data.zip");
                strlist.add("model.zip");

                for (int i = 0; i < strlist.size(); i++) {
                    mContext.setResultText("currently downloading :" + strlist.get(i));
                    Log.d("ptg","downloading from url : " + urlString + strlist.get(i));
                    URL url = new URL(urlString + strlist.get(i));
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return "Server returned HTTP " + connection.getResponseCode()
                                + " " + connection.getResponseMessage();
                    }

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength();

                    // download the file
                    input = connection.getInputStream();

//            output = new FileOutputStream("/data/data/com.example.vadym.test1/textfile.txt");
                    output = new FileOutputStream(mContext.getFilesDir() + "/" + strlist.get(i));

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // allow canceling with back button
                        if (isCancelled()) {
                            input.close();
                            return null;
                        }
                        total += count;
                        // publishing the progress....
                        if (fileLength > 0) // only if total length is known
                            publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }
                }
            } catch (Exception e) {
                mContext.setResultText("error occured!");
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mContext.setResultText("Current download progress %: " + values[0]);
//            Log.d("ptg", "Current download progress %: " + values[0]);

        }

        @Override
        protected void onPostExecute(String s) {
            mContext.setResultText("Download complete");
            super.onPostExecute(s);

        }
    }

    private interface GrpcRunnable {
        void run(FlowerServiceBlockingStub blockingStub, FlowerServiceStub asyncStub, MainActivity activity) throws Exception;
    }

    private static class FlowerServiceRunnable implements GrpcRunnable {
        private Throwable failed;
        private StreamObserver<ClientMessage> requestObserver;

        @Override
        public void run(FlowerServiceBlockingStub blockingStub, FlowerServiceStub asyncStub, MainActivity activity)
                throws Exception {
             join(asyncStub, activity);
        }

        private void join(FlowerServiceStub asyncStub, MainActivity activity)
                throws InterruptedException, RuntimeException {

            final CountDownLatch finishLatch = new CountDownLatch(1);
            requestObserver = asyncStub.join(
                            new StreamObserver<ServerMessage>() {
                                @Override
                                public void onNext(ServerMessage msg) {
                                    handleMessage(msg, activity);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    failed = t;
                                    finishLatch.countDown();
                                    Log.e(TAG, t.getMessage() + " error ahs occured");
                                }

                                @Override
                                public void onCompleted() {
                                    finishLatch.countDown();
                                    Log.e(TAG, "Done");
                                }
                            });
        }

        private void handleMessage(ServerMessage message, MainActivity activity) {
            Log.e(TAG,"HANDLING MESSAGES");
            try {
                ByteBuffer[] weights;
                ClientMessage c = null;
                int device_id;

                if (message.hasGetParameters()) {
                    Log.e(TAG, "Handling GetParameters");
                    activity.setResultText("Handling GetParameters message from the server.");

                    weights = activity.fc.getWeights();
                    c = weightsAsProto(weights);
                } else if (message.hasDeviceInfo()) {
                    Log.e(TAG, "Handling DeviceInfo");
                    activity.setResultText("Handling DeviceInfo.");

                    device_id = Integer.parseInt(activity.device_id.getText().toString());
                    c = deviceInfoAsProto(device_id);
                } else if (message.hasFitIns()) {
                    // long handleStartTime = System.currentTimeMillis();

                    Log.e(TAG, "Handling FitIns");
                    activity.setResultText("Handling Fit request from the server.");

                    List<ByteString> layers = message.getFitIns().getParameters().getTensorsList();

                    Scalar epoch_config = message.getFitIns().getConfigMap().getOrDefault("local_epochs", Scalar.newBuilder().setSint64(1).build());
                    int local_epochs = (int) epoch_config.getSint64();

                    Scalar deadline_config = message.getFitIns().getConfigMap().getOrDefault("deadline", Scalar.newBuilder().setDouble(1.0).build());
                    double deadline = deadline_config.getDouble();

                    Scalar fedprox_config = message.getFitIns().getConfigMap().getOrDefault("fedprox", Scalar.newBuilder().setBool(false).build());
                    boolean fedprox = fedprox_config.getBool();

                    Scalar fedbalancer_config = message.getFitIns().getConfigMap().getOrDefault("fedbalancer", Scalar.newBuilder().setBool(false).build());
                    boolean fedbalancer = fedbalancer_config.getBool();

                    Scalar ss_baseline_config = message.getFitIns().getConfigMap().getOrDefault("ss_baseline", Scalar.newBuilder().setBool(false).build());
                    boolean ss_baseline = ss_baseline_config.getBool();

                    Scalar train_time_per_epoch_config = message.getFitIns().getConfigMap().getOrDefault("train_time_per_epoch", Scalar.newBuilder().setDouble(1.0).build());
                    double train_time_per_epoch = train_time_per_epoch_config.getDouble();

                    Scalar train_time_per_batch_config = message.getFitIns().getConfigMap().getOrDefault("train_time_per_batch", Scalar.newBuilder().setDouble(1.0).build());
                    double train_time_per_batch = train_time_per_batch_config.getDouble();

                    Scalar inference_time_config = message.getFitIns().getConfigMap().getOrDefault("inference_time", Scalar.newBuilder().setDouble(1.0).build());
                    double inference_time = inference_time_config.getDouble();

                    Scalar networking_time_config = message.getFitIns().getConfigMap().getOrDefault("networking_time", Scalar.newBuilder().setDouble(1.0).build());
                    double networking_time = networking_time_config.getDouble();

                    Scalar loss_threshold_config = message.getFitIns().getConfigMap().getOrDefault("loss_threshold", Scalar.newBuilder().setDouble(0.0).build());
                    double loss_threshold = loss_threshold_config.getDouble();

                    Scalar fb_p_config = message.getFitIns().getConfigMap().getOrDefault("fb_p", Scalar.newBuilder().setDouble(0.0).build());
                    double fb_p = fb_p_config.getDouble();

                    // Scalar round_idx_config = message.getFitIns().getConfigMap().getOrDefault("round_idx", Scalar.newBuilder().setSint64(1).build());
                    // int round_idx = (int) round_idx_config.getSint64();

                    Log.e(TAG, local_epochs + " " + deadline + " " + fedprox + " " + fedbalancer + " " + train_time_per_epoch + " " + networking_time);

                    List<Float> sampleloss = message.getFitIns().getSamplelossList();

                    Log.e(TAG, sampleloss.toString());
                    Log.e(TAG, sampleloss.size()+"");

                    // decoding process differs via datasets -> different datasets have different models
                    Log.e(TAG, activity.dataset);
                    ByteBuffer[] newWeights = new ByteBuffer[0];
                    if(activity.dataset.contains("har")){
                        // Our new new model has 6 layers -> har(UCIHAR_CNN)
                        Log.e(TAG, "setting byte buffer of dataset har");
                        newWeights = new ByteBuffer[6] ;
                        for (int i = 0; i < 6; i++) {
                            newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                        }
                    }else if(activity.dataset.contains("femnist")){
                        Log.e(TAG, "setting byte buffer of dataset femnist");
                        // Our new new model has 8 layers -> femnist
                        newWeights = new ByteBuffer[8] ;
                        for (int i = 0; i < 8; i++) {
                            newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                        }
                    }

                    Log.e(TAG, "setting byte buffer done");
                    Pair<ByteBuffer[], Integer> outputs = null;

                    List<Integer> sampleIndexToTrain = new ArrayList<Integer>();
                    List<Float> sortedLoss = new ArrayList<Float>();

                    boolean isThisFirstRound = false;

                    if (fedbalancer || (FedBalancerSingleton.getInstance().getIsBigClient() && ss_baseline)) {
                        if (FedBalancerSingleton.getInstance().getIsFirstRound()) {
                            FedBalancerSingleton.getInstance().setWholeDataLossList(sampleloss);
                            FedBalancerSingleton.getInstance().setIsFirstRound(false);
                            activity.fc.sampleInferenceLatency(newWeights);
                            isThisFirstRound = true;
                        }

                        if (fedbalancer) {
                            Pair<List<Integer>, List<Float>> ssresult = activity.fc.fbSampleSelection(loss_threshold, deadline, local_epochs, train_time_per_batch, fb_p);
                            sampleIndexToTrain = ssresult.first;
                            sortedLoss = ssresult.second;
                        } else if (FedBalancerSingleton.getInstance().getIsBigClient() && ss_baseline) {
                            sampleIndexToTrain = activity.fc.baselineSampleSelection();

                        }
                    }

                    SampleLatencyReturnValues slrv;

                    if (fedprox || fedbalancer) {
                        int ne = -1;
                        Log.e(TAG, "local_epochs, train_time_per_epoch, networking_time: "+local_epochs+" "+train_time_per_epoch+" "+networking_time);
                        if (isThisFirstRound){
                            for (int e_idx = 1; e_idx < local_epochs + 1; e_idx++) {
                                double e_time = ((sampleIndexToTrain.size()-1) / 10 + 1) * train_time_per_batch * e_idx + networking_time + inference_time;
                                if (e_time < deadline) {
                                    ne = e_idx;
                                }
                            }
                        } else {
                            for (int e_idx = 1; e_idx < local_epochs + 1; e_idx++) {
                                double e_time = ((sampleIndexToTrain.size()-1) / 10 + 1) * train_time_per_batch * e_idx + networking_time;
                                if (e_time < deadline) {
                                    ne = e_idx;
                                }
                            }
                        }
                        Log.e(TAG, "ne: "+ ne);

                        if (ne == -1) {
                            ne = 1;
                        }

                        slrv = activity.fc.fit(newWeights, ne, sampleIndexToTrain);
                    }
                    else {
                        slrv = activity.fc.fit(newWeights, local_epochs, sampleIndexToTrain);
                    }

                    FedBalancerSingleton.getInstance().addTrainEpochAndBatchLatencyHistory(Pair.create(slrv.getTrain_time_per_epoch(), slrv.getTrain_time_per_batch()));

                    float current_round_loss_min = (float) 0.0;
                    float current_round_loss_max = (float) 0.0;

                    float loss_square_summ = (float) 0.0;
                    int overthreshold_loss_count = 0;
                    float loss_summ = (float) 0.0;
                    int loss_count = 0;

                    if (fedbalancer || (FedBalancerSingleton.getInstance().getIsBigClient() && ss_baseline)) {
                        for (int idx = 0; idx < sampleIndexToTrain.size(); idx++) {
                            FedBalancerSingleton.getInstance().setIndexOfWholeDataLossList(sampleloss.get(sampleIndexToTrain.get(idx)), sampleIndexToTrain.get(idx));
                        }
                    }

                    if (fedbalancer) {
                        current_round_loss_min = (float) sortedLoss.get(0);
                        current_round_loss_max = (float) percentile(sortedLoss, 80);

                        for(int loss_idx = 0; loss_idx < sortedLoss.size(); loss_idx++) {
                            if (sortedLoss.get(loss_idx) > loss_threshold) {
                                loss_square_summ += (float) (sortedLoss.get(loss_idx) * sortedLoss.get(loss_idx));
                                overthreshold_loss_count ++;
                            }
                            loss_summ += (float) sortedLoss.get(loss_idx);
                            loss_count ++;
                        }
                    }

                    weights = activity.fc.getWeights();

                    c = fitResAsProto(weights, slrv.getSize_training(), current_round_loss_min, current_round_loss_max, loss_square_summ, overthreshold_loss_count, loss_summ, loss_count, slrv.getTrain_time_per_epoch(), slrv.getTrain_time_per_batch(), slrv.getTrain_time_per_epoch_list(), slrv.getTrain_time_per_batch_list());
                }  else if (message.hasInitializeConfigIns()) { // TODO: initialize config implementation
                    Log.e(TAG, "Handling InitializeConfig");

                    activity.setResultText("Handling InitializeConfig request from the server.");
                    String datasetName = message.getInitializeConfigIns().getDatasetName();

                    activity.setResultText("Got datasetName from server as : " + datasetName);

                    // set dataset string to the given datasetName & run initialzation function
                    activity.dataset = datasetName;
                    activity.initializeConfigs();

                    // send response to server
                    c = initializeConfigResAsProto(datasetName);
                    requestObserver.onNext(c);

                    // finish & close the grpc connection
                    requestObserver.onCompleted();
                }

                requestObserver.onNext(c);
                activity.setResultText("Response sent to the server");
                c = null;
            }
            catch (Exception e){
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static Float percentile(List<Float> inputList, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * inputList.size());
        return inputList.get(index-1);
    }

    private static ClientMessage weightsAsProto(ByteBuffer[] weights){
        List<ByteString> layers = new ArrayList<ByteString>();
        System.out.println(weights.length);
        for (int i=0; i < weights.length; i++) {
            layers.add(ByteString.copyFrom(weights[i]));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.ParametersRes res = ClientMessage.ParametersRes.newBuilder().setParameters(p).build();
        return ClientMessage.newBuilder().setParametersRes(res).build();
    }

    private static ClientMessage fitResAsProto(ByteBuffer[] weights, int training_size, float loss_min, float loss_max, float loss_square_summ, int overthreshold_loss_count, float loss_summ, int loss_count, float train_time_per_epoch, float train_time_per_batch, float[] train_time_per_epoch_list, float[] train_time_per_batch_list){
        List<ByteString> layers = new ArrayList<ByteString>();
        for (int i=0; i < weights.length; i++) {
            Log.e(TAG, weights[i]+"");
            layers.add(ByteString.copyFrom(weights[i]));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.FitRes.Builder res_builder = ClientMessage.FitRes.newBuilder().setParameters(p).setNumExamples(training_size).setLossMin(loss_min).setLossMax(loss_max).setLossSquareSum(loss_square_summ).setOverthresholdLossCount(overthreshold_loss_count).setLossSum(loss_summ).setLossCount(loss_count).setTrainTimePerEpoch(train_time_per_epoch).setTrainTimePerBatch(train_time_per_batch);

        for (int i=0; i < train_time_per_epoch_list.length; i++){
            res_builder = res_builder.addTrainTimePerEpochList(train_time_per_epoch_list[i]);
        }

        for (int i=0; i < train_time_per_batch_list.length; i++){
            res_builder = res_builder.addTrainTimePerBatchList(train_time_per_batch_list[i]);
        }

        ClientMessage.FitRes res = res_builder.build();
        return ClientMessage.newBuilder().setFitRes(res).build();
    }

    private static ClientMessage sampleLatencyResAsProto(String msg_receive_time, String msg_sent_time, float train_time_per_epoch, float train_time_per_batch, float inference_time, ByteBuffer[] weights, int training_size, float[] train_time_per_epoch_list, float[] train_time_per_batch_list){
        List<ByteString> layers = new ArrayList<ByteString>();
        for (int i=0; i < weights.length; i++) {
            layers.add(ByteString.copyFrom(weights[i]));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.SampleLatencyRes.Builder res_builder = ClientMessage.SampleLatencyRes.newBuilder().setMsgReceiveTime(msg_receive_time).setMsgSentTime(msg_sent_time).setTrainTimePerEpoch(train_time_per_epoch).setTrainTimePerBatch(train_time_per_batch).setInferenceTime(inference_time).setParameters(p).setNumExamples(training_size);

        for (int i=0; i < train_time_per_epoch_list.length; i++){
            res_builder = res_builder.addTrainTimePerEpochList(train_time_per_epoch_list[i]);
        }

        for (int i=0; i < train_time_per_batch_list.length; i++){
            res_builder = res_builder.addTrainTimePerBatchList(train_time_per_batch_list[i]);
        }

        ClientMessage.SampleLatencyRes res = res_builder.build();

        return ClientMessage.newBuilder().setSampleLatencyRes(res).build();
    }

    // TODO: initialize config response proto
    private static ClientMessage initializeConfigResAsProto(String datasetName){
        ClientMessage.InitializeConfigRes.Builder res_builder;
        if(datasetName == ""){
            res_builder = ClientMessage.InitializeConfigRes.newBuilder().setSuccess(1);
        }else{
            res_builder = ClientMessage.InitializeConfigRes.newBuilder().setSuccess(0);
        }
        ClientMessage.InitializeConfigRes res = res_builder.build();
//        return ClientMessage.newBuilder().setSampleLatencyRes(res).build();
        return ClientMessage.newBuilder().setInitializeConfigRes(res).build();
    }

    private static ClientMessage deviceInfoAsProto(int device_id){
        ClientMessage.DeviceInfoRes res = ClientMessage.DeviceInfoRes.newBuilder().setDeviceId(device_id).build();
        return ClientMessage.newBuilder().setDeviceInfoRes(res).build();
    }
}
