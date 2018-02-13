package com.example.avjindersinghsekhon.minimaltodo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.avjindersinghsekhon.minimaltodo.utils.NetworkUtils;
import com.sumup.merchant.api.SumUpAPI;
import com.sumup.merchant.api.SumUpLogin;
import com.sumup.merchant.api.SumUpPayment;
import com.sumup.merchant.api.SumUpState;
import com.sumup.merchant.Models.TransactionInfo;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.UUID;

public class SumupActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_PAYMENT = 2;

    private static final String PREF_AUTH_CODE = "auth-code";
    private static final String PREF_TOKEN = "token";
    private static final String PREF_TOKEN_EXPIRY_EPOCH = "token-expiry-epoch"; //In ms

    //Action codes for token-dependent operations
    private static final int ACTION_LOGIN = 1;
    private static final int ACTION_RECEIPT = 2;
    private static final int ACTION_NONE = 3;

    private SharedPreferences pref;

    private ScrollView outputScrollView;
    private TextView outputTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SumUpState.init(this);
        setContentView(R.layout.activity_sumup);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        handleAuthIntent();

        //TODO enable Themes?

        Button login = (Button) findViewById(R.id.button_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initApiLogin(true);
            }
        });

        final Button btnCharge = (Button) findViewById(R.id.button_charge);
        btnCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("onClickedd", "check");
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int windowWidth = metrics.widthPixels;
                int windowHeight = metrics.heightPixels;

                final LayoutInflater inflater = (LayoutInflater) SumupActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final float density = SumupActivity.this.getResources().getDisplayMetrics().density;
                View contactLayout = inflater.inflate(R.layout.contact_entry_layout, null);

                final PopupWindow contactsPopupWindow = new PopupWindow(contactLayout, windowWidth, LinearLayout.LayoutParams.WRAP_CONTENT, true);
                //TODO set correct theme
                contactsPopupWindow.setBackgroundDrawable(
                        new ColorDrawable(getResources().getColor(R.color.accent)));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    contactsPopupWindow.setElevation(100f);
                }

                TextView contactEntryMessageTv = contactLayout.findViewById(R.id.contact_entry_message);
                String message = getString(R.string.contact_entry_message)
                        + " "
                        + getString(R.string.payment_sum);
                contactEntryMessageTv.setText(message);
                EditText emailEditText = contactLayout.findViewById(R.id.email_edit_text);
                EditText phoneEditText = contactLayout.findViewById(R.id.phone_edit_text);
                final String email = emailEditText.getText().toString();
                final String phone = phoneEditText.getText().toString();
                Button confirmButton = contactLayout.findViewById(R.id.confirm_contacts_button);
                confirmButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SumUpPayment payment = SumUpPayment.builder()
                                // mandatory parameters
                                // Please go to https://me.sumup.com/developers to get your Affiliate Key by entering the application ID of your app. (e.g. com.sumup.sdksampleapp)
                                .affiliateKey(BuildConfig.SUMUP_AFF_KEY)
                                .total(new BigDecimal(getString(R.string.payment_sum))) // minimum 1.00
                                .currency(SumUpPayment.Currency.BGN)
                                // optional: add details
                                .title("Taxi Ride")
                                .receiptEmail(email)
                                .receiptSMS(phone)
                                // optional: Add metadata
                                .addAdditionalInfo("AccountId", "taxi0334")
                                .addAdditionalInfo("From", "Paris")
                                .addAdditionalInfo("To", "Berlin")
                                // optional: foreign transaction ID, must be unique!
                                .foreignTransactionId(UUID.randomUUID().toString()) // can not exceed 128 chars
                                .build();

                        SumUpAPI.checkout(SumupActivity.this, payment, REQUEST_CODE_PAYMENT);
                        contactsPopupWindow.dismiss();
                    }
                });
                contactsPopupWindow.showAtLocation(btnCharge, Gravity.CENTER, 0, 0);
            }
        });

        Button authRequestButton = (Button) findViewById(R.id.button_request_auth);
        authRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initAuth();
            }
        });

        Button tokenRequestButton = (Button) findViewById(R.id.button_request_token);
        tokenRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("auth_code", pref.getString(PREF_AUTH_CODE, "null"));
                //TODO enable POST request to get token
                Toast.makeText(SumupActivity.this,
                        "Under construction",
                        Toast.LENGTH_SHORT).show();
            }
        });

        Button receiptRequestButton = (Button) findViewById(R.id.button_request_receipt);
        receiptRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initReceipt("TDTPUNSPC3", BuildConfig.SUMUP_MERCHANT);
            }
        });

        outputTextView = findViewById(R.id.outputView);
        outputScrollView = findViewById(R.id.output_scroll_view);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) return;

        Bundle extra = data.getExtras();
        int apiResultCode = extra.getInt(SumUpAPI.Response.RESULT_CODE);
        updateOutput("Result Code: " + apiResultCode);

        if (requestCode == REQUEST_CODE_LOGIN) {
            switch (resultCode) {

                case SumUpAPI.Response.ResultCode.SUCCESSFUL: {
                    updateOutput("Login successful.");
                    break;
                }

                case SumUpAPI.Response.ResultCode.ERROR_INVALID_TOKEN: {
                    pref.edit().putLong(PREF_TOKEN_EXPIRY_EPOCH, 0).apply();
                    updateOutput("Invalid Token. Please try again.");
                    break;
                }
                case SumUpAPI.Response.ResultCode.ERROR_INVALID_PARAM: {
                    updateOutput("Invalid params.");
                    break;
                }
                case SumUpAPI.Response.ResultCode.ERROR_GEOLOCATION_REQUIRED: {
                    updateOutput("Please enable location on device.");
                    break;
                }
                case SumUpAPI.Response.ResultCode.ERROR_NO_CONNECTIVITY: {
                    updateOutput("Please enable Internet connection on device.");
                    break;
                }
                case SumUpAPI.Response.ResultCode.ERROR_ALREADY_LOGGED_IN: {
                    updateOutput("Already logged in.");
                    break;
                }
            }
        }

        if (requestCode == REQUEST_CODE_PAYMENT) {
            //Handle successful transaction
            if (apiResultCode == SumUpAPI.Response.ResultCode.SUCCESSFUL) {
                String txCode = extra.getString(SumUpAPI.Response.TX_CODE);
                TransactionInfo txInfo = extra.getParcelable(SumUpAPI.Response.TX_INFO);
                String merchantCode = txInfo.getMerchantCode();

                initReceipt(txCode, merchantCode);

                updateOutput("\nTransaction Code: " + txCode);
                updateOutput("\nTransaction Info: " + txInfo);
            }
            //TODO handle error cases?

            String apiResponseMessage = extra.getString(SumUpAPI.Response.MESSAGE);
        }
    }

    /**
     * AsyncTask to fetch receipt for a given transaction.
     *
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public class ReceiptQueryTask extends AsyncTask<URL, Void, String> {


        @Override
        protected String doInBackground(URL... params) {
            //TODO check if token is expired, evtl. get another one
            URL searchUrl = params[0];
            String receiptResponceFromSumup = null;
            Log.d("doInBcg", params[0].toString());

            try {
                receiptResponceFromSumup = NetworkUtils.requestReceiptFromUrl(
                        searchUrl,
                        pref.getString(PREF_TOKEN, "null")
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONObject object = null;

            try {
                object = new JSONObject(receiptResponceFromSumup);
                //TODO parse receipt JSON as per actual JSON structure. Dummy JSON getters below...
                object.getString("Receipt data 1");
                object.getString("Receipt data 2");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return receiptResponceFromSumup;
        }

        @Override
        protected void onPostExecute(String receipt) {
            if (receipt != null && !receipt.equals("")) {
//                outputTextView.append(receipt);
                updateOutput(receipt);
            } else {
                Log.d("nullResult", "check");
            }
        }
    }

    /**
     * AsyncTask to fetch Sumup token.
     *
     * @throws IOException Related to network and stream reading
     */
    public class TokenQueryTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            String auth_code = params[0];
            String tokenResponceFromSumup = null;
            String token = null;
            int ttl = 0;

            try {
                tokenResponceFromSumup = NetworkUtils.requestTokenFromUrl(auth_code);
                Log.d("TehTokenResponse", tokenResponceFromSumup + "");
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONObject object;

            try {
                object = new JSONObject(tokenResponceFromSumup);
                token = object.getString("access_token");
                ttl = object.getInt("expires_in");
                pref.edit().putString(PREF_TOKEN, token).apply();
                pref.edit().putLong(PREF_TOKEN_EXPIRY_EPOCH,
                        System.currentTimeMillis() + ttl * 1000).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException n) {
                n.printStackTrace();
            }

            return new String[]{token};
        }

        @Override
        protected void onPostExecute(String[] tokenData) {

            if ((tokenData.length >= 1) && tokenData[0] != null) {

                updateOutput("Token: " + tokenData[0]);
                initApiLogin(false);

            } else {
                Log.d("nullResult", "check");
            }
        }
    }


    private void initApiLogin(Boolean firstAttempt) {
        //Get new token, but avoid loop if it doesn't work
        if (firstAttempt && pref.getLong(PREF_TOKEN_EXPIRY_EPOCH, 0) <= System.currentTimeMillis()) {
            initAuth();
        } else {
            SumUpLogin sumupLogin = SumUpLogin
                    .builder(BuildConfig.SUMUP_AFF_KEY)
                    .accessToken(pref.getString(PREF_TOKEN, "null"))
                    .build();
            SumUpAPI.openLoginActivity(SumupActivity.this, sumupLogin, REQUEST_CODE_LOGIN);
        }
    }

    private void initAuth() {
        Uri authUri = NetworkUtils.buildAuthorizationUrl(SumupActivity.this);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                authUri);
        startActivity(browserIntent);
        Log.d("AuthURI", authUri.toString());
    }

    private void handleAuthIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (data != null) {
            Log.d("Intentt", data.toString());
            Log.d("Intentt", action != null ? action : "null");
            String authCode = Uri.parse(data.toString()).getQueryParameter("code");

            Log.d("Intentt", authCode);

            pref.edit().putString(PREF_AUTH_CODE, authCode).apply();

            new TokenQueryTask().execute(authCode);
        }
    }

    private void initReceipt(String txCode, String merchantCode) {
        if (pref.getLong(PREF_TOKEN_EXPIRY_EPOCH, 0) <= System.currentTimeMillis()) {
            updateOutput("Access token expired. Please login again and then press receipt button.");
        } else {
            new ReceiptQueryTask().execute(NetworkUtils.buildReceiptRequestUrl(txCode, merchantCode));
        }
    }

    /**
     * Append to output TextView console-like.
     *
     * @param line String to append.
     */
    private void updateOutput(String line) {
        if (outputTextView.getText().equals(getString(R.string.transaction_output))) {
            outputTextView.setText("");
        } else {
            outputTextView.append("\n");
        }

        outputTextView.append(line);
        outputTextView.post(new Runnable() {
            @Override
            public void run() {
                outputScrollView.pageScroll(View.FOCUS_DOWN);
            }
        });

    }
}
