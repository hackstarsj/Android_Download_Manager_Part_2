package com.furthergrow.android_download_manager;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

public class DownloadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Context context;
    List<DownloadModel> downloadModels=new ArrayList<>();
    ItemClickListener clickListener;

    public DownloadAdapter(Context context,List<DownloadModel> downloadModels,ItemClickListener itemClickListener){
        this.context=context;
        this.clickListener=itemClickListener;
        this.downloadModels=downloadModels;
    }

    public class DownloadViewHolder extends RecyclerView.ViewHolder{

        TextView file_title;
        TextView file_size;
        ProgressBar file_progress;
        Button pause_resume,sharefile;
        TextView file_status;
        RelativeLayout main_rel;

        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            file_title=itemView.findViewById(R.id.file_title);
            file_size=itemView.findViewById(R.id.file_size);
            file_status=itemView.findViewById(R.id.file_status);
            file_progress=itemView.findViewById(R.id.file_progress);
            pause_resume=itemView.findViewById(R.id.pause_resume);
            main_rel=itemView.findViewById(R.id.main_rel);
            sharefile=itemView.findViewById(R.id.sharefile);

        }
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.download_row,parent,false);
        vh=new DownloadViewHolder(view);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        final DownloadModel downloadModel=downloadModels.get(position);
        final DownloadViewHolder downloadViewHolder= (DownloadViewHolder) holder;

        downloadViewHolder.file_title.setText(downloadModel.getTitle());
        downloadViewHolder.file_status.setText(downloadModel.getStatus());
        downloadViewHolder.file_progress.setProgress(Integer.parseInt(downloadModel.getProgress()));
        downloadViewHolder.file_size.setText("Downloaded : "+downloadModel.getFile_size());

        if(downloadModel.isIs_paused()){
            downloadViewHolder.pause_resume.setText("RESUME");
        }
        else{
            downloadViewHolder.pause_resume.setText("PAUSE");
        }

        if(downloadModel.getStatus().equalsIgnoreCase("RESUME")){
            downloadViewHolder.file_status.setText("Running");
        }


        downloadViewHolder.pause_resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(downloadModel.isIs_paused()){
                    downloadModel.setIs_paused(false);
                    downloadViewHolder.pause_resume.setText("PAUSE");
                    downloadModel.setStatus("RESUME");
                    downloadViewHolder.file_status.setText("Running");
                    if(!resumeDownload(downloadModel)){
                        Toast.makeText(context, "Failed to Resume", Toast.LENGTH_SHORT).show();
                    }
                    notifyItemChanged(position);
                }
                else {
                    downloadModel.setIs_paused(true);
                    downloadViewHolder.pause_resume.setText("RESUME");
                    downloadModel.setStatus("PAUSE");
                    downloadViewHolder.file_status.setText("PAUSE");
                    if(!pauseDownload(downloadModel)){
                        Toast.makeText(context, "Failed to Pause", Toast.LENGTH_SHORT).show();
                    }
                    notifyItemChanged(position);
                }
            }
        });

        downloadViewHolder.main_rel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onCLickItem(downloadModel.getFile_path());
            }
        });

        downloadViewHolder.sharefile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickListener.onShareClick(downloadModel);
            }
        });

    }

    private boolean pauseDownload(DownloadModel downloadModel) {
       int updatedRow=0;
        ContentValues contentValues=new ContentValues();
        contentValues.put("control",1);

        try{
            updatedRow=context.getContentResolver().update(Uri.parse("content://downloads/my_downloads"),contentValues,"title=?",new String[]{downloadModel.getTitle()});
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return 0<updatedRow;
    }

    private boolean resumeDownload(DownloadModel downloadModel) {
        int updatedRow=0;
        ContentValues contentValues=new ContentValues();
        contentValues.put("control",0);

        try{
            updatedRow=context.getContentResolver().update(Uri.parse("content://downloads/my_downloads"),contentValues,"title=?",new String[]{downloadModel.getTitle()});
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return 0<updatedRow;
    }

    @Override
    public int getItemCount() {
        return downloadModels.size();
    }

    public void changeItem(long downloadid){
        int i=0;
        for(DownloadModel downloadModel:downloadModels){
            if(downloadid==downloadModel.getDownloadId()){
                notifyItemChanged(i);
            }
            i++;
        }
    }

    public boolean ChangeItemWithStatus(final String message, long downloadid){
        boolean comp=false;
        int i=0;
        for(final DownloadModel downloadModel:downloadModels){
            if(downloadid==downloadModel.getDownloadId()){
                Realm realm=Realm.getDefaultInstance();
                final int finalI = i;
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        downloadModels.get(finalI).setStatus(message);
                        notifyItemChanged(finalI);
                        realm.copyToRealmOrUpdate(downloadModels.get(finalI));
                    }
                });
                comp=true;
            }
            i++;
        }
        return comp;
    }

    public void setChangeItemFilePath(final String path, long id){

        Realm  realm=Realm.getDefaultInstance();
        int i=0;
        for(DownloadModel downloadModel:downloadModels){
            if(id==downloadModel.getDownloadId()){

                final int finalI = i;
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        downloadModels.get(finalI).setFile_path(path);
                        notifyItemChanged(finalI);
                        realm.copyToRealmOrUpdate(downloadModels.get(finalI));

                    }
                });
            }
            i++;
        }
    }


}
