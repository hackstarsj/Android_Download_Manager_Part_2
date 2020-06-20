package com.furthergrow.android_download_manager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class MainActivity extends AppCompatActivity implements ItemClickListener {

    private static final int PERMISSION_REQUEST_CODE = 101;
    DownloadAdapter downloadAdapter;
    List<DownloadModel> downloadModels=new ArrayList<>();
    Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        realm=Realm.getDefaultInstance();
        registerReceiver(onComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Button add_download_list=findViewById(R.id.add_download_list);
        RecyclerView data_list=findViewById(R.id.data_list);

        add_download_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });

        List<DownloadModel> downloadModelsLocal=getAllDownloads();
        if(downloadModelsLocal!=null){
            if(downloadModelsLocal.size()>0){
                downloadModels.addAll(downloadModelsLocal);
                for(int i=0;i<downloadModels.size();i++){
                    if(downloadModels.get(i).getStatus().equalsIgnoreCase("Pending") || downloadModels.get(i).getStatus().equalsIgnoreCase("Running") || downloadModels.get(i).getStatus().equalsIgnoreCase("Downloading")){
                        DownloadStatusTask downloadStatusTask=new DownloadStatusTask(downloadModels.get(i));
                        runTask(downloadStatusTask,""+downloadModels.get(i).getDownloadId());
                    }
                }
            }
        }
        downloadAdapter=new DownloadAdapter(MainActivity.this,downloadModels,MainActivity.this);
        data_list.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        data_list.setAdapter(downloadAdapter);

        Intent intent=getIntent();
        if(intent!=null){
            String action=intent.getAction();
            String type=intent.getType();
            if(Intent.ACTION_SEND.equals(action) && type!=null){
                if(type.equalsIgnoreCase("text/plain")){
                    handleTextData(intent);
                }
                else if(type.startsWith("image/")){
                    handleImage(intent);
                }
                else if(type.equalsIgnoreCase("application/pdf")){
                    handlePdfFile(intent);
                }
            }
            else if(Intent.ACTION_SEND_MULTIPLE.equals(action) && type!=null){
                if(type.startsWith("image/")){
                    handleMultipleImage(intent);
                }
            }
        }

    }

    private void handlePdfFile(Intent intent) {
        Uri pdffile=intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(pdffile!=null) {
            Log.d("Pdf File Path : ", "" + pdffile.getPath());
        }
    }

    private void handleImage(Intent intent) {
        Uri image=intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(image!=null) {
            Log.d("Image File Path : ", "" + image.getPath());
        }
    }

    private void handleTextData(Intent intent) {
        String  textdata=intent.getStringExtra(Intent.EXTRA_TEXT);
        if(textdata!=null) {
            Log.d("Text Data : ", "" + textdata);
            downloadFile(textdata);
        }
    }

    private void handleMultipleImage(Intent intent) {
        ArrayList<Uri> imageList=intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if(imageList!=null) {
            for (Uri uri : imageList) {
                Log.d("Path ",""+uri.getPath());
            }
        }
    }


    private void showInputDialog(){
        AlertDialog.Builder al=new AlertDialog.Builder(MainActivity.this);
        View view=getLayoutInflater().inflate(R.layout.input_dilaog,null);
        al.setView(view);


        final EditText editText=view.findViewById(R.id.input);
        Button paste=view.findViewById(R.id.paste);

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager= (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                try{
                    CharSequence charSequence=clipboardManager.getPrimaryClip().getItemAt(0).getText();
                    editText.setText(charSequence);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        al.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadFile(editText.getText().toString());
            }
        });

        al.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        al.show();

    }

    private void downloadFile(String url) {
        String filename= URLUtil.guessFileName(url,null,null);
        String downloadPath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        File file=new File(downloadPath,filename);

        DownloadManager.Request request=null;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            request=new DownloadManager.Request(Uri.parse(url))
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }
        else{
            request=new DownloadManager.Request(Uri.parse(url))
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }

        DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long downloadId=downloadManager.enqueue(request);

        Number currentnum=realm.where(DownloadModel.class).max("id");
        int nextId;

        if(currentnum==null){
            nextId=1;
        }
        else{
            nextId=currentnum.intValue()+1;
        }
        final DownloadModel downloadModel=new DownloadModel();
        downloadModel.setId(nextId);
        downloadModel.setStatus("Downloading");
        downloadModel.setTitle(filename);
        downloadModel.setFile_size("0");
        downloadModel.setProgress("0");
        downloadModel.setIs_paused(false);
        downloadModel.setDownloadId(downloadId);
        downloadModel.setFile_path("");

        downloadModels.add(downloadModel);
        downloadAdapter.notifyItemInserted(downloadModels.size()-1);

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealm(downloadModel);
            }
        });


        DownloadStatusTask downloadStatusTask=new DownloadStatusTask(downloadModel);
        runTask(downloadStatusTask,""+downloadId);


    }

    @Override
    public void onCLickItem(String file_path) {
        Log.d("File Path : ",""+file_path);
        openFile(file_path);
    }

    @Override
    public void onShareClick(DownloadModel downloadModel) {
        File file=new File(downloadModel.getFile_path().replaceAll("file:///",""));
        Log.d("File Path",""+file.getAbsolutePath());
        MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
        String ext=MimeTypeMap.getFileExtensionFromUrl(file.getName());
        String type=mimeTypeMap.getExtensionFromMimeType(ext);

        if(type==null){
            type="*/*";
        }

        try{
            Intent intent=new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT,"Sharing File from File Downloader");

            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri path=FileProvider.getUriForFile(MainActivity.this,"com.furthergrow.android_download_manager",file);
                intent.putExtra(Intent.EXTRA_STREAM,path);
            }
            else{
                intent.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(file));
            }
            intent.setType("*/*");
            startActivity(intent);
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, "No Activity Availabe to Handle File", Toast.LENGTH_SHORT).show();
        }

    }

    public class DownloadStatusTask extends AsyncTask<String,String,String>{

        DownloadModel downloadModel;
        public DownloadStatusTask(DownloadModel downloadModel){
            this.downloadModel=downloadModel;
        }

        @Override
        protected String doInBackground(String... strings) {
            downloadFileProcess(strings[0]);
            return null;
        }

        private void downloadFileProcess(String downloadId) {
            DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            boolean downloading=true;
            while (downloading){
                DownloadManager.Query query=new DownloadManager.Query();
                query.setFilterById(Long.parseLong(downloadId));
                Cursor cursor=downloadManager.query(query);
                cursor.moveToFirst();

                int bytes_downloaded=cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int total_size=cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                if(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))==DownloadManager.STATUS_SUCCESSFUL){
                    downloading=false;
                }


                int progress= (int) ((bytes_downloaded*100L)/total_size);
                String status=getStatusMessage(cursor);
                publishProgress(new String[]{String.valueOf(progress), String.valueOf(bytes_downloaded),status});
                cursor.close();
            }

        }

        @Override
        protected void onProgressUpdate(final String... values) {
            super.onProgressUpdate(values);
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                 public void execute(Realm realm) {

                     downloadModel.setFile_size(bytesIntoHumanReadable(Long.parseLong(values[1])));
                     downloadModel.setProgress(values[0]);
                     if (!downloadModel.getStatus().equalsIgnoreCase("PAUSE") && !downloadModel.getStatus().equalsIgnoreCase("RESUME")) {
                         downloadModel.setStatus(values[2]);
                     }
                    downloadAdapter.changeItem(downloadModel.getDownloadId());

                }
           });

        }
    }

    private String getStatusMessage(Cursor cursor) {
        String msg="-";
        switch (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))){
            case DownloadManager.STATUS_FAILED:
                msg="Failed";
                break;
            case DownloadManager.STATUS_PAUSED:
                msg= "Paused";
                 break;
            case DownloadManager.STATUS_RUNNING:
                msg= "Running";
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                msg= "Completed";
                break;
            case DownloadManager.STATUS_PENDING:
                msg= "Pending";
                break;
            default:
                msg="Unknown";
                break;
        }
        return msg;
    }

    BroadcastReceiver onComplete=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
            boolean comp=downloadAdapter.ChangeItemWithStatus("Completed",id);

            if(comp){
                DownloadManager.Query query=new DownloadManager.Query();
                query.setFilterById(id);
                DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Cursor cursor=downloadManager.query(new DownloadManager.Query().setFilterById(id));
                cursor.moveToFirst();

                String downloaded_path=cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                downloadAdapter.setChangeItemFilePath(downloaded_path,id);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
    }

    public void runTask(DownloadStatusTask downloadStatusTask,String id){
        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
                downloadStatusTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,new String[]{id});
            }
            else{
                downloadStatusTask.execute(new String[]{id});
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private String bytesIntoHumanReadable(long bytes) {
        long kilobyte = 1024;
        long megabyte = kilobyte * 1024;
        long gigabyte = megabyte * 1024;
        long terabyte = gigabyte * 1024;

        if ((bytes >= 0) && (bytes < kilobyte)) {
            return bytes + " B";

        } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
            return (bytes / kilobyte) + " KB";

        } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
            return (bytes / megabyte) + " MB";

        } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
            return (bytes / gigabyte) + " GB";

        } else if (bytes >= terabyte) {
            return (bytes / terabyte) + " TB";

        } else {
            return bytes + " Bytes";
        }
    }

    private RealmResults<DownloadModel> getAllDownloads(){
        Realm realm=Realm.getDefaultInstance();
        return realm.where(DownloadModel.class).findAll();
    }

    private void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(MainActivity.this, "Please Give Permission to Upload File", Toast.LENGTH_SHORT).show();
        }
        else{
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkPermission(){
        int result= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(result== PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this, "Permission Successfull", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "Permission Failed", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void openFile(String fileurl){
        if(Build.VERSION.SDK_INT>=23){
            if(!checkPermission()){
                requestPermission();
                Toast.makeText(this, "Please Allow Permission to Open File", Toast.LENGTH_SHORT).show();
                return;
            }

        }

        try{
            fileurl=PathUtil.getPath(MainActivity.this,Uri.parse(fileurl));

            File file=new File(fileurl);
            MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
            String ext=MimeTypeMap.getFileExtensionFromUrl(file.getName());
            String type=mimeTypeMap.getMimeTypeFromExtension(ext);

            if(type==null){
                type="*/*";
            }

            Intent intent=new Intent(Intent.ACTION_VIEW);
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contne= FileProvider.getUriForFile(MainActivity.this,"com.furthergrow.android_download_manager",file);
                intent.setDataAndType(contne,type);
            }
            else{
                intent.setDataAndType(Uri.fromFile(file),type);
            }
            startActivity(intent);
        }
        catch (Exception e){
            Toast.makeText(this, "Unable to Open File", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

}
