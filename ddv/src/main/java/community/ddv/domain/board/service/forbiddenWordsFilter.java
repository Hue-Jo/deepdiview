package community.ddv.domain.board.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class forbiddenWordsFilter {

  private final RedisTemplate<String, String> redisStringTemplate;

  public Set<String> getForbiddenWords() {
    return redisStringTemplate.opsForSet().members("forbidden-words");
  }

  public String filterForbiddenWords(String input) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    Set<String> forbiddenWords = getForbiddenWords();
    for (String word : forbiddenWords) {
      if (input.contains(word)) {
        String masked = "*".repeat(word.length());
        input = input.replace(word, masked);
      }
    }
    return input;
  }
}

