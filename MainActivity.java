package com.aistudymaster.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    int purple = 0xFF40358B, ink = 0xFF20233A, muted = 0xFF666B80, pale = 0xFFF4F2FF;
    EditText topicInput, backendInput;
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    Spinner subjectSpinner, languageSpinner, lengthSpinner;
    TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF30276D);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xFFF8F8FC);
        ScrollView scroll = new ScrollView(this); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(20), dp(22), dp(20), dp(24)); scroll.addView(page);

        TextView badge = text("✦  AI-POWERED LEARNING", 12, 0xFF6C5CE7, true); page.addView(badge);
        TextView title = text("AI Study Master", 30, ink, true); LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2); tp.topMargin=dp(8); page.addView(title,tp);
        TextView sub = text("Turn any topic into colourful, exam-ready study notes.", 15, muted, false); sub.setLineSpacing(dp(3),1f); tp = new LinearLayout.LayoutParams(-1,-2); tp.topMargin=dp(6); page.addView(sub,tp);
        TextView chip = text("📘  HISTORY     🌍  GEOGRAPHY     🧠  REASONING", 11, purple, true); chip.setBackgroundColor(0xFFEAE7FF); chip.setPadding(dp(10),dp(12),dp(10),dp(12)); tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(18);page.addView(chip,tp);

        label(page,"SUBJECT"); subjectSpinner=spinner(new String[]{"History","Geography","English","Reasoning","Science","Mathematics","Indian Polity","Computer","Current Affairs","Other"}); page.addView(subjectSpinner,fieldParams());
        label(page,"TOPIC YOU WANT TO LEARN"); topicInput=new EditText(this); topicInput.setSingleLine(false); topicInput.setMinLines(2); topicInput.setHint("e.g. Mughal Empire, rivers of India, tenses..."); topicInput.setTextSize(16); topicInput.setPadding(dp(14),dp(12),dp(14),dp(12)); topicInput.setBackground(roundBg(0xFFFFFFFF)); page.addView(topicInput,fieldParams());
        label(page,"AI BACKEND URL (HTTPS) — optional until deployed"); backendInput=new EditText(this); backendInput.setSingleLine(true); backendInput.setText("https://YOUR-SERVER.example/generate"); backendInput.setHint("https://your-server.com/generate"); backendInput.setTextSize(14); backendInput.setPadding(dp(14),dp(12),dp(14),dp(12)); backendInput.setBackground(roundBg(0xFFFFFFFF)); page.addView(backendInput,fieldParams());
        label(page,"PDF LANGUAGE"); languageSpinner=spinner(new String[]{"English","Bengali (বাংলা)","English + Bengali bilingual"}); page.addView(languageSpinner,fieldParams());
        label(page,"DETAIL LEVEL"); lengthSpinner=spinner(new String[]{"AI decides based on topic","Quick Notes (5–10 pages)","Complete Concept (15–30 pages)","Master Guide (30–100+ pages)"}); page.addView(lengthSpinner,fieldParams());

        Button generate = new Button(this); generate.setText("✦  CREATE STUDY PDF"); generate.setTextSize(15); generate.setTextColor(0xFFFFFFFF); generate.setAllCaps(false); generate.setBackground(roundBg(purple)); tp=new LinearLayout.LayoutParams(-1,dp(54));tp.topMargin=dp(22);page.addView(generate,tp);
        status=text("Enter a topic and configure your deployed HTTPS AI backend to generate real study content. If no backend is configured, the app can create a sample template.",12,muted,false); status.setLineSpacing(dp(3),1f);tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(12);page.addView(status,tp);
        TextView footer=text("ENGLISH • বাংলা  |  Learn visually, remember better",12,muted,true);footer.setGravity(Gravity.CENTER);tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(24);page.addView(footer,tp);
        generate.setOnClickListener(v -> generateStudyGuide()); setContentView(root);
    }


    void generateStudyGuide() {
        String topic=topicInput.getText().toString().trim();
        if(topic.isEmpty()){topicInput.setError("Enter a topic first");topicInput.requestFocus();return;}
        String endpoint=backendInput.getText().toString().trim();
        if(!endpoint.startsWith("https://") || endpoint.contains("YOUR-SERVER")) {
            new AlertDialog.Builder(this).setTitle("AI backend not connected")
              .setMessage("To generate real AI notes, first deploy the backend/app.py file to an HTTPS server, then paste its /generate URL here. For now, you can create the sample PDF.")
              .setNegativeButton("Cancel",null).setPositiveButton("Create sample PDF",(d,w)->createPdf()).show();
            return;
        }
        final String chosenSubject=subjectSpinner.getSelectedItem().toString();
        final String chosenLanguage=languageSpinner.getSelectedItem().toString();
        final String chosenDetail=lengthSpinner.getSelectedItem().toString();
        status.setText("Generating your study guide… Please keep the app open.");
        executor.execute(() -> {
            HttpURLConnection conn=null;
            try {
                URL url=new URL(endpoint); conn=(HttpURLConnection)url.openConnection(); conn.setRequestMethod("POST"); conn.setConnectTimeout(20000); conn.setReadTimeout(120000); conn.setDoOutput(true); conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                JSONObject body=new JSONObject(); body.put("topic",topic); body.put("subject",chosenSubject); body.put("language",chosenLanguage); body.put("detail",chosenDetail);
                try(OutputStream os=conn.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
                int code=conn.getResponseCode(); BufferedReader br=new BufferedReader(new InputStreamReader(code>=200&&code<300?conn.getInputStream():conn.getErrorStream(),StandardCharsets.UTF_8)); StringBuilder response=new StringBuilder(); String line; while((line=br.readLine())!=null)response.append(line); br.close();
                JSONObject json=new JSONObject(response.toString());
                if(code<200||code>=300) throw new Exception(json.optString("error","Backend error: "+code));
                String content=json.optString("content",""); if(content.trim().isEmpty())throw new Exception("AI returned empty content");
                File file=writeContentPdf(topic,content);
                runOnUiThread(()->{status.setText("AI study guide created successfully."); new AlertDialog.Builder(this).setTitle("Your study PDF is ready!").setMessage("Saved to:\n"+file.getAbsolutePath()).setPositiveButton("OK",null).show();});
            } catch(Exception ex){String message=ex.getMessage()==null?"Connection failed":ex.getMessage();runOnUiThread(()->{status.setText("Could not generate AI PDF. Check backend URL and internet connection.");new AlertDialog.Builder(this).setTitle("AI generation failed").setMessage(message+"\n\nYou can still create the sample PDF while troubleshooting.").setNegativeButton("Close",null).setPositiveButton("Sample PDF",(d,w)->createPdf()).show();});}
            finally {if(conn!=null)conn.disconnect();}
        });
    }

    File writeContentPdf(String topic,String content) throws Exception {
        File dir=new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"AIStudyMaster"); if(!dir.exists()&&!dir.mkdirs())throw new Exception("Cannot create PDF folder");
        String safe=topic.replaceAll("[^A-Za-z0-9_-]","_");if(safe.length()>35)safe=safe.substring(0,35);File out=new File(dir,"AI_StudyGuide_"+safe+".pdf");
        PdfDocument pdf=new PdfDocument(); Paint paint=new Paint(3); int pageNo=0; int y=0; PdfDocument.Page page=null; Canvas canvas=null;
        String[] paragraphs=content.replace("\r","").split("\n");
        for(String paragraph:paragraphs){
            String line=paragraph.trim(); if(page==null||y>760){if(page!=null)finishPdfPage(pdf,page,paint,pageNo);pageNo++;page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());canvas=page.getCanvas();canvas.drawColor(0xFFFFFFFF);paint.setColor(purple);canvas.drawRect(0,0,595,82,paint);paint.setColor(0xFFFFFFFF);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(20);canvas.drawText("AI STUDY MASTER",32,48,paint);y=115;}
            if(line.isEmpty()){y+=10;continue;}
            boolean heading=line.startsWith("#")||line.startsWith("**");String clean=line.replaceAll("^#+\\s*","").replace("**","").replace("`","");
            paint.setColor(heading?purple:ink);paint.setTypeface(Typeface.create("sans-serif",heading?Typeface.BOLD:Typeface.NORMAL));paint.setTextSize(heading?17:12.5f);
            for(String part:wrap(clean,heading?48:78)){if(y>770){finishPdfPage(pdf,page,paint,pageNo);pageNo++;page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());canvas=page.getCanvas();canvas.drawColor(0xFFFFFFFF);paint.setColor(purple);canvas.drawRect(0,0,595,62,paint);paint.setColor(0xFFFFFFFF);paint.setTextSize(18);canvas.drawText("AI STUDY MASTER • CONTINUED",32,40,paint);y=90;paint.setColor(ink);paint.setTextSize(12.5f);}canvas.drawText(part,32,y,paint);y+=heading?28:19;}
            y+=5;
        }
        if(page!=null)finishPdfPage(pdf,page,paint,pageNo);try(FileOutputStream fos=new FileOutputStream(out)){pdf.writeTo(fos);}pdf.close();return out;
    }
    void finishPdfPage(PdfDocument pdf,PdfDocument.Page page,Paint paint,int pageNo){Canvas c=page.getCanvas();paint.setColor(muted);paint.setTextSize(9);paint.setTypeface(Typeface.DEFAULT);c.drawText("AI Study Master • Verify important facts with trusted sources • Page "+pageNo,32,820,paint);pdf.finishPage(page);}

    void createPdf() {
        String topic=topicInput.getText().toString().trim();
        if(topic.isEmpty()){topicInput.setError("Enter a topic first");topicInput.requestFocus();return;}
        String subject=subjectSpinner.getSelectedItem().toString();
        String language=languageSpinner.getSelectedItem().toString();
        String level=lengthSpinner.getSelectedItem().toString();
        try {
            File dir=new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"AIStudyMaster"); if(!dir.exists()&&!dir.mkdirs()) throw new Exception("Could not create output folder");
            String safe=topic.replaceAll("[^A-Za-z0-9_-]","_"); if(safe.length()>35)safe=safe.substring(0,35);
            File out=new File(dir,"StudyGuide_"+safe+".pdf");
            PdfDocument pdf=new PdfDocument();
            List<String[]> pages=new ArrayList<>();
            pages.add(new String[]{"AI STUDY MASTER",subject+" • "+topic,"YOUR ILLUSTRATED STUDY GUIDE","Language: "+language,"Detail: "+level,"\nThis starter PDF is a sample layout. Connect the AI backend to generate verified, topic-specific lessons, diagrams and questions."});
            pages.add(new String[]{"1. TOPIC OVERVIEW","Topic: "+topic,"What to learn","• Understand the meaning and background of the topic.","• Learn important terms, people, dates, places or rules.","• Connect the main ideas with examples.","Study tip: Read the overview first, then make a short summary in your own words."});
            pages.add(new String[]{"2. KEY CONCEPTS","Core idea","Break the topic into 3–5 small ideas. For each idea, write its definition, why it matters, and one example.","Important vocabulary","Record unfamiliar words and their meanings. For history, include dates and events; for geography, include locations and processes; for English, include rules and examples; for reasoning, include the solving steps."});
            pages.add(new String[]{"3. VISUAL MEMORY MAP","MAIN TOPIC","↙                 ↓                 ↘","Key idea 1       Key idea 2       Key idea 3","\nAdd a labelled diagram, timeline, map or cartoon illustration relevant to this topic when AI image generation is connected.","Memory trick: Link each key idea to a simple picture or familiar example."});
            pages.add(new String[]{"4. REVISION NOTES","Remember these points","1. Define the topic in one or two sentences.","2. List the most important facts or rules.","3. Write down common mistakes to avoid.","4. Explain the topic aloud without looking at your notes.","5. Review again after one day and one week."});
            pages.add(new String[]{"5. PRACTICE QUESTIONS","Check your understanding","Q1. What is the main idea of "+topic+"?","Q2. Which three facts or rules are most important?","Q3. Give one example related to this topic.","Q4. Explain the topic in your own words.","\nSample answer guide: Answers must be based on the actual topic content. This template intentionally does not invent factual answers."});
            int pageNo=0;
            for(String[] lines:pages){pageNo++; PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,pageNo).create(); PdfDocument.Page p=pdf.startPage(info); Canvas c=p.getCanvas(); Paint paint=new Paint(3); c.drawColor(0xFFFFFFFF); paint.setColor(purple); c.drawRect(0,0,595,105,paint); paint.setColor(0xFFFFFFFF);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(pageNo==1?25:21); c.drawText(lines[0],36,55,paint); paint.setColor(ink);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(14); int y=145; for(int i=1;i<lines.length;i++){String line=lines[i]; if(line.length()==0){y+=15;continue;} paint.setColor((i==1)?0xFF5B50D6:ink);paint.setTextSize(i==1?18:14);paint.setTypeface(i==1?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("sans-serif",Typeface.NORMAL)); for(String part:wrap(line,66)){c.drawText(part,38,y,paint);y+=25;} y+=7; if(y>770)break;} paint.setColor(muted);paint.setTextSize(10);c.drawText("AI Study Master • Sample template • Page "+pageNo,38,815,paint);pdf.finishPage(p);}
            FileOutputStream fos=new FileOutputStream(out);pdf.writeTo(fos);fos.close();pdf.close();
            new AlertDialog.Builder(this).setTitle("PDF created") .setMessage("A sample study guide has been saved here:\n\n"+out.getAbsolutePath()+"\n\nThis starter version uses a template, not live AI. Configure the backend to generate real topic-specific content.") .setPositiveButton("OK",(d,w)->{}).show();
        } catch(Exception e){Toast.makeText(this,"Could not create PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    List<String> wrap(String s,int max){List<String> out=new ArrayList<>();StringBuilder b=new StringBuilder();for(String word:s.split(" ")){if(b.length()+word.length()+1>max){out.add(b.toString());b=new StringBuilder();}if(b.length()>0)b.append(' ');b.append(word);}if(b.length()>0)out.add(b.toString());return out;}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    void label(LinearLayout p,String s){TextView t=text(s,12,muted,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(18);lp.bottomMargin=dp(7);p.addView(t,lp);}
    Spinner spinner(String[] vals){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,vals);s.setAdapter(a);s.setBackground(roundBg(0xFFFFFFFF));s.setPadding(dp(10),0,dp(10),0);return s;}
    LinearLayout.LayoutParams fieldParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));return p;}
    android.graphics.drawable.GradientDrawable roundBg(int color){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));d.setStroke(dp(1),0xFFE0DFF0);return d;}
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}
