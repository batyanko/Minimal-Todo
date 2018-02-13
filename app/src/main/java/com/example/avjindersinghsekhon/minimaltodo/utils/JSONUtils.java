package com.example.avjindersinghsekhon.minimaltodo.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yankog on 13.02.18.
 */

public class JSONUtils {
    /**
     * Parse receipt JSON to a concise receipt
     *
     * @param receiptResponceFromSumup JSON receipt response from Sumup.
     * @return concise receipt
     */
    public static String getReceiptFromJson(String receiptResponceFromSumup) {
        String niceReceipt = "";

        JSONObject mainObject = null;

        try {
            mainObject = new JSONObject(receiptResponceFromSumup);
            Log.d("niceJSON", mainObject.toString(2));
            JSONObject txObject = mainObject.getJSONObject("transaction_data");
            String txCode = txObject.getString("transaction_code");
            String txAmount = txObject.getString("amount");
            String txCurrency = txObject.getString("currency");

            JSONObject merchantProfile = mainObject.getJSONObject("merchant_data")
                    .getJSONObject("merchant_profile");
            JSONObject merchantAddress = merchantProfile.getJSONObject("address");
            String merchantName = merchantProfile.getString("business_name");
            String merchantCode = merchantProfile.getString("merchant_code");
            String address = merchantAddress.getString("address_line1")
                    + "\n" + merchantAddress.getString("city")
                    + "\n" + merchantAddress.getString("country");

            JSONObject cardObject = txObject.getJSONObject("card");
            String customerName = cardObject.getString("cardholder_name");
            String cardDigits = cardObject.getString("last_4_digits");
            String cardType = cardObject.getString("type");

            JSONArray productArray = txObject.getJSONArray("products");
            StringBuilder productsBuilder = new StringBuilder();

            for (int i = 0; i < productArray.length(); i++) {
                if (i != 1) productsBuilder.append("\n");
                JSONObject product = productArray.getJSONObject(i);
                productsBuilder.append(product.getString("name"));
                productsBuilder.append("  ");
                productsBuilder.append(product.getString("price"));
                productsBuilder.append("   Qty.: ");
                productsBuilder.append(product.getString("quantity"));
                productsBuilder.append("   Total: ");
                productsBuilder.append(product.getString("total_price"));
            }
            String products = productsBuilder.toString();

            niceReceipt = "\n"
                    + "\n"
                    + "----------------"
                    + "\n" + merchantName
                    + "\n"
                    + "\n" + address
                    + "\n"
                    + "\n" + "Transaction Receipt No. " + txCode
                    + "\n"
                    + "\n" + "Payment Details: "
                    + "\n" + txCurrency + " " + txAmount
                    + "\n" + "Card Holder: " + customerName
                    + "\n" + "Card: **** **** **** " + cardDigits
                    + "\n"
                    + "\n" + products
                    + "\n"
                    + "\n" + "--"
                    + "\n" + "Total Amount Paid:   " + txAmount
                    + "\n"
                    + "\n";

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return niceReceipt;
    }
}
