package com.example.qrcodepayment;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;



public class MainActivity extends AppCompatActivity {

    Button price_button;
    TextView price_view;
    Button pay_button;
    HashMap<String, String> hashMap = new HashMap<String, String>();
    String vatRate;
    String qr_code = "";
    String dataqr ="";
    String Vat;
    int SDK_INT = android.os.Build.VERSION.SDK_INT;

    //SSL Cert expired
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                        }
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            OkHttpClient okHttpClient = builder.build();
            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        price_button = findViewById(R.id.price_button);
        price_view = findViewById(R.id.price_view);
        pay_button = findViewById(R.id.pay_button);

        price_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Get QR Request
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                        .permitAll().build();
                StrictMode.setThreadPolicy(policy);

                OkHttpClient client;
                client = getUnsafeOkHttpClient();
                MediaType mediaType = MediaType.parse("application/json");
                RequestBody getbodyqr = RequestBody.create(mediaType, "{\"totalReceiptAmount\":1000}");
                Request request_get_qr = new Request.Builder()
                        .url("https://sandbox-api.payosy.com/api/get_qr_sale")
                        .post(getbodyqr)
                        .addHeader("x-ibm-client-id", "6a343f05-a696-42f5-bf52-feb067664edd")
                        .addHeader("x-ibm-client-secret", "V0pE7gC5gW0cH4bA6kX3mN6jA3xK0tD1qQ5tH4qI6wY0vT8jL2")
                        .addHeader("content-type", "application/json")
                        .addHeader("accept", "application/json")
                        .build();

                try {
                    Response responseGetQR = client.newCall(request_get_qr).execute();
                    dataqr = responseGetQR.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String[] array = dataqr.split(",");
                //QR Code Decoder
                String qr_code1 = array[2].substring(10);
                qr_code = qr_code1.substring(0,qr_code1.length()-2);
                for (int i=0; i<qr_code.length();) {
                    try {
                        String tag = qr_code.substring(i, i=i+2);
                        String len = qr_code.substring(i, i=i+2);
                        int length = Integer.parseInt(len);
                        String values = qr_code.substring(i, i=i+length);
                        hashMap.put(tag, values);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Error parsing number",e);
                    } catch (IndexOutOfBoundsException e) {
                        throw new RuntimeException("Error processing field",e);
                    }
                }
                //Values of QR Code
                    if(Integer.parseInt(hashMap.get("53")) ==949)
                        price_view.setText(hashMap.get("54") + "TL");
                    else
                        price_view.setText(hashMap.get("54"));
                    Vat = hashMap.get("86").substring(0,hashMap.get("54").length()-1);
            }
        });

        pay_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SDK_INT > 8)
                {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                            .permitAll().build();
                    StrictMode.setThreadPolicy(policy);

                    OkHttpClient client;
                    client = getUnsafeOkHttpClient();

                    MediaType mediaType_set = MediaType.parse("application/json");
                    String bodyContent_set = "{\"returnCode\":1000,\"returnDesc\":\"success\",\"receiptMsgCustomer\":\"beko Campaign\",\"receiptMsgMerchant\":\"beko Campaign Merchant\",\"paymentInfoList\":[{\"paymentProcessorID\":67,\"paymentActionList\":[{\"paymentType\":3,\"amount\":"+hashMap.get("54")+",\"currencyID\":"+hashMap.get("53")+",\"vatRate\":"+Vat+"}]}],\"QRdata\":\""+qr_code+"\"}";
                    RequestBody body = RequestBody.create(mediaType_set, bodyContent_set);

                    Request request_set = new Request.Builder()
                        .url("https://sandbox-api.payosy.com/api/payment")
                        .post(body)
                        .addHeader("x-ibm-client-id", "6a343f05-a696-42f5-bf52-feb067664edd")
                        .addHeader("x-ibm-client-secret", "V0pE7gC5gW0cH4bA6kX3mN6jA3xK0tD1qQ5tH4qI6wY0vT8jL2")
                        .addHeader("content-type", "application/json")
                        .addHeader("accept", "application/json")
                        .build();
                    Response response;
                        try {
                        response = client.newCall(request_set).execute();
                        Log.i("RESPONSE_LOG","response message=" + response.message() + " isSuccessful=" + response.isSuccessful() + " response string = " + response.body().string());
                        if(response.isSuccessful() == true){
                            Toast.makeText(MainActivity.this,
                                    "Payment Completed", Toast.LENGTH_LONG).show();
                        }
                        else {
                            Toast.makeText(MainActivity.this,
                                    "Error", Toast.LENGTH_LONG).show();
                        }
                        }
                        catch (IOException ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
    }
}

