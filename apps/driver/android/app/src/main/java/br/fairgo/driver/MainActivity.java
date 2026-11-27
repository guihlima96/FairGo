package br.fairgo.driver;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String ALIAS = "fairgo.driver.session.v1";
    private static final String STORE = "AndroidKeyStore";

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance(STORE);
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, STORE);
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }

    @Override public void configureFlutterEngine(FlutterEngine engine) {
        super.configureFlutterEngine(engine);
        AtomicFile file = new AtomicFile(new File(getNoBackupFilesDir(), "driver-session"));
        new MethodChannel(engine.getDartExecutor().getBinaryMessenger(), "br.fairgo.driver/session")
            .setMethodCallHandler((call, result) -> {
                try {
                    switch (call.method) {
                        case "save": {
                            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                            cipher.init(Cipher.ENCRYPT_MODE, key());
                            byte[] encrypted = cipher.doFinal(((String) call.arguments).getBytes(StandardCharsets.UTF_8));
                            FileOutputStream out = file.startWrite();
                            try {
                                out.write(cipher.getIV());
                                out.write(encrypted);
                                file.finishWrite(out);
                            } catch (Exception ex) {
                                file.failWrite(out);
                                throw ex;
                            }
                            break;
                        }
                        case "read": {
                            if (file.getBaseFile().exists()) {
                                byte[] bytes = file.readFully();
                                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                                cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, bytes, 0, 12));
                                result.success(new String(cipher.doFinal(Arrays.copyOfRange(bytes, 12, bytes.length)), StandardCharsets.UTF_8));
                                return;
                            }
                            break;
                        }
                        case "clear":
                            file.delete();
                            if (file.getBaseFile().exists()) throw new java.io.IOException();
                            break;
                        default: result.notImplemented(); return;
                    }
                    result.success(null);
                } catch (Exception ex) {
                    result.error("session_storage", "Armazenamento indispon�vel.", null);
                }
            });
    }
}
