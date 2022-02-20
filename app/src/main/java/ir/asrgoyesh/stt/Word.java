package ir.asrgoyesh.stt;

import android.graphics.Color;
import android.text.Html;

public class Word {
    private String content;
    private double score;
    private String color;
    public Word(double score,String word){
        this.content=word;
        this.score=score*100;
        if (this.score>80){
            this.color="#209c05";
        }else if (this.score>60){
            this.color="#85e62c";
        }
        else if (this.score>40){
            this.color="#ffff00";
        }
        else if (this.score>20){
            this.color="#ffca3a";
        }else{
            this.color="#ff2128";
        }


    }

    public double getScore() {
        return score;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        String w="<font color='"+color+"'>"+content+" </font>";
        return w;
    }
}
