package ir.asrgoyesh.stt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        RecognitionListener {

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_MIC = 3;
    static private final int STATE_CORRECT= 4;
    static private final int STATE_INCORRECT = 5;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    String[] s={"What's your name?","My name is Frank.","Nice to meet you Frank.","Nice to meet you too."};
    int sno=0;

    private Model model;
    private SpeechService speechService;
    private TextView resultView,stateView,scoreView,ScoresView;
    private RatingBar ratingBar;
    private ImageButton mic,next;
    private LinearLayout ScoreBar;
    String[] sent;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);


        // Setup layout
        resultView = findViewById(R.id.result_text);
        resultView.setText(s[sno]);
        stateView = findViewById(R.id.state_text);
        ScoresView = findViewById(R.id.scores_text);
        ScoreBar = findViewById(R.id.score_bar);
        scoreView = findViewById(R.id.score_text);
        ratingBar = findViewById(R.id.ratingBar);
        mic=findViewById(R.id.recognize_mic);
        next=findViewById(R.id.next);
        setUiState(STATE_START);
        mic.setOnClickListener(view -> recognizeMicrophone());
        next.setOnClickListener(view -> {
            if (sno<s.length) {
                sno++;
                sent = s[sno].substring(0, s[sno].length() - 1).toLowerCase().split(" ");
                setUiState(STATE_READY);
            }
            else
                Toast.makeText(this, "پایان", Toast.LENGTH_LONG).show();
        });

        sent=s[sno].substring(0,s[sno].length()-1).toLowerCase().split(" ");
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


    @SuppressLint("SetTextI18n")
    @Override
    public void onResult(String hypothesis) {
        LinkedList<Word> sentence = new LinkedList<>();
        try {

            JSONObject obj = new JSONObject(hypothesis);
            JSONArray jsonArray = obj.getJSONArray("result");
            for (int i=0;i<jsonArray.length();i++) {
                sentence.add(new Word(jsonArray.getJSONObject(i).getDouble("conf"),jsonArray.getJSONObject(i).getString("word")));
            }


        } catch (Throwable t) {
            Log.e("My App", "Could not parse malformed JSON: \"" + hypothesis + "\"");
        }
        boolean isMatch=true;
        for (int i=0;i<sentence.size();i++) {
            if (!(sent.length==sentence.size())) {
                isMatch = false;
                break;
            }else if (!sent[i].equals(sentence.get(i).getContent())){
                isMatch = false;
                break;
            }
        }
        int score = 0;
        ScoresView.setText("\n");
        if (isMatch) {
            resultView.setText("");
            for (int i = 0; i < sentence.size(); i++) {
                resultView.append(Html.fromHtml(sentence.get(i).toString()));
                ScoresView.append(sentence.get(i).getContent()+": "+(int)sentence.get(i).getScore()+"\n");
                score += (int) sentence.get(i).getScore();
            }
            resultView.append(Html.fromHtml("<font color='"+sentence.getLast().getColor()+"'>"+s[sno].charAt(s[sno].length()-1)+" </font>"));

            int sc=0;
            try {
                sc= score /sentence.size();
            }catch (ArithmeticException ignored){

            }
            scoreView.setText("امتیاز:"+sc);
            ratingBar.setRating((int)(sc/20));
            setUiState(STATE_CORRECT);
            recognizeMicrophone();
        }else{
            resultView.setTextColor(Color.parseColor("#E10600"));
            for (int i = 0; i < sentence.size(); i++) {
                ScoresView.append(sentence.get(i).getContent()+":"+(int)sentence.get(i).getScore()+"\n");
            }
            setUiState(STATE_INCORRECT);
            recognizeMicrophone();
        }


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
                stateView.setText(R.string.preparing);
                mic.setBackgroundResource(R.drawable.mic_back2);
                mic.setEnabled(false);
                next.setVisibility(View.GONE);
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.GONE);
                break;
            case STATE_READY:
                stateView.setText(R.string.ready);
                mic.setImageResource(R.drawable.mic_icon);
                mic.setBackgroundResource(R.drawable.mic_back);
                resultView.setTextColor(Color.BLACK);
                resultView.setText(s[sno]);
                next.setVisibility(View.GONE);
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.GONE);
                mic.setEnabled(true);
                break;
            case STATE_DONE:
                mic.setImageResource(R.drawable.mic_icon);
                mic.setBackgroundResource(R.drawable.mic_back);
                next.setVisibility(View.GONE);
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.VISIBLE);
                mic.setEnabled(true);
                break;
            case STATE_INCORRECT:
                mic.setImageResource(R.drawable.ic_baseline_refresh);
                mic.setBackgroundResource(R.drawable.mic_back);
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.VISIBLE);
                stateView.setText(getString(R.string.incorrect));
                next.setVisibility(View.GONE);
                mic.setEnabled(true);
                break;
            case STATE_CORRECT:
                mic.setImageResource(R.drawable.ic_baseline_refresh);
                mic.setBackgroundResource(R.drawable.mic_back);
                ScoreBar.setVisibility(View.VISIBLE);
                ScoresView.setVisibility(View.VISIBLE);
                stateView.setText(getString(R.string.correct));
                next.setVisibility(View.VISIBLE);
                mic.setEnabled(true);
                break;
            case STATE_MIC:
                mic.setImageResource(R.drawable.ic_baseline_stop_24);
                resultView.setTextColor(Color.BLACK);
                mic.setBackgroundResource(R.drawable.mic_back);
                stateView.setText(getString(R.string.say));
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.GONE);
                next.setVisibility(View.GONE);
                mic.setEnabled(true);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void setErrorState(String message) {
        stateView.setText(message);
        mic.setImageResource(R.drawable.mic_icon);
        mic.setEnabled(false);
    }

    private void recognizeMicrophone() {

        if (speechService != null) {
            speechService.stop();
            speechService = null;
        } else {
            new CountDownTimer(3000,1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    resultView.setText(""+(int)((millisUntilFinished/1000)+1));
                }

                @Override
                public void onFinish() {
                    resultView.setText(s[sno]);

                }
            }.start();
            setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 44000.0f);
                speechService = new SpeechService(rec, 44000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

}