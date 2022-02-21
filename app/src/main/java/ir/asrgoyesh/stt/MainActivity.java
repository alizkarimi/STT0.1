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
import android.os.Handler;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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


    Quiz[] Q={new Quiz(1,"سلام کنجی!","Hello Kenji!"),
            new Quiz(2,"سلام جان.","Hi John."),
            new Quiz(3,"سلام به همگی.","Hello everybody."),
            new Quiz(4,"سلام بچه ها."	,"Hi guys."),
            new Quiz(5,"سلام آقا.","Hello sir."),
            new Quiz(6,"سلام خانم.","Hello madam."),
            new Quiz(7,"سلام آقای براون.","Hello Mr. Brown."),
            new Quiz(8,"صبح بخیر خانم اسمیت.","Good morning Mrs. Smith."),
            new Quiz(9,"بعدازظهربخیر دوشیزه جونز.","Good afternoon Miss Jones."),
            new Quiz(10,"عصربخیر.","Good evening."),
            new Quiz(11,"حالت چطوره؟","How are you?"),
            new Quiz(12,"مادرت چطوره؟","How is your mother?"),
            new Quiz(13,"پدرت چطوره؟","How is your father?"),
            new Quiz(14,"خواهرت چطوره؟","How is your sister?"),
            new Quiz(15,"برادرت چطوره؟","How is your brother?"),
            new Quiz(16,"والدینت چطورن؟","How are your parents?"),
            new Quiz(17,"زنت چطوره؟","How is your wife?"),
            new Quiz(18,"شوهرت چطوره؟","How is your husband?"),
            new Quiz(19,"همسرت چطوره؟","How is your spouse?"),
            new Quiz(20,"نامزدت چطوره؟","How is your fiance?"),
            new Quiz(21,"بچت چطوره؟","How is your child?"),
            new Quiz(22,"بچه هات چطورن؟","How are your children?"),
            new Quiz(23,"پسرت چطوره؟","How is your son?"),
            new Quiz(24,"دخترت چطوره؟","How is your daughter?"),
            new Quiz(25,"مادبزرگت چطوره؟","How is your grand mother?"),
            new Quiz(26,"پدربزرگت چطوره؟","How is your grand father?"),
            new Quiz(27,"نوه ات چطوره؟","How is your grand child?"),
            new Quiz(28,"نوه ات چطوره؟(پسر)","How is your grand son?"),
            new Quiz(29,"نوه ات چطوره؟(دختر)","How is your grand daughter?"),
            new Quiz(30,"نوه هات چطورن؟","How are your grand children?"),
            new Quiz(31,"عموت/داییت چطوره؟","How is your uncle?"),
            new Quiz(32,"خاله ات/عمه ات چطوره؟","How is your aunt?"),
            new Quiz(33,"پسر دایی ات/دختردایی ات/پسر خاله ات چطوره؟"," How  is  your cousin?")
    };

    boolean isFirst=true;

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_MIC = 3;
    static private final int STATE_CORRECT= 4;
    static private final int STATE_INCORRECT = 5;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    int sno=0;

    private Model model;
    private SpeechService speechService;
    private TextView quizView,resultView,stateView,scoreView,ScoresView,cpt;
    private RatingBar ratingBar;
    private ProgressBar progressBar;
    private ImageButton mic,next,skip;
    private LinearLayout ScoreBar;
    String[] sent;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);


        // Setup layout
        quizView = findViewById(R.id.question_text);
        quizView.setText(Q[sno].getQuestion());
        resultView = findViewById(R.id.result_text);
        resultView.setText("?");
        stateView = findViewById(R.id.state_text);
        ScoresView = findViewById(R.id.scores_text);
        cpt= findViewById(R.id.cp_text);
        ScoreBar = findViewById(R.id.score_bar);
        scoreView = findViewById(R.id.score_text);
        ratingBar = findViewById(R.id.ratingBar);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(100);
        mic=findViewById(R.id.recognize_mic);
        next=findViewById(R.id.next);
        skip=findViewById(R.id.skip);
        setUiState(STATE_START);
        mic.setOnClickListener(view -> recognizeMicrophone());
        next.setOnClickListener(view -> {
            if (sno<Q.length) {
                isFirst=true;
                sno++;
                sent = Q[sno].getAnswer().substring(0, Q[sno].getAnswer().length() - 1).toLowerCase().split(" ");
                setUiState(STATE_READY);
            }
            else
                Toast.makeText(this, "پایان", Toast.LENGTH_LONG).show();
        });
        skip.setOnClickListener(view -> {
            if (sno<Q.length) {
                isFirst=true;
                sno++;
                sent = Q[sno].getAnswer().substring(0, Q[sno].getAnswer().length() - 1).toLowerCase().split(" ");
                setUiState(STATE_READY);
            }
            else
                Toast.makeText(this, "پایان", Toast.LENGTH_LONG).show();
        });

        sent=Q[sno].getAnswer().substring(0,Q[sno].getAnswer().length()-1).toLowerCase().split(" ");
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
            resultView.append(Html.fromHtml("<font color='"+sentence.getLast().getColor()+"'>"+Q[sno].getAnswer().charAt(Q[sno].getAnswer().length()-1)+" </font>"));

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
            resultView.setText(Q[sno].getAnswer());
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


    @SuppressLint("SetTextI18n")
    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                stateView.setText(R.string.preparing);
                mic.setBackgroundResource(R.drawable.mic_back2);
                mic.setEnabled(false);
                next.setVisibility(View.GONE);
                skip.setVisibility(View.GONE);
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.GONE);
                break;
            case STATE_READY:
                double d=(double)(sno)/Q.length;
                int pb= (int) (d* 100);
                stateView.setText(R.string.ready);
                mic.setImageResource(R.drawable.mic_icon);
                mic.setBackgroundResource(R.drawable.mic_back);
                progressBar.setProgress(pb);
                cpt.setText(pb+"%");
                resultView.setTextColor(Color.BLACK);
                resultView.setText("?");
                quizView.setText(Q[sno].getQuestion());
                next.setVisibility(View.GONE);
                skip.setVisibility(View.GONE);
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.GONE);
                mic.setEnabled(true);
                break;
            case STATE_DONE:
                mic.setImageResource(R.drawable.mic_icon);
                mic.setBackgroundResource(R.drawable.mic_back);
                next.setVisibility(View.GONE);
                skip.setVisibility(View.GONE);
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.VISIBLE);
                mic.setEnabled(true);
                break;
            case STATE_INCORRECT:
                isFirst=false;
                mic.setImageResource(R.drawable.ic_baseline_refresh);
                mic.setBackgroundResource(R.drawable.mic_back);
                ScoreBar.setVisibility(View.GONE);
                ScoresView.setVisibility(View.VISIBLE);
                stateView.setText(getString(R.string.incorrect));
                next.setVisibility(View.GONE);
                skip.setVisibility(View.VISIBLE);
                mic.setEnabled(true);
                break;
            case STATE_CORRECT:
                mic.setImageResource(R.drawable.ic_baseline_refresh);
                mic.setBackgroundResource(R.drawable.mic_back);
                ScoreBar.setVisibility(View.VISIBLE);
                ScoresView.setVisibility(View.VISIBLE);
                stateView.setText(getString(R.string.correct));
                next.setVisibility(View.VISIBLE);
                skip.setVisibility(View.GONE);
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
                skip.setVisibility(View.GONE);
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
            new CountDownTimer(2400,800) {
                @Override
                public void onTick(long millisUntilFinished) {
                    resultView.setText(""+(int)((millisUntilFinished/800)+1));
                }

                @Override
                public void onFinish() {
                    if (isFirst)
                        resultView.setText("?");
                    else
                        resultView.setText(Q[sno].getAnswer());

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