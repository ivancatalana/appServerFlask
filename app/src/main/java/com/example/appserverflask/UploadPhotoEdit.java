package com.example.appserverflask;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
//----------------------------------------FRAGMENT DE PRUEBA--------------------------------------------------------
//----------------------------------------FRAGMENT DE PRUEBA--------------------------------------------------------
//----------------------------------------FRAGMENT DE PRUEBA--------------------------------------------------------
//----------------------------------------FRAGMENT DE PRUEBA--------------------------------------------------------
public class UploadPhotoEdit extends Fragment {

    private static final int PICK_FILE_REQUEST_CODE = 1;
    private ImageView serverImageView;
    private Button uploadButton;

    public UploadPhotoEdit() {
        // Constructor vacío requerido para fragmentos
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_upload, container, false);

        uploadButton = rootView.findViewById(R.id.uploadButton);
        serverImageView = rootView.findViewById(R.id.serverImageView);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        return rootView;
    }

    private void openFileChooser() {
        // Crear un intent para abrir el FileChooser
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");  // Seleccionar cualquier tipo de archivo

        // Verificar si hay una actividad que pueda manejar el intent
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Obtener la URI del archivo seleccionado
            Uri uri = data.getData();

            // Obtener la ruta del archivo a partir de la URI
            String filePath = getFilePathFromUri(uri);

            // Verificar si se pudo obtener la ruta del archivo
            if (filePath != null) {
                // Continuar con la lógica de subida de archivo
                // ...

                // Crear instancia de OkHttpClient
                OkHttpClient client = new OkHttpClient();

                // Crear instancia de MultipartBody.Builder
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "IMG_20230516_221347_345.jpg",
                                RequestBody.create(MediaType.parse("image/jpeg"), new File(filePath)))
                        .build();

                // Crear solicitud POST
                Request request = new Request.Builder()
                        .url("http://ivancatalana.duckdns.org:8000/upload")
                        .post(requestBody)
                        .build();

                // Ejecutar la solicitud de forma asíncrona
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }
                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String responseData = response.body().string();

                        if (response.isSuccessful()) {
                            // Procesar la respuesta del servidor

                            int startIndex = responseData.indexOf("\"") + 1;
                            int endIndex = responseData.indexOf("\"", startIndex);
                            String imageUrl = responseData.substring(startIndex, endIndex);
                            System.out.println("URL de la imagen: " + imageUrl);


                            System.out.println("Respuesta del servidor: " + responseData);
                            //  JSONObject jsonObject = new JSONObject(responseData);
                            //System.out.println(jsonObject.getJSONObject("link")+"----------------------------------------------------");
                            //String imageUrl = jsonObject.getString("link");
                            System.out.println(imageUrl+"-------------------------------------------------------------------------");

                            // Cargar la imagen desde la URL utilizando Glide y mostrarla en el ImageView
                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .into(serverImageView);
                        }

                        // Mostrar la respuesta del servidor por consola
                        System.out.println("Respuesta del servidor: " + responseData);
                    }

                });
            }
        }
    }

    @Nullable
    private String getFilePathFromUri(Uri uri) {
        String filePath = null;

        if (uri != null) {
            // Si la URI tiene el esquema "file", obtener la ruta directamente
            if (uri.getScheme().equals("file")) {
                filePath = uri.getPath();
            } else {
                // Si la URI tiene otro esquema (como "content"), intenta obtener la ruta utilizando MediaStore
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    filePath = cursor.getString(columnIndex);
                    cursor.close();
                }
            }
        }

        return filePath;
    }
}
           
