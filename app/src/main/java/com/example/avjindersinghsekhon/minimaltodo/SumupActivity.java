package com.example.avjindersinghsekhon.minimaltodo;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.avjindersinghsekhon.minimaltodo.utils.NetworkUtils;
import com.sumup.merchant.api.SumUpAPI;
import com.sumup.merchant.api.SumUpLogin;
import com.sumup.merchant.api.SumUpPayment;
import com.sumup.merchant.api.SumUpState;
import com.sumup.merchant.Models.TransactionInfo;


import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.UUID;

public class SumupActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOGIN = 1;
    private static final int REQUEST_CODE_PAYMENT = 2;

    private TextView outputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SumUpState.init(this);
        setContentView(R.layout.activity_sumup);

        //Enable login...
        Button login = (Button) findViewById(R.id.button_login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SumUpLogin sumupLogin = SumUpLogin.builder("7ca84f17-84a5-4140-8df6-6ebeed8540fc").build();
                SumUpAPI.openLoginActivity(SumupActivity.this, sumupLogin, REQUEST_CODE_LOGIN);
            }
        });

        //Enable transaction...
        Button btnCharge = (Button) findViewById(R.id.button_charge);
        btnCharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SumUpPayment payment = SumUpPayment.builder()
                        // mandatory parameters
                        // Please go to https://me.sumup.com/developers to get your Affiliate Key by entering the application ID of your app. (e.g. com.sumup.sdksampleapp)
                        .affiliateKey("7ca84f17-84a5-4140-8df6-6ebeed8540fc")
                        .total(new BigDecimal("1.12")) // minimum 1.00
                        .currency(SumUpPayment.Currency.EUR)
                        // optional: add details
                        .title("Taxi Ride")
                        .receiptEmail("customer@mail.com")
                        .receiptSMS("+3531234567890")
                        // optional: Add metadata
                        .addAdditionalInfo("AccountId", "taxi0334")
                        .addAdditionalInfo("From", "Paris")
                        .addAdditionalInfo("To", "Berlin")
                        // optional: foreign transaction ID, must be unique!
                        .foreignTransactionId(UUID.randomUUID().toString()) // can not exceed 128 chars
                        .build();

                SumUpAPI.checkout(SumupActivity.this, payment, REQUEST_CODE_PAYMENT);
            }
        });

        outputView = (TextView) findViewById(R.id.outputView);

        URL receiptRequestUrl = NetworkUtils.buildUrl("123", "batyanko");
        new ReceiptQueryTask().execute(receiptRequestUrl);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE_PAYMENT && data != null) {

            Bundle extra = data.getExtras();

            int apiResultCode = extra.getInt(SumUpAPI.Response.RESULT_CODE);

            //Handle successful transaction
            if (apiResultCode == SumUpAPI.Response.ResultCode.SUCCESSFUL) {
                String txCode = extra.getString(SumUpAPI.Response.TX_CODE);
                TransactionInfo txInfo = extra.getParcelable(SumUpAPI.Response.TX_INFO);
                String merchantCode = txInfo.getMerchantCode();

                //https://receipts-ng.sumup.com/v0.1/receipts/TRANSACTION_CODE?mid=YOUR_MERCHANT_CODE

                URL receiptRequestUrl = NetworkUtils.buildUrl(txCode, merchantCode);
                new ReceiptQueryTask().execute(receiptRequestUrl);


            }
            String apiResponseMessage = extra.getString(SumUpAPI.Response.MESSAGE);
        }
    }

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
            return receiptResponceFromSumup;
        }

        @Override
        protected void onPostExecute(String receipt) {
            if (receipt != null && !receipt.equals("")) {
                //Populate output
                outputView.setText(receipt);
            } else {
                Log.d("nullResult", "check");
            }
        }
    }
}
