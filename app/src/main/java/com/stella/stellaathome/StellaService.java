package com.stella.stellaathome;

import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public class StellaService {
    private static final String TAG = "StellaService";
    StellaAPI api;
    private String token;
    public StellaService(){
        Retrofit retro = new Retrofit.Builder()
                .baseUrl("https://api.interstella.online/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retro.create(StellaAPI.class);
        token = "";
    }
    private void login(Runnable repeat){
        api.login(Credential.username, Credential.password).enqueue(new Callback<TokenInfo>() {
            @Override
            public void onResponse(Call<TokenInfo> call, Response<TokenInfo> response) {
                if (response.isSuccessful()){
                    TokenInfo info = response.body();
                    assert info != null;
                    token = "Bearer " + info.AccessToken;
                    repeat.run();
                }else{
                    Log.d(TAG, "onResponse: Failed to login");
                }
            }

            @Override
            public void onFailure(Call<TokenInfo> call, Throwable t) {
                Log.e(TAG, "onFailure: Error login", t);
            }
        });
    }
    public void connected(){
        Log.d(TAG, "connected: Sending Request");
        StellaService obj = this;
        api.connect(token).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (!response.isSuccessful()){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                    login(obj::connected);
                }else{
                    Log.d(TAG, "Connect onResponse: sent");
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                Log.d(TAG, "Connect onFailure: " + t.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
                login(obj::connected);
            }
        });
    }
    public void disconnected(){
        Log.d(TAG, "connected: Sending Request");
        StellaService obj = this;
        api.disconnect(token).enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {
                if (!response.isSuccessful()){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {}
                    login(obj::disconnected);
                }else{
                    Log.d(TAG, "Disconnect onResponse: sent");
                }
            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {
                Log.d(TAG, "Disconnect onFailure: " + t.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
                login(obj::disconnected);
            }
        });
    }
}
interface StellaAPI{
    @FormUrlEncoded
    @POST("/auth/account/token")
    Call<TokenInfo> login(@Field("username") String username, @Field("password") String password);

    @GET("/nearby/connected")
    Call<Message> connect(@Header("Authorization") String accessToken);

    @GET("/nearby/disconnected")
    Call<Message> disconnect(@Header("Authorization") String accessToken);
}
