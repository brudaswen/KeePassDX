/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.fileselect;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kunzisoft.keepass.R;

import java.util.List;

public class FileSelectAdapter extends RecyclerView.Adapter<FileSelectViewHolder> {

    private LayoutInflater inflater;
    private List<String> listFiles;
    private View.OnClickListener mOnClickListener;
    private FileSelectViewHolder.FileSelectClearListener fileSelectClearListener;
    private FileInformationShowListener fileInformationShowListener;

    FileSelectAdapter(Context context, List<String> listFiles) {
        inflater = LayoutInflater.from(context);
        this.listFiles=listFiles;
    }

    @Override
    public FileSelectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.file_row, parent, false);
        view.setOnClickListener(mOnClickListener);
        return new FileSelectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FileSelectViewHolder holder, int position) {
        FileSelectBeen fileSelectBeen = new FileSelectBeen(listFiles.get(position));
        holder.fileName.setText(fileSelectBeen.getFileName());
        if(fileSelectClearListener != null)
            holder.setFileSelectClearListener(fileSelectClearListener);
        if(fileInformationShowListener != null)
            holder.fileInformation.setOnClickListener(new FileInformationClickListener(fileSelectBeen));
    }

    @Override
    public int getItemCount() {
        return listFiles.size();
    }

    void setOnItemClickListener(View.OnClickListener onItemClickListener) {
        this.mOnClickListener = onItemClickListener;
    }

    void setFileSelectClearListener(FileSelectViewHolder.FileSelectClearListener fileSelectClearListener) {
        this.fileSelectClearListener = fileSelectClearListener;
    }

    void setFileInformationShowListener(FileInformationShowListener fileInformationShowListener) {
        this.fileInformationShowListener = fileInformationShowListener;
    }

    private class FileInformationClickListener implements View.OnClickListener {

        private FileSelectBeen fileSelectBeen;

        FileInformationClickListener(FileSelectBeen fileSelectBeen) {
            this.fileSelectBeen = fileSelectBeen;
        }

        @Override
        public void onClick(View view) {
            fileInformationShowListener.onClickFileInformation(fileSelectBeen);
        }
    }

    public interface FileInformationShowListener {
        void onClickFileInformation(FileSelectBeen fileSelectBeen);
    }

}
