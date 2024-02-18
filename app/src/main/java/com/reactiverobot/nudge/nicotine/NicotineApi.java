package com.reactiverobot.nudge.nicotine;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NicotineApi {
    String COMPARE_SHORTS_URL = "https://compareyoutubeshortdescriptions-hxzikqxoqa-uc.a.run.app";

    class CompareShortsResponse {
        public boolean similar;
        public String reasoning;

        @Override
        public String toString() {
            return "CompareShortsResponse{" +
                    "similar=" + similar +
                    ", reasoning='" + reasoning + '\'' +
                    '}';
        }
    }
    @GET("/")
    Call<CompareShortsResponse> compareYoutubeShortDescriptions(@Query("description_1") String description1,
                                                                @Query("description_2") String description2);
}
