package community.ddv.domain.movie.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Genre {

  @Id
  private Long id;
  private String name;

  @OneToMany(mappedBy = "genre", cascade = CascadeType.ALL)
  private List<MovieGenre> movieGenres = new ArrayList<>();

  public Genre(Long id, String name) {
    this.id = id;
    this.name = name;
  }
}
