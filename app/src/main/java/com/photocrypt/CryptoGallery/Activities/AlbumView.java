package com.photocrypt.CryptoGallery.Activities;

/*
 *PhotoCrypt - an encrypted gallery for Android
 *Copyright (C) 2018 Thunder Gabriel <tgabriel@protonmail.com>
 *
 *This program is free software; you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation; either version 3 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, see http://www.gnu.org/licenses/.
 */

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.photocrypt.CryptoGallery.Photo;
import com.photocrypt.CryptoGallery.WhenLongClicked;
import com.photocrypt.CryptoGallery.Threads.ImportPhotos;
import com.photocrypt.CryptoGallery.Threads.ViewThread;
import com.photocrypt.CryptoGallery.ImportParams;
import com.photocrypt.CryptoGallery.helpers.PhotoCrypt;
import com.example.xavi.photocrypt.R;
import com.photocrypt.CryptoGallery.WhenClicked;

import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.Queue;


public class AlbumView extends AppCompatActivity {

    private String title;
    private static final int READ_REQUEST_CODE = 42;
    private static Queue<Photo> photoQueue = new LinkedList<>(); //maybe not the greatest solution to have this static
    private Bundle b;
    GridLayout grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_view);
        Bundle extra_title = getIntent().getExtras();
        this.title = extra_title.getString("TITLE");

        try {
            populateView();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        photoQueue = new LinkedList<>();
    }

    private void reload()
    {
        photoQueue = new LinkedList<>();
        finish();
        startActivity(getIntent());
    }

    private void populateView() throws GeneralSecurityException
    {
        PhotoCrypt photocrypt = new PhotoCrypt(getApplicationContext());

        grid = findViewById(R.id.grid2);


        Handler h1 = new Handler(Looper.getMainLooper()) {

            String title, location;
            byte[] thumbnail;
            Photo photo;

            @Override
            public void handleMessage(Message msg)
            {
                ImageButton btn = new ImageButton(getApplicationContext());
                b = msg.getData();
                title = b.getString("TITLE");
                location = b.getString("LOCATION");
                thumbnail = b.getByteArray("THUMBNAIL");

                photo = new Photo(title, location, thumbnail);

                btn.setOnClickListener(new WhenClicked(null, photo.getLocation(), getApplicationContext()));
                btn.setOnLongClickListener(new WhenLongClicked(photo));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(320,448);
                params.leftMargin = 35;
                params.bottomMargin = 25;
                btn.setLayoutParams(params);
                Bitmap bmp = PhotoCrypt.byteArrayToBitmap(photo.getThumbnail());
                btn.setImageBitmap(bmp);
                grid.addView(btn);
            }
        };

        Thread thread = new Thread(new ViewThread(photocrypt, this.title, h1));
        thread.start();

    }

    public static void addToQueue(Photo photo)
    {
        photoQueue.add(photo);
    }

    public void deleteAll(View v)
    {
        PhotoCrypt photocrypt = new PhotoCrypt(getApplicationContext());

        photocrypt.deletePhotos(photoQueue);

        reload();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData)
    {
        int n;
        Uri uris[];

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK){

            if (resultData.getClipData() != null){
                n = resultData.getClipData().getItemCount();
                uris = new Uri[n];

                for (int i = 0; i < n; i++){
                    uris[i] = resultData.getClipData().getItemAt(i).getUri();
                }
            } else {
                uris = new Uri[] {resultData.getData()};

            } try {
                ImportParams importParams = new ImportParams(uris, getApplicationContext(), this.title);
                ImportPhotos importThread = new ImportPhotos();
                importThread.execute(importParams);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void export(View v) throws Exception
    {
        if (!photoQueue.isEmpty()) {
            PhotoCrypt photocrypt = new PhotoCrypt(getApplicationContext());
            photocrypt.exportPhotos(photoQueue);
            reload();
        }

    }

    public void getPhotoFromSystem(View v)
    {
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }
}
