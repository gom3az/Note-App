package com.example.mg.todo.ui.NoteFragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.bumptech.glide.RequestManager;
import com.example.mg.todo.R;
import com.example.mg.todo.data.model.NoteModel;
import com.example.mg.todo.ui.NoteFragment.DI.IFragmentScope;
import com.example.mg.todo.utils.BitmapUtil;
import com.example.mg.todo.utils.DataFragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;


@SuppressLint("ClickableViewAccessibility")
@IFragmentScope
public class NoteFragmentPresenter implements INoteFragContract.IPresenter {
    private static final int REQUEST_CODE = 19;
    private final NoteFragment mView;
    private final RequestManager glide;
    private NoteModel mNote;
    private String mFileLocation;
    private Bitmap mBitmap;
    private DataFragment dataFragment;

    @Inject
    NoteFragmentPresenter(NoteFragment mView, RequestManager glide) {
        this.mView = mView;
        this.glide = glide;

        FragmentManager fm = mView.getFragmentManager();
        dataFragment = (DataFragment) Objects.requireNonNull(fm).findFragmentByTag("data");
        // create the fragment and data the first time
        if (dataFragment == null) {
            dataFragment = new DataFragment();
            fm.beginTransaction().add(dataFragment, "data").commit();
        }
    }

    @Override
    public void initFragmentData(NoteModel mNote) {
        this.mNote = mNote;
        // if user clicked on a note to update it it calls this function to update the ui of the fragment
        // else init a new note object
        if (mNote != null) {
            mView.editTextTitle.setText(mNote.getText());
            mView.editTextDescription.setText(mNote.getDescription());
            if (mNote.getImage() != null) {
                byte[] bytes = BitmapUtil.decodeImage(mNote.getImage());
                mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mView.onImageAdded(mBitmap, 2f);
            }
        } else this.mNote = new NoteModel();
    }

    @Override
    public void onSaveInstanceState() {
        dataFragment.setmUpdated(NoteFragment.mUpdated);
        dataFragment.setData(mNote);
        if (mView.imageNote.getDrawable() != null) {
            String s = BitmapUtil.encodeDrawable(mView.imageNote.getDrawable());
            dataFragment.setTempImage(s);
        }

    }

    @Override
    public void onRestoreState() {
        NoteFragment.mUpdated = dataFragment.getmUpdated();
        mNote = dataFragment.getData();
        String currentImage = dataFragment.getTempImage();
        if (currentImage != null) {
            glide.asBitmap()
                    .load(BitmapUtil.decodeImage(currentImage))
                    .into(mView.imageNote);
        }
    }

    @Override
    public void onDoneClick() {
        String title = mView.editTextTitle.getText().toString();
        String description = mView.editTextDescription.getText().toString();
        if (title.equals("") || description.equals("")) mView.onFilledDataError();
        else {
            mNote.setText(title);
            mNote.setDescription(description);
            if (NoteFragment.mUpdated != -1) // check if the note is newly added or an edited one
                mNote.setmDate(String.format("Edited on: %s",
                        new SimpleDateFormat("EEE, MMM d, ''yy hh:mm aaa",
                                Locale.getDefault()).format(new Date())));
            else
                mNote.setmDate(new SimpleDateFormat("EEE, MMM d, ''yy hh:mm aaa",
                        Locale.getDefault()).format(new Date()));

            // check if there is an image on edit text to add it into database
            // else if user took an image and then delete it so we delete it
            if (mView.imageNote.getDrawable() != null)
                mNote.setImage(BitmapUtil.encodeDrawable(mView.imageNote.getDrawable()));
            else mNote.setImage(null);
            mView.mSendNote.sendNoteObject(mNote, NoteFragment.mUpdated);
            mView.getDialog().dismiss();
        }
    }

    @Override
    public void onTakeImageClick() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(Objects.requireNonNull(mView.getActivity()).getPackageManager()) != null) {
            File file = null;
            try {
                file = BitmapUtil.createTempImageFile(Objects.requireNonNull(mView.getActivity()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (file != null) {

                mFileLocation = BitmapUtil.mCurrentPhotoPath;
                Uri uri = FileProvider.getUriForFile(mView.getActivity(), "com.example.mg.todo", file);
                i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                mView.startActivityForResult(i, REQUEST_CODE);
            }
        }
    }

    // get user taken image and puts it into description edit text
    // then add to note object
    // compress  mBitmap to byte array using mBitmap CompressFormat
    // then encode it to base 64 to store it as a string into the database
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            mBitmap = BitmapUtil.resamplePic(mFileLocation);
            mView.onImageAdded(mBitmap, 1f);
        } else {
            //mNote.setImage(BitmapUtil.encodeDrawable(mView.imageNote.getDrawable()));
            mView.onCancelImageCapture();
        }
    }

    @Override
    public void onImageClick(View v) {
        if (mNote.getImage() != null) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            //Inflating the Popup using xml file
            popup.getMenuInflater()
                    .inflate(R.menu.popup_menu, popup.getMenu());

            //deletes image if user clicked delete button from menu
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.delete) {
                        onDeleteImageClicked();
                    } else if (item.getItemId() == R.id.view) {
                        onViewImageClicked();
                    }
                    return true;
                }
            });
            popup.show();
        }
    }

    @Override
    public void onDeleteImageClicked() {
        mView.imageNote.setOnClickListener(null);
        mView.imageNote.setImageResource(0);
    }


    @Override
    public void onViewImageClicked() {
        final Dialog builder = new Dialog(Objects.requireNonNull(mView.getContext()));
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(builder.getWindow()).setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setContentView(R.layout.image_viewer);
        ImageView imageView = builder.findViewById(R.id.image_preview);
        Bitmap bitmap = ((BitmapDrawable) mView.imageNote.getDrawable()).getBitmap();
        imageView.setImageBitmap(BitmapUtil.resize(bitmap, 2f));
        builder.show();
    }

}

