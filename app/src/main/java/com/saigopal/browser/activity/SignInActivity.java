package com.saigopal.browser.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.saigopal.browser.R;
import com.saigopal.browser.unit.HelperUnit;

import java.util.Objects;

public class SignInActivity extends AppCompatActivity {

    private Button Backup,LogOut;
    private SignInButton SignIn;
    private ImageButton Close;
    private TextView SignInText;
    private static final int RC_SIGN_IN = 0;
    private static final String TAG = "signIn";
    public GoogleSignInClient mGoogleSignInClient;
    public FirebaseAuth mAuth;
    private ProgressDialog pm;
    private LinearLayout SignInLay,BackUpLay;
    private Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = SignInActivity.this;
        HelperUnit.applyTheme(context);
        setContentView(R.layout.activity_sign_in);

        SignIn = findViewById(R.id.btn_sign);
        Close = findViewById(R.id.btn_sign_close);
        Backup = findViewById(R.id.backup);
        SignInText = findViewById(R.id.Sign_details);
        SignInLay = findViewById(R.id.sign_lay);
        BackUpLay = findViewById(R.id.backup_lay);
        LogOut = findViewById(R.id.logout);

        pm = new ProgressDialog(this);
        pm.setMessage("Loading...");

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (mAuth.getCurrentUser() != null) {
           SignInLay.setVisibility(View.GONE);
           BackUpLay.setVisibility(View.VISIBLE);
           SignInText.setText("SignedIn as : "+mAuth.getCurrentUser().getDisplayName());
        }

        Close.setOnClickListener(v ->
                SignInActivity.super.onBackPressed());

        LogOut.setOnClickListener(v ->
                mGoogleSignInClient.signOut().addOnCompleteListener(SignInActivity.this,
                task -> {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(getBaseContext(), "Sign-Out", Toast.LENGTH_LONG).show();
                    recreate();
                }));

        SignIn.setOnClickListener(v -> signIn());




    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);

            } catch (ApiException e)
            {
                Toast.makeText(SignInActivity.this, "ERROR CODE "+e.getMessage(), Toast.LENGTH_SHORT).show();
                Toast.makeText(SignInActivity.this,"Login failed\nPlease try again",Toast.LENGTH_SHORT).show();
                pm.dismiss();
            }
        }
    }


    public void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            pm.dismiss();
                            SignInLay.setVisibility(View.GONE);
                            BackUpLay.setVisibility(View.VISIBLE);
                            SignInText.setText("SignedIn as : "+mAuth.getCurrentUser().getDisplayName());
                        } else {
                            pm.dismiss();
                            Toast.makeText(SignInActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();

                        }

                    }
                });
    }


    private void signIn() {
        pm.show();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}