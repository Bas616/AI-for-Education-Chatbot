package com.example.aiforeducation;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private LinearLayout chatMessages;
    private EditText userInput;
    private Button sendButton;
    private Button languageButton;
    private TextView guideText;
    private boolean isEnglish = true;
    private Handler handler = new Handler(Looper.getMainLooper());
    private List<String> responses = new ArrayList<>();
    private Map<String, String> knowledgeBase = new HashMap<>();
    private Animation slideInRightAnimation;
    private Animation slideInLeftAnimation;
    private MediaPlayer button01aPlayer;
    private MediaPlayer correctAnswer1Player;
    private MediaPlayer blip01Player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatMessages = findViewById(R.id.chat_messages);
        userInput = findViewById(R.id.user_input);
        sendButton = findViewById(R.id.send_button);
        languageButton = findViewById(R.id.language_button);
        guideText = findViewById(R.id.guide_text);

        // Load animations
        slideInRightAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideInLeftAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);

        // Initialize MediaPlayers
        button01aPlayer = MediaPlayer.create(this, R.raw.button01a);
        correctAnswer1Player = MediaPlayer.create(this, R.raw.correctanswer1);
        blip01Player = MediaPlayer.create(this, R.raw.blip01);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button01aPlayer.start(); // Play button click sound
                sendMessage();
            }
        });

        languageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchLanguage();
            }
        });

        initializeKnowledgeBase();
        // loadResponses();  No need to load from story.txt anymore
        setGuideText();

        // Add this line to your onCreate() method
        loadOkHttpDependency();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release MediaPlayers to avoid memory leaks
        button01aPlayer.release();
        correctAnswer1Player.release();
        blip01Player.release();
    }

    private void initializeKnowledgeBase() {
        // ... (This can be removed or kept as needed)
    }

    private void sendMessage() {
        final String userMessage = userInput.getText().toString().trim();
        if (!userMessage.isEmpty()) {
            displayMessage(userMessage, "user");
            userInput.setText("");

            // ส่งข้อความไปยัง ChatGPT API
            sendMessageToChatGPT(userMessage);
        }
    }

    private void sendMessageToChatGPT(String message) {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"" + message + "\"}]}");
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + "API-KEY")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // แสดงข้อความ error ใน UI thread
                        String noMatchResponse = isEnglish ? "Error: " + e.getMessage() : "เกิดข้อผิดพลาด: " + e.getMessage();
                        displayMessage(noMatchResponse, "bot");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray choicesArray = jsonObject.getJSONArray("choices");
                        JSONObject firstChoice = choicesArray.getJSONObject(0);
                        JSONObject messageObject = firstChoice.getJSONObject("message");
                        String content = messageObject.getString("content");

                        // แสดงผลข้อความตอบกลับใน UI thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                correctAnswer1Player.start();
                                displayMessage(content, "bot");
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // แสดงข้อความ error ใน UI thread
                                String noMatchResponse = isEnglish ? "Error parsing JSON: " + e.getMessage() : "เกิดข้อผิดพลาดในการแยกวิเคราะห์ JSON: " + e.getMessage();
                                displayMessage(noMatchResponse, "bot");
                            }
                        });
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // แสดงข้อความ error ใน UI thread
                            String noMatchResponse = isEnglish ? "Error: " + response.code() + " " + response.message() : "เกิดข้อผิดพลาด: " + response.code() + " " + response.message();
                            displayMessage(noMatchResponse, "bot");
                        }
                    });
                }
            }
        });
    }

    private void displayMessage(String message, String sender) {
        TextView messageView = new TextView(this);
        messageView.setText(message);
        messageView.setPadding(16, 16, 16, 16);

        if (sender.equals("user")) {
            messageView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            messageView.setBackgroundColor(0xFF000000); // Black for user
            messageView.setTextColor(0xFFFFFFFF); // White text color
            messageView.startAnimation(slideInRightAnimation);
        } else {
            messageView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            messageView.setBackgroundColor(0xFF444444); // Dark gray for bot
            messageView.setTextColor(0xFFFFFF00); // Yellow text color
            messageView.startAnimation(slideInLeftAnimation);
        }

        chatMessages.addView(messageView);
    }

    private void switchLanguage() {
        isEnglish = !isEnglish;
        languageButton.setText(isEnglish ? "EN" : "TH");
    }

    private void setGuideText() {
        String guide = "ตัวอย่าง Input:\n" +
                "  - สวัสดี\n" +
                "  - hello\n" +
                "  - generative ai\n" +
                "  - what is chatgpt\n" +
                "  - google gemini คืออะไร\n" +
                "  - claude ai คืออะไร\n" +
                "  - what is a chatbot\n" +
                "  - nlp คืออะไร\n" +
                "  - what is huggingface\n" +
                "  - kaggle คืออะไร\n" +
                "  - ai ในการศึกษา\n" +
                "  - examples of ai in education\n" +
                "  - 2 + 2\n" +
                "  - 5 - 3\n" +
                "  - 4 * 6\n" +
                "  - 10 / 2\n" +
                "  - sqrt(16)\n\n" +

                "ตัวอย่าง Output:\n" +
                "  - สวัสดีค่ะ มีอะไรให้ฉันช่วยเหลือเกี่ยวกับ AI ในด้านการศึกษาบ้างคะ\n" +
                "  - Hi there! How can I help you with AI in education?\n" +
                "  - Generative AI เป็น AI ประเภทหนึ่งที่สามารถสร้างเนื้อหาใหม่ได้ เช่น ข้อความ รูปภาพ หรือแม้แต่โค้ด\n" +
                "  - ChatGPT is a large language model chatbot trained by Google.\n" +
                "  - Google Gemini เป็นแบบจำลองภาษาขนาดใหญ่ ฝึกฝนโดย Google\n" +
                "  - Claude AI เป็นแชทบอทที่พัฒนาโดย Anthropic\n" +
                "  - A chatbot is an AI program that can simulate conversation with human users.\n" +
                "  - NLP ย่อมาจาก Natural Language Processing เป็นสาขาหนึ่งของ AI ที่เน้นการทำให้คอมพิวเตอร์เข้าใจและประมวลผลภาษาของมนุษย์\n" +
                "  - Hugging Face is a platform that provides tools and resources for natural language processing and machine learning.\n" +
                "  - Kaggle เป็นแพลตฟอร์มสำหรับการแข่งขันวิทยาศาสตร์ข้อมูลและการเรียนรู้ของเครื่อง\n" +
                "  - AI ในการศึกษามีประโยชน์มากมาย เช่น การเรียนรู้แบบปรับให้เป็นส่วนตัว การสอนแบบตัวต่อตัว และเครื่องมือสำหรับการสร้างแหล่งข้อมูลทางการศึกษา\n" +
                "  - Examples of AI in education include personalized learning platforms, AI tutors, and tools for creating educational resources.\n" +
                "  - 4\n" +
                "  - 2\n" +
                "  - 24\n" +
                "  - 5\n" +
                "  - 4";

        guideText.setText(guide);
    }

    // Add this method to your MainActivity class
    private void loadOkHttpDependency() {
        try {
            Class.forName("okhttp3.OkHttpClient");
        } catch (ClassNotFoundException e) {
            // If OkHttp is not found, show an error message
            Snackbar.make(findViewById(android.R.id.content),
                    "OkHttp dependency not found. Please add it to your project.",
                    Snackbar.LENGTH_LONG).show();
        }
    }
}