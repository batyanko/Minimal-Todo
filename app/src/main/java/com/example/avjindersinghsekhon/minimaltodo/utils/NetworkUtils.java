/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.avjindersinghsekhon.minimaltodo.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.avjindersinghsekhon.minimaltodo.BuildConfig;
import com.example.avjindersinghsekhon.minimaltodo.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * These utilities will be used to communicate with the network.
 */
public class NetworkUtils {

    /**
     * Builds the URL used to query for a Sumup receipt.
     *
     * @param txCode       Transaction code
     * @param merchantCode Merchant code
     * @return The URL to use to query for receipt
     */
    public static URL buildReceiptRequestUrl(String txCode, String merchantCode) {

        Uri builtUri = Uri.parse("https://receipts-ng.sumup.com/v0.1/receipts").buildUpon()
                .appendPath(txCode)
                .appendQueryParameter("mid", merchantCode)
                .build();

        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public static Uri buildAuthorizationUrl(Context context) {

        String urlString = "https://api.sumup.com/authorize?"
                + "response_type=code&"
                + "client_id=" + BuildConfig.MINIMAL_CLIENT_ID + "&"
                + "redirect_uri=" + context.getString(R.string.MINIMAL_REDIRECT_URL);

        Log.d("authUrl", urlString);
        Uri url = null;
        url = Uri.parse(urlString);
        /*
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }*/
        return url;
    }

    /**
     * Method to return the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();


            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuffer buffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
                Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

            }

            return buffer.toString();


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String requestReceiptFromUrl(URL url, String token) throws IOException {
        String body = null;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .header("access_token", token)
                .build();


        Response response = client.newCall(request).execute();
        try {
            body = response.body().string();
            Log.d("LeBody", body);
            Log.d("thatMessage", response.message());
            Log.d("header", response.header("Access-Control-Allow-Origin"));
            Log.d("header", response.header("Access-Control-Request-Method"));
            Log.d("header", response.header("Connection"));
            Log.d("header", response.header("Content-Type"));
            Log.d("header", response.header("Date"));
            Log.d("header", response.header("Server"));
            Log.d("header", response.header("Status"));
            Log.d("header", response.header("Strict-Transport-Security"));
            Log.d("header", response.header("Vary"));
            Log.d("header", response.header("X-Content-Type-Options"));
            Log.d("header", response.header("X-Frame-Options"));
            Log.d("header", response.message());
            Log.d("thatCode", response.code() + "");
            Log.d("thoseHeaders", response.headers().names().toString());
            response.close();
        } catch (NullPointerException e) {
            //yippppee
            e.printStackTrace();
        }
        return body;
    }

    public static String requestTokenFromUrl(String authCode) throws IOException {
        String body = null;
        OkHttpClient client = new OkHttpClient();

        FormBody.Builder formBuilder = new FormBody.Builder();
        formBuilder.add("grant_type", "authorization_code");
        formBuilder.add("client_id", BuildConfig.MINIMAL_CLIENT_ID);
        formBuilder.add("client_secret", BuildConfig.MINIMAL_CLIENT_SECRET);
        formBuilder.add("redirect_uri", "https://sites.google.com/view/strokeratecoach/home");
        formBuilder.add("code", authCode);

        RequestBody formBody = formBuilder.build();

        Request request = new Request.Builder()
                .url("https://api.sumup.com/token")
                .post(formBody)
                .build();

        Response response = client.newCall(request).execute();
        try {
            body = response.body().string();
            Log.d("LeBody", body);
            Log.d("thatMessage", response.message());
            Log.d("header", response.header("Access-Control-Allow-Origin"));
            Log.d("header", response.header("Access-Control-Request-Method"));
            Log.d("header", response.header("Connection"));
            Log.d("header", response.header("Content-Type"));
            Log.d("header", response.header("Date"));
            Log.d("header", response.header("Server"));
            Log.d("header", response.header("Status"));
            Log.d("header", response.header("Strict-Transport-Security"));
            Log.d("header", response.header("Vary"));
            Log.d("header", response.header("X-Content-Type-Options"));
            Log.d("header", response.header("X-Frame-Options"));
            Log.d("header", response.message());
            Log.d("thatCode", response.code() + "");
            Log.d("thoseHeaders", response.headers().names().toString());
            response.close();
        } catch (NullPointerException e) {
            //yippppee
            e.printStackTrace();
        }
        return body;
    }

    /**
     * Method to return Sumup token data from a HTTP POST request.
     *
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static String getPostResponseFromHttpUrl(String authCode) throws IOException {
        Log.d("onPostResponse", "check");
        URL url = new URL("https://api.sumup.com/token");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setInstanceFollowRedirects(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("grant_type", "authorization_code");
        connection.setRequestProperty("client_id", BuildConfig.MINIMAL_CLIENT_ID);
        connection.setRequestProperty("client_secret", BuildConfig.MINIMAL_CLIENT_SECRET);
        connection.setRequestProperty("redirect_uri", "https://sites.google.com/view/strokeratecoach/home");
        connection.setRequestProperty("code", authCode);

        Log.d("status", " " + connection.getRequestProperties().toString());


        int responseCode = connection.getResponseCode();

        InputStream errorStream = connection.getErrorStream();
//        InputStream stream = connection.getInputStream();
        Log.d("status", responseCode + " ");

        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(errorStream));
        StringBuffer buffer = new StringBuffer();
        String line = "";
        while ((line = reader.readLine()) != null) {
            buffer.append(line + "\n");
        }

        Log.d("status", responseCode + " / " + buffer.toString());

        return buffer.toString();
    }
}