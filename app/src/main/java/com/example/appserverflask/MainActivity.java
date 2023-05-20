package com.example.appserverflask;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificar si los permisos ya están concedidos
        if (hasStoragePermission()) {
            // Los permisos ya están concedidos, puedes acceder al archivo aquí
        } else {
            // Los permisos no están concedidos, solicitarlos
            requestStoragePermission();
        }
    }

    private boolean hasStoragePermission() {
        // Verificar si el permiso de almacenamiento ya está concedido
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true; // No se requiere verificación en versiones anteriores a Android 6.0
        }
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        // Solicitar el permiso de almacenamiento en tiempo de ejecución
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Verificar si el permiso fue concedido
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes acceder al archivo aquí
            } else {
                // Permiso denegado, manejar la situación en consecuencia
            }
        }
    }
}
