package com.community.board.movie.discovery;
import com.community.board.movie.client.model.DiscoveryMovie;
import org.junit.jupiter.api.Test;
import java.time.LocalDate; import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
class MovieRankingPolicyTests {
 @Test void excludesTinyVoteCountDespiteHighRatingAndCombinesAllThreeSignals(){
  MovieRankingPolicy policy=new MovieRankingPolicy();
  DiscoveryMovie tiny=new DiscoveryMovie(1L,"tiny",null,LocalDate.now(),9.9,10,1);
  DiscoveryMovie trusted=new DiscoveryMovie(2L,"trusted",null,LocalDate.now(),8.8,30000,100);
  assertThat(policy.rank(List.of(tiny,trusted))).containsExactly(trusted);
 }
}
