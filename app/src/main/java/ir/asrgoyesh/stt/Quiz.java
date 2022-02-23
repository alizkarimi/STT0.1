package ir.asrgoyesh.stt;

public class Quiz {
    private int ID;
    private String question;
    private String answer;
    private int qVoice;
    private int aVoice;
    public Quiz(int id,String q,String a,int qv,int av){
        this.ID=id;
        this.question=q;
        this.answer=a;
        this.qVoice=qv;
        this.aVoice=av;

    }

    public int getID() {
        return ID;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public int getQVoice() {
        return qVoice;
    }

    public int getAVoice() {
        return aVoice;
    }
}
