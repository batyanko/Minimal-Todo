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
import android.widget.TextView;

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
    private static final String PREF_TOKEN_EXPIRY_EPOCH = "token-expiry-epoch";

    private SharedPreferences pref;

    private TextView outputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SumUpState.init(this);
        setContentView(R.layout.activity_sumup);

        pref = pref = PreferenceManager.getDefaultSharedPreferences(this);

        handleAuthIntent();

        //TODO enable Themes?

        //Enable login...
        Button login = (Button) findViewById(R.id.button_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SumUpLogin sumupLogin = SumUpLogin.builder("f9070809-39a3-4adb-92f5-588c4002c755").build();
                SumUpAPI.openLoginActivity(SumupActivity.this, sumupLogin, REQUEST_CODE_LOGIN);
            }
        });

        //Enable transaction...
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

        outputView = (TextView) findViewById(R.id.outputView);

//        URL authUrl = NetworkUtils.buildAuthorizationUrl();

//        Intent browserIntent = new Intent(Intent.ACTION_VIEW, NetworkUtils.buildAuthorizationUrl(this));
//        startActivity(browserIntent);
//        Log.d("AuthURI", authUrl.toString());
//        new AuthQueryTask().execute(authUrl);

//        new TokenQueryTask().execute();

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE_PAYMENT && data != null) {

            Bundle extra = data.getExtras();

            int apiResultCode = extra.getInt(SumUpAPI.Response.RESULT_CODE);

            if (outputView.getText().equals(getString(R.string.transaction_output))) {
                outputView.setText("");
            } else {
                outputView.append("\n");
            }
            ;
            outputView.append("Result Code: " + apiResultCode);

            //Handle successful transaction
            if (apiResultCode == SumUpAPI.Response.ResultCode.SUCCESSFUL) {
                String txCode = extra.getString(SumUpAPI.Response.TX_CODE);
                TransactionInfo txInfo = extra.getParcelable(SumUpAPI.Response.TX_INFO);
                String merchantCode = txInfo.getMerchantCode();

                URL receiptRequestUrl = NetworkUtils.buildReceiptRequestUrl(txCode, merchantCode);
                new ReceiptQueryTask().execute(receiptRequestUrl);

                outputView.append("\nTransaction Code: " + txCode);
                outputView.append("\nTransaction Info: " + txInfo);

            }
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
            URL searchUrl = params[0];
            String receiptResponceFromSumup = null;
            Log.d("doInBcg", params[0].toString());

            try {
                receiptResponceFromSumup = NetworkUtils.getResponseFromHttpUrl(searchUrl);
                Log.d("TehReceiptResponse", receiptResponceFromSumup + "");
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
                outputView.append(receipt);
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
            String tokenResponceFromSumup = null;
            String token = null;
            int ttl = 0;

            try {
                tokenResponceFromSumup = NetworkUtils.getPostResponseFromHttpUrl(params[0]);
                Log.d("TehTokenResponse", tokenResponceFromSumup + "");
            } catch (IOException e) {
                e.printStackTrace();
            }

            JSONObject object = null;

            try {
                object = new JSONObject(tokenResponceFromSumup);
                token = object.getString("access_token");
                ttl = object.getInt("expires_in");
                pref.edit().putString(PREF_TOKEN, token).apply();
                pref.edit().putLong(PREF_TOKEN_EXPIRY_EPOCH,
                        System.currentTimeMillis() + ttl*1000).apply();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return new String[]{token};
        }

        @Override
        protected void onPostExecute(String[] tokenData) {

            if ((tokenData.length >= 1) && tokenData[0] != null) {

                outputView.append("Token: " + tokenData[0] + "Token Expiry: ");

            } else {
                Log.d("nullResult", "check");
            }
        }
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
}
