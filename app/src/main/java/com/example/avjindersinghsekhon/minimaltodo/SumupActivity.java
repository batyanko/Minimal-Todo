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

import com.example.avjindersinghsekhon.minimaltodo.utils.JSONUtils;
import com.example.avjindersinghsekhon.minimaltodo.utils.NetworkUtils;
import com.sumup.merchant.api.SumUpAPI;
import com.sumup.merchant.api.SumUpLogin;
import com.sumup.merchant.api.SumUpPayment;
import com.sumup.merchant.api.SumUpState;


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
    private static final String PREF_LAST_TRANSACTION = "last-tx-code";

    private SharedPreferences pref;

    private ScrollView outputScrollView;
    private TextView outputTextView;

    String theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        theme = getSharedPreferences(MainActivity.THEME_PREFERENCES, MODE_PRIVATE).getString(MainActivity.THEME_SAVED, MainActivity.LIGHTTHEME);
        if(theme.equals(MainActivity.DARKTHEME)){
            Log.d("OskarSchindler", "whoa");
            setTheme(R.style.CustomStyle_DarkTheme);
        }
        else{
            Log.d("OskarSchindler", "eh?");
            setTheme(R.style.CustomStyle_LightTheme);
        }

        super.onCreate(savedInstanceState);
        SumUpState.init(this);
        setContentView(R.layout.activity_sumup);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        handleAuthIntent();

        //TODO enable Themes?

        final Button login = (Button) findViewById(R.id.button_login);
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
                initCharge();
            }
        });

        final Button receiptRequestButton = (Button) findViewById(R.id.button_request_receipt);
        receiptRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initReceipt(
                        pref.getString(PREF_LAST_TRANSACTION, "TDTPUNSPC3"),
                        BuildConfig.SUMUP_MERCHANT);
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

        //Common error messages?
        switch (apiResultCode) {
            case SumUpAPI.Response.ResultCode.ERROR_INVALID_PARAM: {
                updateOutput(getString(R.string.login_message_fail_params));
                break;
            }
            case SumUpAPI.Response.ResultCode.ERROR_GEOLOCATION_REQUIRED: {
                updateOutput(getString(R.string.login_message_fail_location));
                break;
            }
            case SumUpAPI.Response.ResultCode.ERROR_NO_CONNECTIVITY: {
                updateOutput(getString(R.string.login_message_fail_internet));
                break;
            }
        }

        if (requestCode == REQUEST_CODE_LOGIN) {
            switch (apiResultCode) {

                case SumUpAPI.Response.ResultCode.SUCCESSFUL: {
                    updateOutput(getString(R.string.login_message_success));
                    break;
                }

                case SumUpAPI.Response.ResultCode.ERROR_INVALID_TOKEN: {
                    pref.edit().putLong(PREF_TOKEN_EXPIRY_EPOCH, 0).apply();
                    updateOutput(getString(R.string.login_message_fail_token));
                    break;
                }

                case SumUpAPI.Response.ResultCode.ERROR_ALREADY_LOGGED_IN: {
                    updateOutput(getString(R.string.login_message_fail_logged_in));
                    break;
                }
            }
        }

        if (requestCode == REQUEST_CODE_PAYMENT) {

            updateOutput(extra.getString(SumUpAPI.Response.MESSAGE));

            switch (resultCode) {
                case SumUpAPI.Response.ResultCode.SUCCESSFUL : {
                    String txCode = extra.getString(SumUpAPI.Response.TX_CODE);
                    pref.edit().putString(PREF_LAST_TRANSACTION, txCode).apply();

                    initReceipt(txCode, BuildConfig.SUMUP_MERCHANT);

                    updateOutput("\n" + getString(R.string.transaction_code_label) + txCode);
                    break;
                }
                case SumUpAPI.Response.ResultCode.ERROR_NOT_LOGGED_IN : {
                    updateOutput(getString(R.string.tx_message_login_first));
                }
            }
            //TODO handle error cases?
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
            URL searchUrl = params[0];
            String receiptResponceFromSumup = null;

            try {
                receiptResponceFromSumup = NetworkUtils.requestReceiptFromUrl(
                        searchUrl,
                        pref.getString(PREF_TOKEN, "null")
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

            return JSONUtils.getReceiptFromJson(receiptResponceFromSumup);
        }

        @Override
        protected void onPostExecute(String receipt) {
            if (receipt != null && !receipt.equals("")) {
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

    /**
     * Display dialog to enter customer contact, then start a Sumup checkout
     */
    private void initCharge() {
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
                new ColorDrawable(getResources().getColor(R.color.primary_lightest)));

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
                        .title("Using *the* Minimal App")
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
        contactsPopupWindow.showAtLocation(findViewById(R.id.button_charge), Gravity.CENTER, 0, 0);
    }

    /**
     * Invoke Sumup login website to get auth code. Part of login process.
     */
    private void initAuth() {
        Uri authUri = NetworkUtils.buildAuthorizationUrl(SumupActivity.this);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                authUri);
        startActivity(browserIntent);
        Log.d("AuthURI", authUri.toString());
    }

    /**
     * Handle incoming intent with auth code. Part of login process.
     */
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

    /**
     * Initialize AsyncTask to query and show receipt, in case a valid token is available.
     * @param txCode transaction code
     * @param merchantCode merchant code
     */
    private void initReceipt(String txCode, String merchantCode) {
        if (pref.getLong(PREF_TOKEN_EXPIRY_EPOCH, 0) <= System.currentTimeMillis()) {
            updateOutput(getString(R.string.receipt_invalid_token));
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
                outputScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}
