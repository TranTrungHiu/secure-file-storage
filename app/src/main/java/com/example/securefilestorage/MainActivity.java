    package com.example.securefilestorage;

    import android.Manifest;
    import android.app.Activity;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.net.Uri;
    import android.os.Bundle;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;

    import java.io.BufferedReader;
    import java.io.File;
    import java.io.FileInputStream;
    import java.io.FileOutputStream;
    import java.io.InputStream;
    import java.io.InputStreamReader;
    import java.io.OutputStream;

    // Thêm thư viện Jetpack Security
    import androidx.security.crypto.EncryptedFile;
    import androidx.security.crypto.MasterKeys;

    public class MainActivity extends AppCompatActivity {
        private static final int REQUEST_PERMISSION = 1;
        private EditText editText;
        private TextView textView;
        private Button btnSave, btnRead, btnOpenFolder;
        private File lastSavedFile;
        private static final int REQUEST_CODE_OPEN_FILE = 100;
        private Button btnChooseFile;
        private ActivityResultLauncher<Intent> saveFileLauncher;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            btnChooseFile = findViewById(R.id.btnChooseFile);
            btnChooseFile.setOnClickListener(v -> chooseFile());

            editText = findViewById(R.id.editText);
            textView = findViewById(R.id.textView);
            btnSave = findViewById(R.id.btnSave);
    //        btnOpenFolder = findViewById(R.id.btnOpenFolder);
            saveFileLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                saveToCustomLocation(uri, editText.getText().toString());
                            }
                        }
                    }
            );
            // Thêm sự kiện nhấn nút "Lưu" trong onCreate()
            btnSave.setOnClickListener(v -> chooseSaveLocation());
            btnRead = findViewById(R.id.btnRead);
            // Thêm sự kiện nhấn nút "Đọc" trong onCreate()
            btnRead.setOnClickListener(v -> readFromFile("secure_text_encrypted.txt"));
            requestStoragePermission();
        }

        private void chooseFile() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain"); // Chỉ chọn file văn bản

            startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
        }
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    readFileFromUri(uri);
                }
            }
        }

        private void saveToCustomLocation(Uri uri, String content) {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                outputStream.write(content.getBytes());
                outputStream.close();

                Toast.makeText(this, "Lưu file thành công!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lưu file!", Toast.LENGTH_SHORT).show();
            }
        }

        private void chooseSaveLocation() {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "my_secure_file.txt");

            saveFileLauncher.launch(intent);
        }

        private void readFileFromUri(Uri uri) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                inputStream.close();

                // Hiển thị nội dung lên textView
                runOnUiThread(() -> textView.setText(sb.toString()));

                Toast.makeText(this, "Đọc file thành công!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi đọc file", Toast.LENGTH_SHORT).show();
            }
        }



        //    Kiểm tra và yêu cầu quyền truy cập thẻ nhớ
        private void requestStoragePermission() {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == REQUEST_PERMISSION) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Đã cấp quyền lưu trữ", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Từ chối quyền lưu trữ", Toast.LENGTH_SHORT).show();
                }
            }
        }


        // Lưu file vào thẻ nhớ
        private void saveToFile(String filename, String content) {
            try {
                File path = new File(getExternalFilesDir(null), "SecureFiles");
                if (!path.exists()) {
                    path.mkdirs(); // Tạo thư mục nếu chưa có
                }

                File file = new File(path, filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(content.getBytes());
                fos.close();

                // Thông báo đường dẫn chính xác của file
                String message = "Lưu thành công tại: " + file.getAbsolutePath();
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lưu file", Toast.LENGTH_SHORT).show();
            }
        }
        //    Đọc file từ thẻ nhớ
        private void readFromFile(String filename) {
            try {
                File file = new File(getFilesDir(), filename);

                if (!file.exists()) {
                    Toast.makeText(this, "File không tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }

                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                fis.close();

                textView.setText(sb.toString());
                Toast.makeText(this, "Đọc file thành công từ: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi đọc file", Toast.LENGTH_SHORT).show();
            }
        }

        //    Thêm phương thức mã hóa
        private void encryptFile(File file, String content) {
            try {
                if (file.exists()) {
                    file.delete();
                }
                EncryptedFile encryptedFile = new EncryptedFile.Builder(
                        file,
                        this,
                        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build();

                FileOutputStream fos = encryptedFile.openFileOutput();
                fos.write(content.getBytes());
                fos.close();

                // Thông báo đường dẫn chính xác của file đã mã hóa
                String message = "Lưu mã hóa thành công tại: " + file.getAbsolutePath();
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi mã hóa file", Toast.LENGTH_SHORT).show();
            }
        }
    }