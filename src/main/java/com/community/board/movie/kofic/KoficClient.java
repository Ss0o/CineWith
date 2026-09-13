package com.community.board.movie.kofic;

import com.community.board.movie.client.model.MovieDetail;

public interface KoficClient {
    KoreanTheatricalInfo getKoreanTheatricalInfo(MovieDetail movie);
}
