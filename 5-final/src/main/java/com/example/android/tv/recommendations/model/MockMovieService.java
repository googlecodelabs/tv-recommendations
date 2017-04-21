/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.tv.recommendations.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Mocks gathering movies from an external source. */
public final class MockMovieService {

    private static List<Movie> list;
    private static long count = 0;

    /**
     * Creates and caches a list of movies.
     *
     * @return a list of movies.
     */
    public static List<Movie> getList() {
        if (list == null || list.isEmpty()) {
            list = createMovieList();
        }
        return list;
    }

    /**
     * Shuffles the list of movies to make the returned list appear to be a different list from
     * {@link #getList()}.
     *
     * @return a list of movies in random order.
     */
    public static List<Movie> getFreshList() {
        List<Movie> shuffledMovies = new ArrayList<>(getList());
        Collections.shuffle(shuffledMovies);
        return shuffledMovies;
    }

    private static List<Movie> createMovieList() {
        List<Movie> list = new ArrayList<>();
        String title[] = {
            "Zeitgeist 2010_ Year in Review",
            "Google Demo Slam_ 20ft Search",
            "Introducing Gmail Blue",
            "Introducing Google Fiber to the Pole",
            "Introducing Google Nose"
        };

        String description =
                "Fusce id nisi turpis. Praesent viverra bibendum semper. "
                        + "Donec tristique, orci sed semper lacinia, quam erat rhoncus massa, non congue tellus est "
                        + "quis tellus. Sed mollis orci venenatis quam scelerisque accumsan. Curabitur a massa sit "
                        + "amet mi accumsan mollis sed et magna. Vivamus sed aliquam risus. Nulla eget dolor in elit "
                        + "facilisis mattis. Ut aliquet luctus lacus. Phasellus nec commodo erat. Praesent tempus id "
                        + "lectus ac scelerisque. Maecenas pretium cursus lectus id volutpat.";

        String videoUrl[] = {
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review.mp4",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft%20Search.mp4",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail%20Blue.mp4",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Fiber%20to%20the%20Pole.mp4",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose.mp4"
        };
        String bgImageUrl[] = {
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review/bg.jpg",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft%20Search/bg.jpg",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail%20Blue/bg.jpg",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Fiber%20to%20the%20Pole/bg.jpg",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/bg.jpg",
        };
        String cardImageUrl[] = {
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Zeitgeist/Zeitgeist%202010_%20Year%20in%20Review/card.jpg",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%2020ft%20Search/card.jpg",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Gmail%20Blue/card.jpg",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Fiber%20to%20the%20Pole/card.jpg",
            "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Nose/card.jpg"
        };

        list.add(
                buildMovieInfo(
                        "category",
                        title[0],
                        description,
                        "Studio Zero",
                        videoUrl[0],
                        cardImageUrl[0],
                        bgImageUrl[0]));
        list.add(
                buildMovieInfo(
                        "category",
                        title[1],
                        description,
                        "Studio One",
                        videoUrl[1],
                        cardImageUrl[1],
                        bgImageUrl[1]));
        list.add(
                buildMovieInfo(
                        "category",
                        title[2],
                        description,
                        "Studio Two",
                        videoUrl[2],
                        cardImageUrl[2],
                        bgImageUrl[2]));
        list.add(
                buildMovieInfo(
                        "category",
                        title[3],
                        description,
                        "Studio Three",
                        videoUrl[3],
                        cardImageUrl[3],
                        bgImageUrl[3]));
        list.add(
                buildMovieInfo(
                        "category",
                        title[4],
                        description,
                        "Studio Four",
                        videoUrl[4],
                        cardImageUrl[4],
                        bgImageUrl[4]));

        return list;
    }

    private static Movie buildMovieInfo(
            String category,
            String title,
            String description,
            String studio,
            String videoUrl,
            String cardImageUrl,
            String bgImageUrl) {
        Movie movie = new Movie();
        movie.setId(count);
        incCount();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setStudio(studio);
        movie.setCategory(category);
        movie.setCardImageUrl(cardImageUrl);
        movie.setBackgroundImageUrl(bgImageUrl);
        movie.setVideoUrl(videoUrl);
        return movie;
    }

    private static void incCount() {
        count++;
    }
}
