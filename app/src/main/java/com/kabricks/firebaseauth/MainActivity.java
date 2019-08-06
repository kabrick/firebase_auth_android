package com.kabricks.firebaseauth;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    // create map value for country codes
    Map<String, String> country_codes_map = new HashMap<>();
    TextView user_country;
    EditText user_country_code, user_phone_number;
    String phoneNumber = "+";
    String verification_id;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        user_phone_number = findViewById(R.id.user_phone_number);
        user_country_code = findViewById(R.id.user_country_code);
        user_country = findViewById(R.id.user_country);

        mAuth = FirebaseAuth.getInstance();

        // add the country codes;
        country_codes_map.put("UG", "256");
        country_codes_map.put("CA", "1");
        country_codes_map.put("US", "1");
        country_codes_map.put("FR", "33");
        country_codes_map.put("DE", "49");
        country_codes_map.put("KE", "254");
        country_codes_map.put("NG", "234");
        country_codes_map.put("RW", "250");
        country_codes_map.put("ZA", "27");
        country_codes_map.put("ES", "34");
        country_codes_map.put("TZ", "255");

        // get user current country
        TelephonyManager tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        Locale locale = new Locale("", tm.getSimCountryIso());
        user_country.setText(locale.getDisplayCountry());
        user_country_code.setText(country_codes_map.get(locale.getCountry()));

        phoneNumber += country_codes_map.get(locale.getCountry());
    }

    public void verifyUser(View view){
        phoneNumber += user_phone_number.getText().toString();

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks

        // reset the phone number string
        phoneNumber = "+";
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

            //Getting the code sent by SMS
            String code = phoneAuthCredential.getSmsCode();

            if (code != null) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verification_id, code);
                continueRegistration(credential);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            //storing the verification id that is sent to the user
            verification_id = verificationId;
            displayVerificationDialog();
        }
    };

    public void displayVerificationDialog(){
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.layout_verification_code, null);
        final EditText verification_code = view.findViewById(R.id.verification_code);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        builder.setCancelable(false)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // check if verification code is correct
                        String code = verification_code.getText().toString().trim();

                        if (code.isEmpty() || code.length() < 6) {
                            verification_code.setError("Enter valid code");
                            verification_code.requestFocus();
                        } else {
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verification_id, code);
                            continueRegistration(credential);
                        }
                    }
                });

        final Dialog dialog = builder.create();
        dialog.show();
    }

    public void continueRegistration(PhoneAuthCredential credential){
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            //verification successful, start the profile activity
                            Intent intent = new Intent(MainActivity.this, UserDetailsActivity.class);
                            startActivity(intent);
                        } else {
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(MainActivity.this, "Invalid code entered", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Something went wrong. Please try again", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }
}
