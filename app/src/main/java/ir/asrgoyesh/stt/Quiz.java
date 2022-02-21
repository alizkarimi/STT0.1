package ir.asrgoyesh.stt;

public class Quiz {
    private int ID;
    private String question;
    private String answer;
    public Quiz(int id,String q,String a){
        this.ID=id;
        this.question=q;
        this.answer=a;
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
}
