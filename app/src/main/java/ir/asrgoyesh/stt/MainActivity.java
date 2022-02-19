package ir.asrgoyesh.stt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.JsonReader;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.io.StringReader;

public class MainActivity extends AppCompatActivity implements
        RecognitionListener {

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_MIC = 3;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Model model;
    private SpeechService speechService;
    private TextView resultView;
    private Button mic;
    private ToggleButton pause;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);


        // Setup layout
        resultView = findViewById(R.id.result_text);
        mic=findViewById(R.id.recognize_mic);
        pause=(ToggleButton) findViewById(R.id.pause);
        setUiState(STATE_START);
        mic.setOnClickListener(view -> recognizeMicrophone());
        pause.setOnCheckedChangeListener((view, isChecked) -> pause(isChecked));

        LibVosk.setLogLevel(LogLevel.INFO);

        // Check if user has given permission to record audio, init the model after permission is granted
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initModel();
        }
    }

    private void initModel() {
        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    setUiState(STATE_READY);
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                initModel();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
    }


    @Override
    public void onResult(String hypothesis) {
        resultView.append(hypothesis + "\n");
        /*try {
            JSONObject songs = new JSONObject(hypothesis);
            resultView.append(songs + "\n");
        }catch (JSONException e) {
            System.out.println(e.getMessage());
        }*/
    }

    @Override
    public void onFinalResult(String hypothesis) {
        //resultView.append(hypothesis + "\n");
        setUiState(STATE_DONE);
    }

    @Override
    public void onPartialResult(String hypothesis) {
        //resultView.append(hypothesis + "\n");
        /*try {
            JSONObject songs= new JSONObject(hypothesis);

            resultView.append(songs + "\n");

        }catch (JSONException e) {
            System.out.println(e.getMessage());
        }*/
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        setUiState(STATE_DONE);
    }

    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                resultView.setText(R.string.preparing);
                resultView.setMovementMethod(new ScrollingMovementMethod());
                mic.setEnabled(false);
                pause.setEnabled((false));
                break;
            case STATE_READY:
                resultView.setText(R.string.ready);
                mic.setText(R.string.recognize_microphone);
                mic.setEnabled(true);
                pause.setEnabled((false));
                break;
            case STATE_DONE:
                mic.setText(R.string.recognize_microphone);
                mic.setEnabled(true);
                pause.setEnabled((false));
                break;
            case STATE_MIC:
                mic.setText(R.string.stop_microphone);
                resultView.setText(getString(R.string.say_something));
                mic.setEnabled(true);
                pause.setEnabled((true));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void setErrorState(String message) {
        resultView.setText(message);
        mic.setText(R.string.recognize_microphone);
        mic.setEnabled(false);
    }


    private void recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE);
            speechService.stop();
            speechService = null;
        } else {
            setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }


    private void pause(boolean checked) {
        if (speechService != null) {
            speechService.setPause(checked);
        }
    }

}