package com.community.board.movie.client.model;

public enum MovieCategory {
    ACTION("action", "액션", 28), COMEDY("comedy", "코미디", 35), ROMANCE("romance", "로맨스", 10749),
    HORROR("horror", "공포", 27), SCIENCE_FICTION("sf", "SF", 878);

    private final String key;
    private final String label;
    private final int tmdbGenreId;
    MovieCategory(String key, String label, int tmdbGenreId) { this.key = key; this.label = label; this.tmdbGenreId = tmdbGenreId; }
    public String key() { return key; }
    public String label() { return label; }
    public int tmdbGenreId() { return tmdbGenreId; }
}
