package com.project.toko.DishesActivities;

import static com.project.toko.mylibrary.SharedClass.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.toko.R;
import com.project.toko.mylibrary.DishItem;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class EditOffer extends AppCompatActivity {
    private int frequency = 0;
    private String name, desc, price, currentPhotoPath = null, error_msg = "", keyChild;
    private int quantValue = -1;
    private ImageView imageview;

    private boolean editing = false, photoChanged = false;

    private Button quantButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_offer);


        quantButton = findViewById(R.id.quantity);
        quantButton.setOnClickListener(f->setQuantity());

        findViewById(R.id.plus).setOnClickListener(p -> editPhoto());
        findViewById(R.id.img_profile).setOnClickListener(e -> editPhoto());

        imageview = findViewById(R.id.img_profile);

        String dishName = getIntent().getStringExtra(EDIT_EXISTING_DISH);
        if(dishName != null)
            getData(dishName);
        else
            imageview.setImageResource(R.drawable.hamburger);

        findViewById(R.id.button).setOnClickListener(e -> {
            if(checkFields()){
                storeDatabase();

                finish();
            }
            else{
                Toast.makeText(getApplicationContext(), error_msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getData(String dishName){
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
        Query query = myRef.child(RESTAURATEUR_INFO + "/" + ROOT_UID + "/" + DISHES_PATH)
                .orderByChild("name").equalTo(dishName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    DishItem dish = new DishItem();

                    for(DataSnapshot d : dataSnapshot.getChildren()) {
                        dish = d.getValue(DishItem.class);
                        keyChild = d.getKey();
                        break;
                    }

                    editing = true;

                    assert dish != null;
                    name = dish.getName();
                    desc = dish.getDesc();
                    price = String.valueOf(dish.getPrice());
                    quantValue = dish.getQuantity();
                    currentPhotoPath = dish.getPhoto();
                    frequency = dish.getFrequency();

                    if(currentPhotoPath != null)
                        Glide.with(getApplicationContext()).load(currentPhotoPath).diskCacheStrategy(DiskCacheStrategy.RESOURCE).into((ImageView)findViewById(R.id.img_profile));
                    else
                        Glide.with(getApplicationContext()).load(R.drawable.hamburger).into(imageview);

                    ((EditText)findViewById(R.id.name)).setText(name);
                    ((EditText)findViewById(R.id.description)).setText(desc);
                    ((EditText)findViewById(R.id.price)).setText(price);

                    quantButton.setText(Integer.toString(quantValue));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("EDIT OFFER", "Failed to read value.", error.toException());
            }
        });
    }

    private boolean checkFields(){
        name = ((EditText)findViewById(R.id.name)).getText().toString();
        desc = ((EditText)findViewById(R.id.description)).getText().toString();
        price = ((EditText)findViewById(R.id.price)).getText().toString();

        if(name.trim().length() == 0){
            error_msg = "Fill name";
            return false;
        }

        if(desc.trim().length() == 0){
            error_msg = "Fill description";
            return false;
        }

        if(price.trim().length() == 0){
            error_msg = "Insert price";
            return false;
        }

        if(quantValue == -1){
            error_msg = "Insert quantity";
            return false;
        }

        return true;
    }

    @SuppressLint("SetTextI18n")
    private void setQuantity(){
        AlertDialog quantDialog = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = LayoutInflater.from(EditOffer.this);
        final View view = inflater.inflate(R.layout.quantity_dialog, null);

        NumberPicker quantity = view.findViewById(R.id.quant_picker);

        quantDialog.setView(view);

        quantDialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK", (dialog, which) -> {
            quantValue = quantity.getValue();
            quantButton.setText(Integer.toString(quantValue));
        });
        quantDialog.setButton(DialogInterface.BUTTON_NEGATIVE,"CANCEL", (dialog, which) -> {
            dialog.dismiss();
        });

        quantity.setMinValue(1);
        quantity.setMaxValue(999);
        if(quantValue != -1)
            quantity.setValue(quantValue);
        else
            quantity.setValue(1);

        quantDialog.show();
    }

    private void editPhoto(){
        AlertDialog alertDialog = new AlertDialog.Builder(EditOffer.this, R.style.AlertDialogStyle).create();
        LayoutInflater factory = LayoutInflater.from(EditOffer.this);
        final View view = factory.inflate(R.layout.custom_dialog, null);

        alertDialog.setOnCancelListener(dialog -> {
            alertDialog.dismiss();
        });

        view.findViewById(R.id.camera).setOnClickListener( c -> {
            cameraIntent();
            alertDialog.dismiss();
        });
        view.findViewById(R.id.gallery).setOnClickListener( g -> {
            galleryIntent();
            alertDialog.dismiss();
        });

        alertDialog.setView(view);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Camera", (dialog, which) -> {
            cameraIntent();
            dialog.dismiss();
        });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Gallery", (dialog, which) -> {
            galleryIntent();
            dialog.dismiss();
        });
        alertDialog.show();
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void cameraIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();

            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.project.toko",
                    photoFile);

            photoChanged = true;

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, 2);
        }
    }

    private void galleryIntent(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    PERMISSION_GALLERY_REQUEST);
        }
        else{
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, 1);
        }
    }

    @SuppressLint("SimpleDateFormat")
    private File createImageFile() {
        // Create an image file name
        String timeStamp = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        }
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File( storageDir + File.separator +
                imageFileName + /* prefix */
                ".jpg"
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_GALLERY_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission Run Time: ", "Obtained");

                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, 1);
                } else {
                    Log.d("Permission Run Time: ", "Denied");

                    Toast.makeText(getApplicationContext(), "Access to media files denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if((requestCode == 1) && resultCode == RESULT_OK && null != data){
            Uri selectedImage = data.getData();
            photoChanged = true;

            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            currentPhotoPath = picturePath;
        }

        if((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK){
            Glide.with(getApplicationContext()).load(currentPhotoPath).into((ImageView)findViewById(R.id.img_profile));
        }
    }

    private void storeDatabase(){
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference(
                RESTAURATEUR_INFO + "/" + ROOT_UID + "/" + DISHES_PATH);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Map<String, Object> dishMap = new HashMap<>();

        if(!editing)
            keyChild = myRef.push().getKey();

        if(photoChanged) {
            Uri photoUri = Uri.fromFile(new File(currentPhotoPath));
            StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());

            ref.putFile(photoUri).continueWithTask(task -> {
                if (!task.isSuccessful()){
                    throw Objects.requireNonNull(task.getException());
                }
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    Uri downUri = task.getResult();

                    dishMap.put(keyChild, new DishItem(name, desc, price, quantValue, downUri.toString(), frequency));
                    myRef.updateChildren(dishMap);
                }
            });
        }
        else{
            if(currentPhotoPath != null)
                dishMap.put(keyChild, new DishItem(name, desc, price, quantValue, currentPhotoPath, frequency));
            else
                dishMap.put(keyChild, new DishItem(name, desc, price, quantValue, null, frequency));

            myRef.updateChildren(dishMap);
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog dialog = new AlertDialog.Builder(EditOffer.this).create();
        LayoutInflater inflater = LayoutInflater.from(EditOffer.this);
        final View view = inflater.inflate(R.layout.reservation_dialog, null);

        view.findViewById(R.id.button_confirm).setOnClickListener(e -> super.onBackPressed());
        view.findViewById(R.id.button_cancel).setOnClickListener(e -> dialog.dismiss());

        dialog.setView(view);
        dialog.setTitle("Are you sure to cancel?");

        dialog.show();
    }
}
