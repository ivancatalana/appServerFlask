package com.example.appserverflask;

import static com.example.appserverflask.MainActivity.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class UploadFragment extends Fragment {

    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    private ImageView imageView;
    private ImageView serverImageView;
    private Button uploadButton;
    private Button buttonRotate;
    private Bitmap bitmap;
    private String serverURL = "http://ivancatalana.duckdns.org:8000/";  // Reemplaza con la URL de tu servidor Flask
    private String additionalText;
    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);

        buttonRotate = view.findViewById(R.id.btnRotate);
        buttonRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateImage90Degrees();
                // Girar la imagen 90 grados hacia la derecha
               // Bitmap rotatedBitmap = rotateImage(bitmap, 90f);
                //bitmap=rotatedBitmap;
            }
        });

        imageView = view.findViewById(R.id.imageView);
        serverImageView = view.findViewById(R.id.serverImageView);
        uploadButton = view.findViewById(R.id.uploadButton);
        uploadButton.setEnabled(false);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                } else {
                    pickImageFromGallery();
                }
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageView.getTag() != null) {
                    Uri imageUri = Uri.parse(imageView.getTag().toString());
// Verificar si se tienen los permisos necesarios
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // Si los permisos no se han concedido, solicitarlos al usuario
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    } else {
// Verificar si se tiene el permiso de lectura del almacenamiento externo
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        // Solicitar el permiso de lectura del almacenamiento externo
                        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
                    } else {
                        // El permiso ya está concedido, llamar al método uploadFile
                        try {
                            long imageSize = getFileSizeFromUri(imageUri);
                            long maxSize = 50000 * 1024; // Tamaño máximo en bytes

                            if (imageSize > maxSize) {
                                uploadFileComprimido(imageUri);
                            } else {
                                uploadFile(imageUri);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }}
                }
            }
        });


        return view;
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == getActivity().RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            // Obtener la ruta del archivo de la URI
            String filePath = getImagePathFromUri(imageUri);

            // Cargar la imagen como miniatura respetando su aspecto original
            int targetWidth = imageView.getWidth();
            int targetHeight = imageView.getHeight();
            Bitmap bitmap = decodeSampledBitmapFromFile(filePath, targetWidth, targetHeight);

            // Mostrar la imagen corregida en el ImageView
            imageView.setImageBitmap(bitmap);
            imageView.setTag(imageUri.toString());
            uploadButton.setEnabled(true);
        }
    }

    private Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {
        // Primero, se obtienen las dimensiones del archivo de imagen sin decodificar
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calcular el factor de escala para ajustar las dimensiones de la imagen a las dimensiones de la miniatura deseada
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decodificar el archivo de imagen con el factor de escala calculado
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Dimensiones originales de la imagen
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        // Si las dimensiones originales de la imagen son mayores que las dimensiones de la miniatura deseada,
        // se calcula el factor de escala apropiado
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Se calcula el valor más grande inSampleSize que es una potencia de 2 y mantiene ambas dimensiones
            // de la imagen mayores que las dimensiones de la miniatura deseada
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImageFromGallery();
        }
    }
    private String getImagePathFromUri(Uri uri) {
        String imagePath = null;
        if (getContext() != null) {
            Cursor cursor = null;
            try {
                String[] projection = {MediaStore.Images.Media.DATA};
                cursor = getContext().getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    imagePath = cursor.getString(columnIndex);
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return imagePath;
    }
    private void uploadFile(Uri uri) {
        // Ruta del archivo en el dispositivo Android
        String filePath = getImagePathFromUri(uri);

        // Rotar la imagen según los metadatos de rotación
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        bitmap = rotateImageBasedOnExif(bitmap, filePath);

        // Crea un cliente OkHttp con un tiempo de espera extendido
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(200, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS) // Establece el tiempo de espera para leer la respuesta del servidor
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();


        String randomFileName = UUID.randomUUID().toString() + ".jpg";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bitmapData = baos.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", randomFileName,
                        RequestBody.create(MediaType.parse("image/jpeg"), bitmapData))
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
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    // Procesar la respuesta del servidor
                    System.out.println("Respuesta del servidor: " + responseData);
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        String imageUrl = jsonObject.getString("link");
                        // Cargar la imagen desde la URL utilizando Glide y mostrarla en el ImageView
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .into(serverImageView);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Manejar la respuesta de error del servidor
                    System.out.println("Error en la respuesta del servidor: " + response.code());
                }
            }
        });
    }

    private void uploadFileComprimido(Uri uri) throws IOException {
        // Ruta del archivo en el dispositivo Android
        String filePath = getImagePathFromUri(uri);

        // Rotar la imagen según los metadatos de rotación
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        bitmap = rotateImageBasedOnExif(bitmap, filePath);


        // Comprimir la imagen a un tamaño máximo de 50 KB
        int maxSize = 500 * 1024; // Tamaño máximo en bytes

        float scale = (float) Math.sqrt((double) maxSize / (double) bitmap.getByteCount());
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap compressedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Guardar el archivo comprimido en el almacenamiento interno
        File compressedFile = saveBitmapToInternalStorage(compressedBitmap);


        // Crea un cliente OkHttp con un tiempo de espera extendido
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(200, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS) // Establece el tiempo de espera para leer la respuesta del servidor
                .writeTimeout(200, TimeUnit.SECONDS)
                .build();

        String randomFileName = UUID.randomUUID().toString() + ".jpg";

        // Crear instancia de MultipartBody.Builder
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", randomFileName, RequestBody.create(MediaType.parse("image/jpeg"), compressedFile))
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
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    // Procesar la respuesta del servidor
                    System.out.println("Respuesta del servidor: " + responseData);
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        String imageUrl = jsonObject.getString("link");
                        // Cargar la imagen desde la URL utilizando Glide y mostrarla en el ImageView
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .into(serverImageView);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Manejar la respuesta de error del servidor
                    System.out.println("Error en la respuesta del servidor: " + response.code());
                }
            }
        });
    }

    private File saveBitmapToInternalStorage(Bitmap bitmap) {
        File file = new File(requireContext().getCacheDir(), "compressed_image.jpg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos); // Comprimir la imagen con calidad del 75%
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private void rotateImage90Degrees() {
        // Obtén la imagen actual (puedes ajustar esto según tu implementación)
        ImageView imageView = requireView().findViewById(R.id.imageView);
        Drawable drawable = imageView.getDrawable();
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

        // Crea una matriz de rotación de 90 grados
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        // Aplica la matriz de rotación a la imagen
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Establece la imagen rotada en el ImageView
        imageView.setImageBitmap(rotatedBitmap);
    }
    private Bitmap rotateImage(Bitmap bitmap2, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap2, 0, 0, bitmap2.getWidth(), bitmap2.getHeight(), matrix, true);
    }
    private Bitmap rotateImageBasedOnExif(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int rotationAngle = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
                    break;
            }

            if (rotationAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }
    public void setAdditionalText(String text) {
        this.additionalText = text;
    }
    private long getFileSizeFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                long size = inputStream.available();
                inputStream.close();
                return size;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }


}