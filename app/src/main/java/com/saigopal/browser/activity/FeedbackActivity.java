package com.saigopal.browser.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.saigopal.browser.R;
import com.saigopal.browser.unit.HelperUnit;
import com.saigopal.browser.view.Toasty;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedbackActivity extends AppCompatActivity {

    private MaterialCheckBox ResponseEmail;
    private TextInputLayout EmailInputLayout;
    private RadioGroup FeedbackType;
    private TextInputEditText Feedback,Email;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        String showNavButton = Objects.requireNonNull(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("sp_theme", "0"));
        if (showNavButton.equals("3")){
            this.setTheme(R.style.AppTheme_dark);
        }else HelperUnit.applyTheme(this);
        setContentView(R.layout.activity_feedback);

        ResponseEmail = findViewById(R.id.response_checkbox);
        EmailInputLayout = findViewById(R.id.email_input_layout);
        ImageButton close = findViewById(R.id.close);
        MaterialButton send = findViewById(R.id.send_feedback);
        FeedbackType = findViewById(R.id.feedback_type_group);
        Feedback = findViewById(R.id.Feedback);
        Email = findViewById(R.id.email);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Feedback");

        close.setOnClickListener(v -> FeedbackActivity.super.onBackPressed());

        ResponseEmail.setOnCheckedChangeListener(
                (buttonView, isChecked) -> EmailInputLayout.setEnabled(isChecked)
        );

        send.setOnClickListener(v -> {

            String feedbackTypeText = ((RadioButton)findViewById(FeedbackType.getCheckedRadioButtonId())).getText().toString();
            String FeedbackText = Objects.requireNonNull(Feedback.getText()).toString().trim();

            if (!FeedbackText.isEmpty() && FeedbackText.length() > 15){

                if (ResponseEmail.isChecked()) {
                    String EmailId = Objects.requireNonNull(Email.getText()).toString().trim();

                    if (EmailId.isEmpty()){
                        Toasty.show(FeedbackActivity.this,"Enter Email");
                    }else {
                        if (isEmailValid(EmailId)) {
                            SendFeedback(feedbackTypeText, FeedbackText, EmailId);
                        }else Toasty.show(FeedbackActivity.this,"Enter valid Email");
                    }

                }else SendFeedback(feedbackTypeText,FeedbackText,null);

            }else Toasty.show(FeedbackActivity.this,"Feedback must contains 15 or more characters");

        });


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void SendFeedback(String Type,String Feedback,String Email){

        ProgressDialog ps = new ProgressDialog(this);
        ps.setMessage("Sending...");
        ps.show();
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("dd MM yyyy, HH:mm");
        String date = df.format(Calendar.getInstance().getTime());
        DatabaseReference db = databaseReference.child(Type).push();
        db.child("Time").setValue(date);
        db.child("Feedback").setValue(Feedback).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (Email != null) {
                    db.child("Email").setValue(Email).addOnSuccessListener(aVoid ->
                            ps.dismiss()
                    );
                }ps.dismiss();
                Toasty.show(FeedbackActivity.this,"Thank you for the feedback");
            }else {
                ps.dismiss();
                Toasty.show(FeedbackActivity.this,"Error can't send feedback");
            }
        });
    }

    public static boolean isEmailValid(String email) {
        String expression = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}