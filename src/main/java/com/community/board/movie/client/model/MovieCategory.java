package com.community.board.movie.client.model;

import java.util.Arrays;

public enum MovieCategory {
    ACTION("action", "액션", 28),
    ADVENTURE("adventure", "모험", 12),
    ANIMATION("animation", "애니메이션", 16),
    COMEDY("comedy", "코미디", 35),
    DRAMA("drama", "드라마", 18),
    FANTASY("fantasy", "판타지", 14),
    HORROR("horror", "공포", 27),
    ROMANCE("romance", "로맨스", 10749),
    SCIENCE_FICTION("sf", "SF", 878),
    THRILLER("thriller", "스릴러", 53);

    private final String key;
    private final String label;
    private final int tmdbGenreId;
    MovieCategory(String key, String label, int tmdbGenreId) { this.key = key; this.label = label; this.tmdbGenreId = tmdbGenreId; }
    public String key() { return key; }
    public String label() { return label; }
    public int tmdbGenreId() { return tmdbGenreId; }

    public static MovieCategory fromKey(String key) {
        return Arrays.stream(values())
                .filter(category -> category.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 영화 장르입니다."));
    }
}
